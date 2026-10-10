package parfait.persistence.version

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupVersionPort
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.persistence.repository.ParfaitGroupRepository

@Component
@Transactional
class ParfaitGroupVersionAdapter(
    private val parfaitGroupRepository: ParfaitGroupRepository,
) : ParfaitGroupVersionPort {
    @Transactional(readOnly = true)
    override fun getVersion(groupId: Long): Long =
        parfaitGroupRepository.findVersionById(groupId)
            ?: throw ParfaitGroupException(ParfaitGroupError.GROUP_NOT_FOUND)

    override fun bump(groupId: Long) {
        if (parfaitGroupRepository.incrementVersion(groupId) == 0) {
            throw ParfaitGroupException(ParfaitGroupError.GROUP_NOT_FOUND)
        }
    }
}
