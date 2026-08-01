package parfait.http.parfaitgroup

import parfait.common.error.BaseErrorCode
import parfait.core.parfaitgroup.domain.ParfaitGroupError

enum class ParfaitGroupApiErrorCode(
    override val status: Int,
    override val code: String,
    override val message: String,
) : BaseErrorCode {
    INVALID_INVITE_CODE(404, "INVALID_INVITE_CODE", "유효하지 않은 초대코드입니다"),
    GROUP_ALREADY_JOINED(409, "GROUP_ALREADY_JOINED", "이미 참여한 그룹입니다"),
    GROUP_MEMBER_LIMIT_REACHED(409, "GROUP_MEMBER_LIMIT_REACHED", "그룹의 최대 인원이 모두 참여했습니다"),
    GROUP_NICKNAME_ALREADY_USED(409, "GROUP_NICKNAME_ALREADY_USED", "그룹에서 이미 사용 중인 닉네임입니다"),
    INVALID_GROUP_NAME(400, "INVALID_GROUP_NAME", "그룹명이 올바르지 않습니다"),
    INVALID_GROUP_NICKNAME(400, "INVALID_GROUP_NICKNAME", "그룹 닉네임이 올바르지 않습니다"),
    INVALID_GROUP_MEMBER_LIMIT(400, "INVALID_GROUP_MEMBER_LIMIT", "그룹 최대 인원은 1명 이상 12명 이하여야 합니다"),
    MEMBER_NOT_FOUND(404, "MEMBER_NOT_FOUND", "존재하지 않는 회원입니다"),
    GROUP_NOT_FOUND(404, "GROUP_NOT_FOUND", "존재하지 않는 그룹입니다"),
    GROUP_NOT_JOINED(403, "GROUP_NOT_JOINED", "참여하지 않은 그룹입니다"),
    INVALID_GROUP_REPORT_REASON(400, "INVALID_GROUP_REPORT_REASON", "신고 사유를 입력해 주세요"),
    ;

    companion object {
        fun from(error: ParfaitGroupError): ParfaitGroupApiErrorCode = valueOf(error.name)
    }
}
