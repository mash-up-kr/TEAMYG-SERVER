package parfait.external.discord.command

import org.springframework.stereotype.Component

@Component
class CommandDispatcher(
    commands: List<DiscordCommand>,
) {
    private val commandsByName: Map<String, DiscordCommand>

    init {
        val duplicates = commands.groupBy { it.name }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "중복된 슬래시 명령어 이름: $duplicates" }
        commandsByName = commands.associateBy { it.name }
    }

    /** 예외는 던진 채로 두며 호출자(라우터)가 처리한다. */

    fun dispatch(
        name: String,
        request: CommandRequest,
    ): CommandReply {
        val command = commandsByName[name] ?: return CommandReply.text("알 수 없는 명령어입니다", ephemeral = true)
        if (command.adminOnly && !request.isAdmin) {
            return CommandReply.text("운영진만 사용할 수 있는 명령어입니다", ephemeral = true)
        }
        return command.execute(request)
    }
}
