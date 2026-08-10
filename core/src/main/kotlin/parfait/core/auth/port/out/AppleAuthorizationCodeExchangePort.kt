package parfait.core.auth.port.out

interface AppleAuthorizationCodeExchangePort {
    fun exchange(authorizationCode: String): String
}
