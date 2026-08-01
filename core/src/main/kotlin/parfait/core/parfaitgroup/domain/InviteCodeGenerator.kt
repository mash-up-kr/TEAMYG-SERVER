package parfait.core.parfaitgroup.domain

import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class InviteCodeGenerator {
    private val random = SecureRandom()

    fun generate(): InviteCode {
        val code =
            buildString(InviteCode.LENGTH) {
                repeat(InviteCode.LENGTH) {
                    append(ALPHABET[random.nextInt(ALPHABET.length)])
                }
            }
        return InviteCode.generated(code)
    }

    private companion object {
        const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}
