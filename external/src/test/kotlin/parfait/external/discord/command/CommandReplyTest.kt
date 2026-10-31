package parfait.external.discord.command

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.dv8tion.jda.api.EmbedBuilder
import org.junit.jupiter.api.Test

class CommandReplyTest {
    private val embed = EmbedBuilder().setTitle("t").build()

    @Test
    fun `text만 있으면 정상이다`() {
        CommandReply.text("a", ephemeral = true).text shouldBe "a"
    }

    @Test
    fun `embed만 있으면 정상이다`() {
        CommandReply.embed(embed, ephemeral = false).embed shouldBe embed
    }

    @Test
    fun `text와 embed가 모두 없으면 예외다`() {
        shouldThrow<IllegalArgumentException> { CommandReply(ephemeral = true) }
    }

    @Test
    fun `text와 embed가 모두 있으면 예외다`() {
        shouldThrow<IllegalArgumentException> { CommandReply(text = "a", embed = embed, ephemeral = true) }
    }
}
