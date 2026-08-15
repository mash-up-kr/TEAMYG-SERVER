package parfait.core.parfaitgroup.domain

enum class ParfaitGroupError {
    INVALID_INVITE_CODE,
    GROUP_ALREADY_JOINED,
    GROUP_MEMBER_LIMIT_REACHED,
    INVALID_GROUP_NAME,
    INVALID_GROUP_NICKNAME,
    INVALID_GROUP_MEMBER_LIMIT,
    MEMBER_NOT_FOUND,
    GROUP_NOT_FOUND,
    GROUP_NOT_JOINED,
    INVALID_GROUP_REPORT_REASON,
}

class ParfaitGroupException(
    val error: ParfaitGroupError,
) : RuntimeException(error.name)
