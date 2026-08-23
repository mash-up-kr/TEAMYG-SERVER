package parfait.bootstrap

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.test.JobOperatorTestUtils
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import parfait.core.parfait.domain.Parfait
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfait.port.out.ParfaitSavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupSavePort
import parfait.core.parfaitgroup.domain.InviteCode
import parfait.core.parfaitgroup.domain.ParfaitGroup
import java.time.LocalDate

/**
 * `parfaitCanvasRotationJob`을 Spring Batch의 실제 실행 경로(JobOperator, JobRepository,
 * 배치 메타테이블)까지 통째로 태워보는 종단 테스트.
 *
 * 기존 배치 관련 테스트는 전부 [parfait.core.parfait.port.`in`.RotateParfaitCanvasesUseCase]를
 * 경계에서 mock 처리한다. 그래서 "배치 메타테이블이 실제로 존재하는가"라는 문제를
 * 어떤 테스트도 잡아내지 못했고, 설정 파일을 읽다가 뒤늦게 발견됐다.
 *
 * 이 테스트는 실제 배포 설정을 그대로 둔 채(bootstrap/application.yaml의
 * spring.flyway.enabled=true, spring.batch.jdbc.initialize-schema=never) 배치 메타테이블이
 * 마련되어 Job이 끝까지 도는지를 검증한다. 즉 V12가 메타테이블의 단일 소유자라는 사실을
 * 종단으로 확인하는 자리다. 테스트에서만 설정을 바꿔 우회하면 "무엇이 배치 메타테이블을
 * 만드는가"라는 정작 중요한 질문을 검증하지 못한다.
 */
@Testcontainers
@SpringBatchTest
@SpringBootTest(
    classes = [ParfaitApplication::class],
    properties = [
        "jwt.secret-key=batch-job-test-only-dummy-jwt-secret-key-32-bytes-min",
    ],
)
class ParfaitCanvasRotationJobIntegrationTest {
    companion object {
        @Container
        @JvmStatic
        val mysql = MySQLContainer("mysql:8.4")

        @Container
        @JvmStatic
        val redis: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mysql.jdbcUrl }
            registry.add("spring.datasource.username") { mysql.username }
            registry.add("spring.datasource.password") { mysql.password }
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
        }
    }

    // @SpringBatchTest는 컨텍스트에 Job 빈이 정확히 1개일 때만 이걸 자동으로 주입해준다.
    // 이 모듈 조합에는 parfaitCanvasRotationJob 하나뿐이라 별도 설정 없이 연결된다.
    @Autowired
    private lateinit var jobOperatorTestUtils: JobOperatorTestUtils

    // persistence 어댑터를 직접 참조하지 않고 core 포트로만 데이터를 셋업/검증한다.
    // 헥사고날 경계를 지키면서도 실제 어댑터(빈)를 그대로 태우므로 배치 자체의 검증 목적에는 충분하다.
    @Autowired
    private lateinit var parfaitGroupSavePort: ParfaitGroupSavePort

    @Autowired
    private lateinit var parfaitSavePort: ParfaitSavePort

    @Autowired
    private lateinit var parfaitQueryPort: ParfaitQueryPort

    @Test
    fun `parfaitCanvasRotationJob을 실제로 실행하면 COMPLETED로 끝나고 어제 ACTIVE 캔버스가 회전된다`() {
        val group =
            parfaitGroupSavePort.save(
                ParfaitGroup.create(name = "회전테스트그룹", inviteCode = InviteCode.of("ROTA01"), memberLimit = 12),
            )
        val groupId = group.requireId()
        val yesterday = LocalDate.now().minusDays(1)
        parfaitSavePort.save(Parfait.createToday(parfaitGroupId = groupId, date = yesterday))

        val jobExecution = jobOperatorTestUtils.startJob(jobOperatorTestUtils.getUniqueJobParameters())

        jobExecution.status shouldBe BatchStatus.COMPLETED

        // 토핑을 하나도 배치하지 않았으므로 어제 캔버스는 EMPTY로 마감되어야 한다.
        val rotated = parfaitQueryPort.findByGroupIdAndDate(groupId, yesterday)
        rotated shouldNotBe null
        rotated?.status?.name shouldBe "EMPTY"

        // 마감 직후 오늘 날짜의 새 ACTIVE 캔버스가 생성되어 있어야 한다.
        val next = parfaitQueryPort.findByGroupIdAndDate(groupId, LocalDate.now())
        next shouldNotBe null
        next?.status?.name shouldBe "ACTIVE"
    }
}
