package parfait.persistence.parfait

import org.springframework.stereotype.Component
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.persistence.repository.ParfaitRepository

@Component
class ParfaitAdapter(
    private val parfaitRepository: ParfaitRepository,
) : ParfaitQueryPort {
    override fun findDistinctYearsByGroupId(groupId: Long): List<Int> =
        parfaitRepository.findDistinctYearsByParfaitGroupId(groupId)
}
