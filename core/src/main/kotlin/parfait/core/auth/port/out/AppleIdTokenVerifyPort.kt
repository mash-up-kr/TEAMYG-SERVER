package parfait.core.auth.port.out

interface AppleIdTokenVerifyPort {
    fun verify(
        identityToken: String,
        nonce: String,
    ): String
}
