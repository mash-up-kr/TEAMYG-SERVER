package parfait.common.error

interface BaseErrorCode {
    val status: Int
    val code: String
    val message: String
}
