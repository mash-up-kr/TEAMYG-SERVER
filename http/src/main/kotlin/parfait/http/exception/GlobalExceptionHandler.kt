package parfait.http.exception

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import parfait.common.error.CommonErrorCode
import parfait.common.response.ApiResponse
import parfait.core.exception.BusinessException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        log.info("BusinessException: {}", e.errorCode.code, e)
        return ResponseEntity
            .status(e.errorCode.status)
            .body(ApiResponse.error(e.errorCode))
    }

    @ExceptionHandler(
        MissingServletRequestParameterException::class,
        HttpMessageNotReadableException::class,
        MethodArgumentTypeMismatchException::class,
        MethodArgumentNotValidException::class,
    )
    fun handleBadRequest(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.info("Bad request: {}", e.message)
        return ResponseEntity
            .status(CommonErrorCode.INVALID_REQUEST.status)
            .body(ApiResponse.error(CommonErrorCode.INVALID_REQUEST))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.error("Unhandled exception", e)
        return ResponseEntity
            .status(CommonErrorCode.INTERNAL_SERVER_ERROR.status)
            .body(ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR))
    }
}
