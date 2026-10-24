package parfait.persistence.migration

import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Connection
import java.sql.DriverManager

@Testcontainers
class FlywayMigrationTest {
    @Container
    val mysql = MySQLContainer("mysql:8.4")

    @Test
    fun `전체 마이그레이션이 빈 DB에 성공적으로 적용된다`() {
        val result = migrate(mysql.databaseName)

        result shouldBeGreaterThan 0
    }

    /**
     * flyway가 꺼져 있던 기간 동안 `ddl-auto: update`가 남긴 드리프트(#110)를 재현하고,
     * V16이 그것을 빈 DB에서 출발한 스키마와 같은 상태로 수렴시키는지 검증한다.
     *
     * `ddl-auto: update`는 컬럼·인덱스를 추가만 하고 삭제하지 않는다. 그 결과 DROP을 포함한
     * 마이그레이션의 효과가 누락되어 `parfait_image` INSERT가 실패했던 것이 이 테스트의 배경이다.
     * V16은 운영 DB뿐 아니라 신규 DB에서도 안전해야 하므로, 두 경로의 최종 스키마가
     * 정확히 같아야 한다.
     */
    @Test
    fun `ddl-auto 드리프트가 있는 스키마도 V16 적용 후 빈 DB 결과와 동일해진다`() {
        migrate(mysql.databaseName)
        val fromEmptyDatabase = snapshot(mysql.databaseName)

        val drifted = driftedDatabase("drifted")
        migrate(drifted)

        snapshot(drifted) shouldBe fromEmptyDatabase
    }

    /**
     * V16이 V14의 데이터 이관까지 대신하는지 검증한다.
     *
     * baseline으로 V14를 건너뛰면 컬럼만 있고 값이 비어 있는 멤버가 남는다. 전부 `DEFAULT`로
     * 채우면 NOT NULL은 만족하지만 활성 멤버가 탈퇴 멤버용 칩을 갖게 되므로, 그룹 안에서
     * 겹치지 않는 `TYPEn`이 배정되어야 한다.
     */
    @Test
    fun `V16은 칩이 비어 있는 활성 멤버에게 그룹 내 미사용 TYPE을 배정한다`() {
        val drifted = driftedDatabase("backfill")
        seedMembersWithEmptyChip(drifted)

        migrate(drifted)

        val chips = nametagChips(drifted)
        // 이미 배정된 값은 건드리지 않는다.
        chips["이미배정"] shouldBe "TYPE1"
        // 탈퇴 멤버는 DEFAULT로 수렴한다.
        chips["탈퇴자"] shouldBe "DEFAULT"
        // NULL·빈 문자열 모두 미배정으로 보고, 서로 겹치지 않는 미사용 칩을 받는다.
        val backfilled = listOf(chips.getValue("널"), chips.getValue("빈문자열"))
        backfilled.forEach { it shouldBeIn ASSIGNABLE_CHIPS - "TYPE1" }
        backfilled.toSet().size shouldBe 2
    }

    @Test
    fun `V17은 device_token 테이블을 token 유니크 제약과 인덱스와 함께 생성한다`() {
        migrate(mysql.databaseName)

        val snapshot = snapshot(mysql.databaseName)

        snapshot.any { it.startsWith("COLUMN|device_token|token|varchar(512)|NO|") } shouldBe true
        snapshot.any { it.startsWith("COLUMN|device_token|session_id|varchar(64)|YES|") } shouldBe true
        snapshot.any { it.startsWith("COLUMN|device_token|platform|varchar(20)|NO|") } shouldBe true
        snapshot.any { it == "CONSTRAINT|device_token|uk_device_token_token|UNIQUE" } shouldBe true
        snapshot.any { it == "CONSTRAINT|device_token|fk_device_token_member|FOREIGN KEY" } shouldBe true
        snapshot.any { it.startsWith("INDEX|device_token|idx_device_token_member_session_id|") } shouldBe true
    }

    /** V15까지 적용한 뒤 운영에서 관측된 드리프트를 얹은 DB를 만든다. */
    private fun driftedDatabase(name: String): String {
        connect(mysql.databaseName).use { it.createStatement().execute("CREATE DATABASE $name") }
        migrate(name, target = MigrationVersion.fromVersion("15"))
        applyDdlAutoDrift(name)
        return name
    }

    /** 운영 DB에서 실제로 관측된 드리프트를 그대로 재현한다. */
    private fun applyDdlAutoDrift(database: String) {
        connect(database).use { connection ->
            connection.createStatement().use { statement ->
                listOf(
                    // V5·V10이 지우려던 컬럼이 남아 있다
                    "ALTER TABLE member ADD COLUMN email VARCHAR(255) NULL",
                    "ALTER TABLE member ADD COLUMN apple_refresh_token VARCHAR(1024) NULL",
                    // V6이 지우려던 유니크 제약이 남아 있다
                    "ALTER TABLE parfait_group_member " +
                        "ADD CONSTRAINT uk_parfait_group_member_group_nickname " +
                        "UNIQUE (parfait_group_id, group_nickname)",
                    // V15의 NOT NULL과 V6의 정밀도가 반영되지 않았다
                    "ALTER TABLE parfait_group_member MODIFY COLUMN nametag_chip VARCHAR(10) NULL",
                    "ALTER TABLE parfait_group_member MODIFY COLUMN left_at DATETIME(6) NULL",
                    // ddl-auto는 평범한 Long 컬럼에 FK를 만들지 않는다
                    "ALTER TABLE parfait_image DROP FOREIGN KEY fk_parfait_image_placed_by_group_member",
                    "ALTER TABLE parfait_image DROP INDEX fk_parfait_image_placed_by_group_member",
                    // ddl-auto가 만든 컬럼에는 V7·V11이 지정한 DEFAULT가 없다
                    "ALTER TABLE image_meta ALTER COLUMN image_type DROP DEFAULT",
                    "ALTER TABLE image_meta ALTER COLUMN status DROP DEFAULT",
                    "ALTER TABLE parfait ALTER COLUMN status DROP DEFAULT",
                ).forEach(statement::execute)
            }
        }
    }

