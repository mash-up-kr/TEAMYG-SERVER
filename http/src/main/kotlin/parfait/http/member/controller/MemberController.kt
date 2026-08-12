package parfait.http.member.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.member.port.`in`.ChangeGlobalNicknameUseCase
import parfait.core.member.port.`in`.WithdrawUseCase
import parfait.http.member.dto.ChangeGlobalNicknameRequest
import parfait.http.member.dto.ChangeGlobalNicknameResponse

@Tag(name = "Member")
@RestController
@RequestMapping("/api/v1/users/me")
class MemberController(
    private val changeGlobalNicknameUseCase: ChangeGlobalNicknameUseCase,
    private val withdrawUseCase: WithdrawUseCase,
) {
    @PatchMapping("/nickname")
    fun changeNickname(
        authentication: Authentication,
        @RequestBody @Valid request: ChangeGlobalNicknameRequest,
    ): ApiResponse<ChangeGlobalNicknameResponse> {
        val result = changeGlobalNicknameUseCase.change(authentication.memberId(), request.nickname)
        return ApiResponse.ok(ChangeGlobalNicknameResponse(result.nickname))
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun withdraw(authentication: Authentication) {
        withdrawUseCase.withdraw(authentication.memberId())
    }

    private fun Authentication.memberId(): Long = name.toLong()
}
