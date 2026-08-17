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
            // "오늘"은 자정이 아니라 ParfaitDay 기준(새벽 3시)이어야 한다. 캘린더 LocalDate.now()를
            // 쓰면(isAfter(오늘)만 걸러내는 예전 방식) 인증 없는 테스트 트리거 엔드포인트가 하루
            // 중 아무 때나(새벽 3시 이전이든 아니든) rotateOne을 한 번 더 부르는 순간 활성 캔버스
            // 날짜가 "진짜 오늘+1"로 앞서가버리고, 그 뒤로는 매 새벽 3시 배치마다 그 날짜가 항상
            // LocalDate.now()와 정확히 같아서 isAfter가 절대 못 막아 영구적으로 하루씩 계속
            // 밀린다. ParfaitDay 기준(실제로 그 영업일이 지났는지)으로 판단해야 몇 번을 호출해도
            // 안전하다.
        }

        return closeAndCreateNext(groupId, active, now)
    }

    /**
     * 테스트 트리거 엔드포인트 전용: rotateOne의 ParfaitDay 가드를 건너뛰고 활성 캔버스를
     * 즉시 마감·다음 날짜 캔버스를 생성한다. 정식 배치는 이 메서드를 쓰지 않는다 — 정식 배치가
     * 쓰는 rotateOne의 가드가 안전하게 막아주는 덕분에, 여기서 하루 앞당겨 만든 캔버스도 실제로
     * 그 날짜가 되면 정식 배치가 다시 건드리지 않는다(영구 드리프트로 이어지지 않는다).
     */
    @Transactional
    fun forceRotateOne(
        groupId: Long,
        now: LocalDateTime = LocalDateTime.now(),
    ): RotateOneResult? {
        val active = parfaitQueryPort.findActiveByGroupId(groupId) ?: return null

        return closeAndCreateNext(groupId, active, now)
    }

    private fun closeAndCreateNext(
        groupId: Long,
        active: Parfait,
        now: LocalDateTime,
    ): RotateOneResult {
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
