package parfait.persistence.migration

import io.kotest.matchers.ints.shouldBeGreaterThan
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class FlywayMigrationTest {
    @Container
    val mysql = MySQLContainer("mysql:8.4")

    @Test
    fun `전체 마이그레이션이 빈 DB에 성공적으로 적용된다`() {
        val result =
            Flyway
                .configure()
                .dataSource(mysql.jdbcUrl, mysql.username, mysql.password)
                .locations("classpath:db/migration")
                .load()
                .migrate()

        result.migrationsExecuted shouldBeGreaterThan 0
    }
}
