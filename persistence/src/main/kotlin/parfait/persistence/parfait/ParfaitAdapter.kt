package parfait.persistence.parfait

import org.springframework.stereotype.Component
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfait.port.out.ParfaitSummary
import parfait.persistence.repository.ParfaitRepository
import java.time.LocalDate

@Component
class ParfaitAdapter(
    private val parfaitRepository: ParfaitRepository,
) : ParfaitQueryPort {
    override fun findDistinctYearsByGroupId(groupId: Long): List<Int> =
        parfaitRepository.findDistinctYearsByParfaitGroupId(groupId)

    override fun existsByIdAndGroupId(
        parfaitId: Long,
        groupId: Long,
    ): Boolean = parfaitRepository.existsByIdAndParfaitGroupId(parfaitId, groupId)

    override fun findAllByGroupIdAndDateRange(
        groupId: Long,
        from: LocalDate,
        to: LocalDate,
    ): List<ParfaitSummary> =
        parfaitRepository.findAllByParfaitGroupIdAndParfaitDateBetweenOrderByParfaitDateDesc(groupId, from, to).map {
            ParfaitSummary(id = requireNotNull(it.id), date = it.parfaitDate)
        }
}
