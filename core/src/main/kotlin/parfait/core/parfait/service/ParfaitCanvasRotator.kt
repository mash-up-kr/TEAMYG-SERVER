package parfait.core.parfait.service

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import parfait.core.parfait.domain.Parfait
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfait.port.out.ParfaitSavePort
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort
import java.time.LocalDate

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
    fun rotateOne(groupId: Long): RotateOneResult? {
        val active = parfaitQueryPort.findActiveByGroupId(groupId) ?: return null

        if (!active.parfaitDate.isBefore(LocalDate.now())) {
            return null // 가드 1: 오늘 또는 미래 날짜 캔버스는 아직 마감 대상이 아님.
            // isAfter(오늘)만 걸러내면 "오늘" 날짜는 통과해버린다 — 자정~새벽 3시 사이
            // ensure()로 오늘 캔버스가 이미 활성화된 뒤 배치가 도는 경우, 방금 만든 오늘
            // 캔버스를 마감하고 내일 날짜 캔버스를 또 만들어버리는 버그로 이어진다.
        }

        val hasToppings = parfaitImageQueryPort.existsByParfaitId(active.requireId())
        val closed = if (hasToppings) active.close() else active.markEmpty()
        parfaitSavePort.save(closed)

        val targetDate = closed.parfaitDate.plusDays(1)
        val createdNew =
            if (parfaitQueryPort.findByGroupIdAndDate(groupId, targetDate) != null) {
                false // 가드 2: 해당 날짜 캔버스가 이미 있음(어떤 상태든) — 생성 생략
            } else {
                parfaitSavePort.save(Parfait.createToday(parfaitGroupId = groupId, date = targetDate))
                true
            }
        return RotateOneResult(wasEmpty = !hasToppings, created = createdNew)
    }
}

data class RotateOneResult(
    val wasEmpty: Boolean,
    val created: Boolean,
)
