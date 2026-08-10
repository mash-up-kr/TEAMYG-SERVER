package parfait.http.member.controller

import jakarta.validation.Valid
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.member.port.`in`.ChangeGlobalNicknameUseCase
import parfait.http.member.dto.ChangeGlobalNicknameRequest
import parfait.http.member.dto.ChangeGlobalNicknameResponse

@RestController
@RequestMapping("/api/v1/users/me")
class MemberController(
    private val changeGlobalNicknameUseCase: ChangeGlobalNicknameUseCase,
) {
    @PatchMapping("/nickname")
    fun changeNickname(
        authentication: Authentication,
        @RequestBody @Valid request: ChangeGlobalNicknameRequest,
    ): ApiResponse<ChangeGlobalNicknameResponse> {
        val result = changeGlobalNicknameUseCase.change(authentication.memberId(), request.nickname)
        return ApiResponse.ok(ChangeGlobalNicknameResponse(result.nickname))
    }

    private fun Authentication.memberId(): Long = name.toLong()
}
