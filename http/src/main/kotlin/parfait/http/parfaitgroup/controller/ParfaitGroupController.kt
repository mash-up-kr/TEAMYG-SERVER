package parfait.http.parfaitgroup.controller

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.parfaitgroup.application.port.`in`.ChangeMyParfaitGroupNicknameUseCase
import parfait.core.parfaitgroup.application.port.`in`.CreateParfaitGroupUseCase
import parfait.core.parfaitgroup.application.port.`in`.GetMyParfaitGroupDetailUseCase
import parfait.core.parfaitgroup.application.port.`in`.GetMyParfaitGroupsUseCase
import parfait.core.parfaitgroup.application.port.`in`.JoinParfaitGroupUseCase
import parfait.core.parfaitgroup.application.port.`in`.LeaveParfaitGroupCommand
import parfait.core.parfaitgroup.application.port.`in`.LeaveParfaitGroupUseCase
import parfait.core.parfaitgroup.application.port.`in`.PreviewParfaitGroupJoinUseCase
import parfait.core.parfaitgroup.application.port.`in`.ReportParfaitGroupUseCase
import parfait.http.parfaitgroup.dto.ChangeMyParfaitGroupNicknameRequest
import parfait.http.parfaitgroup.dto.ChangeMyParfaitGroupNicknameResponse
import parfait.http.parfaitgroup.dto.CreateParfaitGroupRequest
import parfait.http.parfaitgroup.dto.CreateParfaitGroupResponse
import parfait.http.parfaitgroup.dto.JoinParfaitGroupRequest
import parfait.http.parfaitgroup.dto.JoinParfaitGroupResponse
import parfait.http.parfaitgroup.dto.LeaveParfaitGroupResponse
import parfait.http.parfaitgroup.dto.MyParfaitGroupDetailResponse
import parfait.http.parfaitgroup.dto.MyParfaitGroupResponse
import parfait.http.parfaitgroup.dto.PreviewParfaitGroupJoinResponse
import parfait.http.parfaitgroup.dto.ReportParfaitGroupRequest
import parfait.http.parfaitgroup.dto.ReportParfaitGroupResponse

@RestController
@RequestMapping("/api/parfait-groups")
class ParfaitGroupController(
    private val previewParfaitGroupJoinUseCase: PreviewParfaitGroupJoinUseCase,
    private val joinParfaitGroupUseCase: JoinParfaitGroupUseCase,
    private val createParfaitGroupUseCase: CreateParfaitGroupUseCase,
    private val getMyParfaitGroupsUseCase: GetMyParfaitGroupsUseCase,
    private val getMyParfaitGroupDetailUseCase: GetMyParfaitGroupDetailUseCase,
    private val changeMyParfaitGroupNicknameUseCase: ChangeMyParfaitGroupNicknameUseCase,
    private val leaveParfaitGroupUseCase: LeaveParfaitGroupUseCase,
    private val reportParfaitGroupUseCase: ReportParfaitGroupUseCase,
) {
    @GetMapping
    fun getMyGroups(authentication: Authentication): ApiResponse<List<MyParfaitGroupResponse>> =
        ApiResponse.Companion.ok(
            getMyParfaitGroupsUseCase.getAll(authentication.memberId()).map(MyParfaitGroupResponse.Companion::from),
        )

    @GetMapping("/{groupId}")
    fun getMyGroup(
        authentication: Authentication,
        @PathVariable groupId: Long,
    ): ApiResponse<MyParfaitGroupDetailResponse> =
        ApiResponse.Companion.ok(
            MyParfaitGroupDetailResponse.Companion.from(
                getMyParfaitGroupDetailUseCase.get(authentication.memberId(), groupId),
            ),
        )

    @GetMapping("/join-preview")
    fun previewJoin(
        authentication: Authentication,
        @RequestParam inviteCode: String,
    ): ApiResponse<PreviewParfaitGroupJoinResponse> {
        val result = previewParfaitGroupJoinUseCase.preview(authentication.memberId(), inviteCode)
        return ApiResponse.Companion.ok(PreviewParfaitGroupJoinResponse.Companion.from(result))
    }

    @PostMapping("/join")
    fun join(
        authentication: Authentication,
        @RequestBody request: JoinParfaitGroupRequest,
    ): ApiResponse<JoinParfaitGroupResponse> {
        val result = joinParfaitGroupUseCase.join(request.toCommand(authentication.memberId()))
        return ApiResponse.Companion.ok(JoinParfaitGroupResponse.Companion.from(result))
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        authentication: Authentication,
        @RequestBody request: CreateParfaitGroupRequest,
    ): ApiResponse<CreateParfaitGroupResponse> {
        val result = createParfaitGroupUseCase.create(request.toCommand(authentication.memberId()))
        return ApiResponse.Companion.created(CreateParfaitGroupResponse.Companion.from(result))
    }

    @PatchMapping("/{groupId}/nickname")
    fun changeMyNickname(
        authentication: Authentication,
        @PathVariable groupId: Long,
        @RequestBody request: ChangeMyParfaitGroupNicknameRequest,
    ): ApiResponse<ChangeMyParfaitGroupNicknameResponse> =
        ApiResponse.Companion.ok(
            ChangeMyParfaitGroupNicknameResponse.Companion.from(
                changeMyParfaitGroupNicknameUseCase.change(request.toCommand(authentication.memberId(), groupId)),
            ),
        )

    @DeleteMapping("/{groupId}/members/me")
    fun leave(
        authentication: Authentication,
        @PathVariable groupId: Long,
    ): ApiResponse<LeaveParfaitGroupResponse> =
        ApiResponse.Companion.ok(
            LeaveParfaitGroupResponse.Companion.from(
                leaveParfaitGroupUseCase.leave(LeaveParfaitGroupCommand(authentication.memberId(), groupId)),
            ),
        )

    @PostMapping("/{groupId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    fun report(
        authentication: Authentication,
        @PathVariable groupId: Long,
        @RequestBody request: ReportParfaitGroupRequest,
    ): ApiResponse<ReportParfaitGroupResponse> =
        ApiResponse.Companion.created(
            ReportParfaitGroupResponse.Companion.from(
                reportParfaitGroupUseCase.report(request.toCommand(authentication.memberId(), groupId)),
            ),
        )

    private fun Authentication.memberId(): Long = name.toLong()
}