    /**
     * 칩이 비어 있는 활성 멤버 두 명(NULL·빈 문자열), 이미 배정된 활성 멤버 한 명,
     * 칩이 비어 있는 탈퇴 멤버 한 명을 같은 그룹에 넣는다.
     */
    private fun seedMembersWithEmptyChip(database: String) {
        connect(database).use { connection ->
            connection.createStatement().use { statement ->
                listOf(
                    """
                    INSERT INTO parfait_group (id, name, invite_code, member_limit, created_at, updated_at)
                    VALUES (1, '백필그룹', 'BACKFL', 12, NOW(), NOW())
                    """.trimIndent(),
                    """
                    INSERT INTO member (id, login_provider, provider_user_id, global_nickname, created_at, updated_at)
                    VALUES (1, 'KAKAO', 'p1', 'n1', NOW(), NOW()), (2, 'KAKAO', 'p2', 'n2', NOW(), NOW()),
                           (3, 'KAKAO', 'p3', 'n3', NOW(), NOW()), (4, 'KAKAO', 'p4', 'n4', NOW(), NOW())
                    """.trimIndent(),
                    """
                    INSERT INTO parfait_group_member
                        (parfait_group_id, member_id, group_nickname, joined_at, left_at, nametag_chip)
                    VALUES (1, 1, '널', NOW(), NULL, NULL),
                           (1, 2, '빈문자열', NOW(), NULL, ''),
                           (1, 3, '이미배정', NOW(), NULL, 'TYPE1'),
                           (1, 4, '탈퇴자', NOW(), NOW(), NULL)
                    """.trimIndent(),
                ).forEach(statement::execute)
            }
        }
    }

    private fun nametagChips(database: String): Map<String, String> =
        connect(database)
            .use { connection ->
                connection.query(
                    "SELECT CONCAT_WS('|', group_nickname, nametag_chip) FROM parfait_group_member",
                )
            }.associate { it.substringBefore('|') to it.substringAfter('|') }

    private fun migrate(
        database: String,
        target: MigrationVersion = MigrationVersion.LATEST,
    ): Int =
        Flyway
            .configure()
            .dataSource(jdbcUrl(database), ROOT_USER, mysql.password)
            .locations("classpath:db/migration")
            .target(target)
            .load()
            .migrate()
            .migrationsExecuted

    /**
     * 컬럼 정의·인덱스·제약을 정렬된 문자열로 뽑는다. 컬럼 물리 순서는 제외한다 —
     * `ddl-auto`가 컬럼을 뒤에 덧붙인 탓에 운영과 신규 DB의 순서가 다르지만,
     * 테이블을 재생성하지 않는 한 맞출 수 없고 동작에도 영향이 없다.
     */
    private fun snapshot(database: String): List<String> =
        connect(database).use { connection ->
            val columns =
                connection.query(
                    """
                    SELECT CONCAT_WS('|', 'COLUMN', TABLE_NAME, COLUMN_NAME, COLUMN_TYPE,
                                     IS_NULLABLE, IFNULL(COLUMN_DEFAULT, '-'), EXTRA)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = ? AND TABLE_NAME <> 'flyway_schema_history'
                    """.trimIndent(),
                    database,
                )
            val indexes =
                connection.query(
                    """
                    SELECT CONCAT_WS('|', 'INDEX', TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX,
                                     COLUMN_NAME, NON_UNIQUE)
                    FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = ? AND TABLE_NAME <> 'flyway_schema_history'
                    """.trimIndent(),
                    database,
                )
            val constraints =
                connection.query(
                    """
                    SELECT CONCAT_WS('|', 'CONSTRAINT', TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE)
                    FROM information_schema.TABLE_CONSTRAINTS
                    WHERE TABLE_SCHEMA = ? AND TABLE_NAME <> 'flyway_schema_history'
                    """.trimIndent(),
                    database,
                )
            (columns + indexes + constraints).sorted()
        }

    private fun Connection.query(
        sql: String,
        vararg parameters: String,
    ): List<String> =
        prepareStatement(sql).use { statement ->
            parameters.forEachIndexed { index, parameter -> statement.setString(index + 1, parameter) }
            statement.executeQuery().use { resultSet ->
                generateSequence { if (resultSet.next()) resultSet.getString(1) else null }.toList()
            }
        }

    private fun connect(database: String): Connection =
        DriverManager.getConnection(jdbcUrl(database), ROOT_USER, mysql.password)

    private fun jdbcUrl(database: String): String = mysql.jdbcUrl.replace("/${mysql.databaseName}", "/$database")

    companion object {
        /** 테스트용 스키마를 하나 더 만들어야 해서 컨테이너 기본 사용자 대신 root로 붙는다. */
        private const val ROOT_USER = "root"

        /** NameTagChipType에서 DEFAULT를 뺀, 실제로 배정 가능한 칩. */
        private val ASSIGNABLE_CHIPS = (1..12).map { "TYPE$it" }
    }
}
