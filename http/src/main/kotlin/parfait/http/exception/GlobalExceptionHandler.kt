package parfait.http.exception

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
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

    // 도메인에서 던진 BusinessException
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("BusinessException: {}", e.errorCode.code, e)
        return ResponseEntity
            .status(e.errorCode.status)
            .body(ApiResponse.error(e.errorCode))
    }

    // MissingServletRequestParameterException(필수 파라미터 누락), HttpMessageNotReadableException(요청 본문 형식 오류),
    // MethodArgumentTypeMismatchException(파라미터 타입 불일치)
    @ExceptionHandler(
        MissingServletRequestParameterException::class,
        HttpMessageNotReadableException::class,
        MethodArgumentTypeMismatchException::class,
    )
    fun handleBadRequest(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("Bad request: {}", e.message)
        return ResponseEntity
            .status(CommonErrorCode.INVALID_REQUEST.status)
            .body(ApiResponse.error(CommonErrorCode.INVALID_REQUEST))
    }

    // 그 외 예상 못한 모든 예외 — 최후의 안전망.
    // 실제 컨트롤러가 생기기 전까지는 영향이 없어 의도적으로 보류함 —
    // 첫 실제 API 작성 시 ResponseEntityExceptionHandler 상속으로 재설계 필요.
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.error("Unhandled exception", e)
        return ResponseEntity
            .status(CommonErrorCode.INTERNAL_SERVER_ERROR.status)
            .body(ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR))
    }
}
