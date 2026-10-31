package parfait.external.discord.bot

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/** JDA 생성 지점. 컨텍스트 테스트가 실제 디스코드 연결 없이 교체할 수 있도록 분리했다. */
fun interface JdaFactory {
    fun create(
        token: String,
        listeners: List<ListenerAdapter>,
    ): JDA
}

internal object DefaultJdaFactory : JdaFactory {
    private const val EVENT_POOL_SIZE = 4

    override fun create(
        token: String,
        listeners: List<ListenerAdapter>,
    ): JDA =
        JDABuilder
            .createLight(token, emptyList())
            .setEventPool(newEventPool(), true)
            .addEventListeners(*listeners.toTypedArray())
            .build()

    /** 슬래시 명령어 처리가 게이트웨이 읽기 스레드를 막지 않도록 분리한 이벤트 풀(JDA 종료 시 함께 닫힌다). */
    private fun newEventPool(): ExecutorService {
        val counter = AtomicInteger()
        return Executors.newFixedThreadPool(EVENT_POOL_SIZE) { task ->
            Thread(task, "discord-event-${counter.incrementAndGet()}").apply { isDaemon = true }
        }
    }
}
