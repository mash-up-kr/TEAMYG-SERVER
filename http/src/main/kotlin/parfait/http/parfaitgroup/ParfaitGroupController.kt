package parfait.http.parfaitgroup

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
        ApiResponse.ok(
            getMyParfaitGroupsUseCase.getAll(authentication.memberId()).map(MyParfaitGroupResponse::from),
        )

    @GetMapping("/{groupId}")
    fun getMyGroup(
        authentication: Authentication,
        @PathVariable groupId: Long,
    ): ApiResponse<MyParfaitGroupDetailResponse> =
        ApiResponse.ok(
            MyParfaitGroupDetailResponse.from(
                getMyParfaitGroupDetailUseCase.get(authentication.memberId(), groupId),
            ),
        )

    @GetMapping("/join-preview")
    fun previewJoin(
        authentication: Authentication,
        @RequestParam inviteCode: String,
    ): ApiResponse<PreviewParfaitGroupJoinResponse> {
        val result = previewParfaitGroupJoinUseCase.preview(authentication.memberId(), inviteCode)
        return ApiResponse.ok(PreviewParfaitGroupJoinResponse.from(result))
    }

    @PostMapping("/join")
    fun join(
        authentication: Authentication,
        @RequestBody request: JoinParfaitGroupRequest,
    ): ApiResponse<JoinParfaitGroupResponse> {
        val result = joinParfaitGroupUseCase.join(request.toCommand(authentication.memberId()))
        return ApiResponse.ok(JoinParfaitGroupResponse.from(result))
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        authentication: Authentication,
        @RequestBody request: CreateParfaitGroupRequest,
    ): ApiResponse<CreateParfaitGroupResponse> {
        val result = createParfaitGroupUseCase.create(request.toCommand(authentication.memberId()))
        return ApiResponse.created(CreateParfaitGroupResponse.from(result))
    }

    @PatchMapping("/{groupId}/nickname")
    fun changeMyNickname(
        authentication: Authentication,
        @PathVariable groupId: Long,
        @RequestBody request: ChangeMyParfaitGroupNicknameRequest,
    ): ApiResponse<ChangeMyParfaitGroupNicknameResponse> =
        ApiResponse.ok(
            ChangeMyParfaitGroupNicknameResponse.from(
                changeMyParfaitGroupNicknameUseCase.change(request.toCommand(authentication.memberId(), groupId)),
            ),
        )

    @DeleteMapping("/{groupId}/members/me")
    fun leave(
        authentication: Authentication,
        @PathVariable groupId: Long,
    ): ApiResponse<LeaveParfaitGroupResponse> =
        ApiResponse.ok(
            LeaveParfaitGroupResponse.from(
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
        ApiResponse.created(
            ReportParfaitGroupResponse.from(
                reportParfaitGroupUseCase.report(request.toCommand(authentication.memberId(), groupId)),
            ),
        )

    private fun Authentication.memberId(): Long = name.toLong()
}
