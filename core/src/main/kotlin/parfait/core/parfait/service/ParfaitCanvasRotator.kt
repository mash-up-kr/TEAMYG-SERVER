package parfait.core.parfait.service

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import parfait.core.parfait.domain.Parfait
import parfait.core.parfait.domain.ParfaitDay
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfait.port.out.ParfaitSavePort
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort
import java.time.LocalDateTime

@Component
class ParfaitCanvasRotator(
    private val parfaitQueryPort: ParfaitQueryPort,
    private val parfaitImageQueryPort: ParfaitImageQueryPort,
    private val parfaitSavePort: ParfaitSavePort,
) {
    /**
     * "마감 후 생성" 순서 자체가 그룹당 ACTIVE 파르페를 최대 1개로 유지하는 근거다.
     * [parfait.core.parfait.port.`in`.EnsureActiveCanvasUseCase.ensure]와
     * [ParfaitQueryPort.findActiveByGroupId]는 이 순서가 지켜진다는 전제에 기대고 있으므로,
     * 새 캔버스 생성을 먼저 하거나 마감을 생략하는 방향으로 바꾸지 말 것.
     */
    @Transactional
    fun rotateOne(
        groupId: Long,
        now: LocalDateTime = LocalDateTime.now(),
    ): RotateOneResult? {
        val active = parfaitQueryPort.findActiveByGroupId(groupId) ?: return null

        val today = ParfaitDay.current(now)
        if (!active.parfaitDate.isBefore(today)) {
            return null // 가드 1: 오늘 또는 미래 날짜 캔버스는 아직 마감 대상이 아님.
            // "오늘"은 자정이 아니라 ParfaitDay 기준(새벽 3시)이다 — 캘린더 LocalDate.now()를
            // 쓰면 새벽 3시 이전에(예: 테스트 트리거 엔드포인트) 호출됐을 때 아직 끝나지 않은
            // 전날 캔버스를 조기에 마감해버린다.
        }

        val hasToppings = parfaitImageQueryPort.existsByParfaitId(active.requireId())
        val closed = if (hasToppings) active.close(now) else active.markEmpty(now)
        parfaitSavePort.save(closed)

        val targetDate = closed.parfaitDate.plusDays(1)
        val createdNew =
            if (parfaitQueryPort.findByGroupIdAndDate(groupId, targetDate) != null) {
                false // 가드 2: 해당 날짜 캔버스가 이미 있음(어떤 상태든) — 생성 생략
            } else {
                parfaitSavePort.save(Parfait.createToday(parfaitGroupId = groupId, date = targetDate, now = now))
                true
            }
        return RotateOneResult(wasEmpty = !hasToppings, created = createdNew)
    }
}

data class RotateOneResult(
    val wasEmpty: Boolean,
    val created: Boolean,
)
