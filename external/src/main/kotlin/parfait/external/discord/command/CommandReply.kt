package parfait.external.discord.command

import net.dv8tion.jda.api.entities.MessageEmbed

data class CommandReply(
    val text: String? = null,
    val embed: MessageEmbed? = null,
    val ephemeral: Boolean,
) {
    init {
        require((text == null) != (embed == null)) { "text 와 embed 중 정확히 하나만 있어야 한다" }
    }

    companion object {
        fun text(
            text: String,
            ephemeral: Boolean,
        ) = CommandReply(text = text, ephemeral = ephemeral)

        fun embed(
            embed: MessageEmbed,
            ephemeral: Boolean,
        ) = CommandReply(embed = embed, ephemeral = ephemeral)
    }
}
