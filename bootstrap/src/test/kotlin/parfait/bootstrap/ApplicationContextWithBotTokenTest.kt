package parfait.bootstrap

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import parfait.external.discord.bot.CommandRegistrar
import parfait.external.discord.bot.CommandRouter
import parfait.external.discord.bot.DiscordBot
import parfait.external.discord.bot.JdaFactory
import parfait.external.discord.command.CommandDispatcher
import parfait.external.discord.command.admode.AdModeCommand
import parfait.external.discord.command.help.HelpCommand
import parfait.external.discord.command.reportnow.ReportNowCommand
import parfait.external.discord.command.signup.SignupCommand
import parfait.external.discord.report.DisabledReportSender
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 봇 토큰이 설정된 운영 조합에서 컨텍스트가 순환 의존 없이 뜨는지 검증한다.
 * 실제 디스코드 연결은 가짜 [JdaFactory] 로 막는다.
 */
@Testcontainers
@SpringBootTest(
    classes = [ParfaitApplication::class, ApplicationContextWithBotTokenTest.FakeJdaConfig::class],
    properties = [
        "jwt.secret-key=smoke-test-only-dummy-jwt-secret-key-32-bytes-min",
        "discord.bot.token=dummy",
        "discord.bot.guild-id=1",
        "discord.bot.report-channel-id=1",
    ],
)
class ApplicationContextWithBotTokenTest {
    @TestConfiguration
    class FakeJdaConfig {
        @Bean
        fun jdaFactory(): JdaFactory =
            JdaFactory { _, listeners ->
                created.add(listeners)
                throw IllegalStateException("테스트: 실제 디스코드 연결 안 함")
            }
    }

    companion object {
        val created = CopyOnWriteArrayList<List<ListenerAdapter>>()

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

    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun `토큰이 있으면 컨텍스트가 순환 의존 없이 뜨고 봇 관련 빈이 등록된다`() {
        context.getBeansOfType(DiscordBot::class.java).size shouldBe 1
        context.getBeansOfType(CommandRouter::class.java).size shouldBe 1
        context.getBeansOfType(CommandRegistrar::class.java).size shouldBe 1
        context.getBeansOfType(DisabledReportSender::class.java).size shouldBe 0
        listOf(AdModeCommand::class, HelpCommand::class, ReportNowCommand::class, SignupCommand::class)
            .forEach { context.getBeansOfType(it.java).size shouldBe 1 }
        context.getBeansOfType(CommandDispatcher::class.java).size shouldBe 1
    }

    @Test
    fun `기동 완료 후 JDA 가 라우터와 등록기를 리스너로 하여 생성된다`() {
        created shouldHaveSize 1
        created.single().map { it::class } shouldBe listOf(CommandRouter::class, CommandRegistrar::class)
    }
}
