package parfait.external.discord.command

import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

/** 슬래시 명령어 하나. 구현체를 `@Component`로 등록하면 라우터·등록기·`/help`가 자동으로 인식한다.
 *
 * 명령어 실행은 반드시 CommandDispatcher를 거쳐야 한다(adminOnly 검사가 디스패처 한 곳에만 있다).
 * definition()만 JDA 타입을 노출한다.
 */
interface DiscordCommand {
    val name: String

    /** `/help` 에 보여줄 사용법 (예: `/가입자 [기간]`) */
    val usage: String
    val description: String

    /** true 면 운영진 역할이 있는 사용자만 쓸 수 있다. */
    val adminOnly: Boolean

    fun definition(): SlashCommandData

    fun execute(request: CommandRequest): CommandReply
}
