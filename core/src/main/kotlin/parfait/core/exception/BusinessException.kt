package parfait.core.exception

import parfait.common.error.BaseErrorCode

open class BusinessException(
    val errorCode: BaseErrorCode,
) : RuntimeException(errorCode.message)
