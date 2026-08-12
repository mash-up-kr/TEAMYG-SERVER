package parfait.http.member.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.member.port.`in`.ChangeGlobalNicknameUseCase
import parfait.core.member.port.`in`.GetMyAccountUseCase
import parfait.core.member.port.`in`.WithdrawUseCase
import parfait.http.member.dto.ChangeGlobalNicknameRequest
import parfait.http.member.dto.ChangeGlobalNicknameResponse
import parfait.http.member.dto.MyAccountResponse

@Tag(name = "Member")
@RestController
@RequestMapping("/api/v1/users/me")
class MemberController(
    private val changeGlobalNicknameUseCase: ChangeGlobalNicknameUseCase,
    private val withdrawUseCase: WithdrawUseCase,
    private val getMyAccountUseCase: GetMyAccountUseCase,
) {
    @Operation(summary = "내 계정 정보 조회")
    @GetMapping
    fun getMyAccount(authentication: Authentication): ApiResponse<MyAccountResponse> {
        val result = getMyAccountUseCase.getMyAccount(authentication.memberId())
        return ApiResponse.ok(MyAccountResponse(result.memberId, result.provider.name, result.nickname))
    }

    @Operation(summary = "전역 닉네임 변경")
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
