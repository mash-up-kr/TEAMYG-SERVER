package parfait.core.auth.port.out

interface KakaoIdTokenVerifyPort {
    fun verify(
        idToken: String,
        nonce: String,
    ): String
}
