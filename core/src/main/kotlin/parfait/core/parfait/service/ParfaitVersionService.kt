package parfait.core.parfait.service

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import parfait.core.parfait.event.ParfaitVersionBumpedEvent
import parfait.core.parfait.port.out.ParfaitVersionPort

/**
 * 캔버스(그림)를 바꾸는 모든 쓰기는 반드시 같은 트랜잭션 안에서 이 메서드를 호출해야 한다.
 * 빠뜨리면 v2 today 가 계속 304 만 내려 다른 사용자 화면에 변경이 반영되지 않는다.
 */
@Service
class ParfaitVersionService(
    private val parfaitVersionPort: ParfaitVersionPort,
    private val eventPublisher: ApplicationEventPublisher,
) {
    // 트랜잭션 밖에서 호출되면 AFTER_COMMIT 무효화 이벤트가 조용히 버려지므로, 진행 중인 트랜잭션을 요구한다.
    @Transactional(propagation = Propagation.MANDATORY)
    fun bump(parfaitId: Long) {
        parfaitVersionPort.bump(parfaitId)
        eventPublisher.publishEvent(ParfaitVersionBumpedEvent(parfaitId))
    }
}
