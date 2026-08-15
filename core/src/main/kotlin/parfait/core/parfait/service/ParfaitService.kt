package parfait.core.parfait.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.parfait.domain.Parfait
import parfait.core.parfait.port.`in`.EnsureActiveCanvasUseCase
import parfait.core.parfait.port.`in`.GetParfaitYearsUseCase
import parfait.core.parfait.port.`in`.RotateParfaitCanvasesResult
import parfait.core.parfait.port.`in`.RotateParfaitCanvasesUseCase
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfait.port.out.ParfaitSavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import java.time.LocalDate

@Service
class ParfaitService(
    private val parfaitQueryPort: ParfaitQueryPort,
    private val parfaitGroupMemberQueryPort: ParfaitGroupMemberQueryPort,
    private val parfaitSavePort: ParfaitSavePort,
    private val parfaitGroupQueryPort: ParfaitGroupQueryPort,
    private val parfaitCanvasRotator: ParfaitCanvasRotator,
    @Qualifier("canvasRotationRetryTemplate") private val retryTemplate: RetryTemplate,
) : GetParfaitYearsUseCase,
    EnsureActiveCanvasUseCase,
    RotateParfaitCanvasesUseCase {
    private val log = LoggerFactory.getLogger(ParfaitService::class.java)

    @Transactional(readOnly = true)
    override fun getYears(
        memberId: Long,
        groupId: Long,
    ): List<Int> {
        if (!parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(groupId, memberId)) {
            throw ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)
        }
        return parfaitQueryPort.findDistinctYearsByGroupId(groupId)
    }

    @Transactional
    override fun ensure(
        groupId: Long,
        targetDate: LocalDate,
    ): Parfait =
        parfaitQueryPort.findByGroupIdAndDate(groupId, targetDate)
            ?: parfaitSavePort.save(Parfait.createToday(parfaitGroupId = groupId, date = targetDate))

    override fun rotateAll(): RotateParfaitCanvasesResult {
        var closed = 0
        var empty = 0
        var created = 0
        var failed = 0
        parfaitGroupQueryPort.findAllIds().forEach { groupId ->
            try {
                retryTemplate.execute<Unit, Exception> {
                    parfaitCanvasRotator.rotateOne(groupId)?.let { result ->
                        if (result.wasEmpty) empty++ else closed++
                        if (result.created) created++
                    }
                }
            } catch (e: Exception) {
                failed++
                log.error("캔버스 회전 실패, 재시도 소진 - groupId={}", groupId, e)
            }
        }
        return RotateParfaitCanvasesResult(closed, empty, created, failed)
    }
}
