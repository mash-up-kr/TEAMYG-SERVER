package parfait.http.parfait.controller

import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.parfait.port.`in`.GetParfaitDetailCommand
import parfait.core.parfait.port.`in`.GetParfaitDetailUseCase
import parfait.core.parfait.port.`in`.GetParfaitYearsUseCase
import parfait.core.parfait.port.`in`.GetPastParfaitsCommand
import parfait.core.parfait.port.`in`.GetPastParfaitsUseCase
import parfait.core.parfait.port.`in`.GetTodayParfaitCommand
import parfait.core.parfait.port.`in`.GetTodayParfaitUseCase
import parfait.http.parfait.dto.GetTodayParfaitResponse
import parfait.http.parfait.dto.ParfaitYearsResponse
import parfait.http.parfait.dto.PastParfaitsResponse
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/groups/{groupId}/parfaits")
class ParfaitController(
    private val getParfaitYearsUseCase: GetParfaitYearsUseCase,
    private val getPastParfaitsUseCase: GetPastParfaitsUseCase,
    private val getTodayParfaitUseCase: GetTodayParfaitUseCase,
    private val getParfaitDetailUseCase: GetParfaitDetailUseCase,
) {
    @GetMapping("/year")
    fun getYears(
        authentication: Authentication,
        @PathVariable groupId: Long,
    ): ApiResponse<ParfaitYearsResponse> =
        ApiResponse.ok(
            ParfaitYearsResponse(getParfaitYearsUseCase.getYears(authentication.memberId(), groupId)),
        )

    @GetMapping("/today")
    fun getToday(
        authentication: Authentication,
        @PathVariable groupId: Long,
    ): ApiResponse<GetTodayParfaitResponse> =
        ApiResponse.ok(
            GetTodayParfaitResponse.from(
                getTodayParfaitUseCase.get(
                    GetTodayParfaitCommand(memberId = authentication.memberId(), groupId = groupId),
                ),
            ),
        )

    @GetMapping
    fun getPastParfaits(
        authentication: Authentication,
        @PathVariable groupId: Long,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
    ): ApiResponse<PastParfaitsResponse> =
        ApiResponse.ok(
            PastParfaitsResponse.from(
                getPastParfaitsUseCase.getPastParfaits(
                    GetPastParfaitsCommand(
                        memberId = authentication.memberId(),
                        groupId = groupId,
                        from = from,
                        to = to,
                    ),
                ),
            ),
        )

    @GetMapping("/{parfaitId}")
    fun getDetail(
        authentication: Authentication,
        @PathVariable groupId: Long,
        @PathVariable parfaitId: Long,
    ): ApiResponse<GetTodayParfaitResponse> =
        ApiResponse.ok(
            GetTodayParfaitResponse.from(
                getParfaitDetailUseCase.getDetail(
                    GetParfaitDetailCommand(
                        memberId = authentication.memberId(),
                        groupId = groupId,
                        parfaitId = parfaitId,
                    ),
                ),
            ),
        )

    private fun Authentication.memberId(): Long = name.toLong()
}
