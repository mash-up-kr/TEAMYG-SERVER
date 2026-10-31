package parfait.external.discord.command

/** JDA 이벤트에서 명령어 로직에 필요한 값만 뽑은 요청. 명령어 로직이 JDA 타입에 의존하지 않게 한다. */
data class CommandRequest(
    val subcommand: String?,
    val options: Map<String, String>,
    val isAdmin: Boolean,
)
