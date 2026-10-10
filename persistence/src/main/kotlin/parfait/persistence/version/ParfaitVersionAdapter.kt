package parfait.persistence.version

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import parfait.core.exception.BusinessException
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.out.ParfaitVersionPort
import parfait.persistence.repository.ParfaitRepository

@Component
@Transactional
class ParfaitVersionAdapter(
    private val parfaitRepository: ParfaitRepository,
) : ParfaitVersionPort {
    @Transactional(readOnly = true)
    override fun getVersion(parfaitId: Long): Long =
        parfaitRepository.findVersionById(parfaitId) ?: throw BusinessException(ParfaitErrorCode.PARFAIT_NOT_FOUND)

    override fun bump(parfaitId: Long) {
        if (parfaitRepository.incrementVersion(parfaitId) == 0) {
            throw BusinessException(ParfaitErrorCode.PARFAIT_NOT_FOUND)
        }
    }
}
