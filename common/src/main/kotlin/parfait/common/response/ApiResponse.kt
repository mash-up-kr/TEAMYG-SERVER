package parfait.common.response

import parfait.common.error.BaseErrorCode

@ConsistentCopyVisibility
data class ApiResponse<T> private constructor(
    val success: Boolean,
    val code: String,
    val message: String,
    val data: T?,
) {
    companion object {
        fun <T> ok(data: T): ApiResponse<T> =
            ApiResponse(
                success = true,
                code = "OK",
                message = "요청이 성공적으로 처리되었습니다",
                data = data,
            )

        fun <T> created(data: T): ApiResponse<T> =
            ApiResponse(
                success = true,
                code = "CREATED",
                message = "생성되었습니다",
                data = data,
            )

        fun error(errorCode: BaseErrorCode): ApiResponse<Nothing> =
            ApiResponse(
                success = false,
                code = errorCode.code,
                message = errorCode.message,
                data = null,
            )
    }
}
