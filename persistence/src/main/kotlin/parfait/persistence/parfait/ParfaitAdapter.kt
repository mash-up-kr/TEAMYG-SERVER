package parfait.persistence.parfait

import org.springframework.stereotype.Component
import parfait.core.parfait.domain.BackgroundType
import parfait.core.parfait.domain.Parfait
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfait.port.out.ParfaitSavePort
import parfait.core.parfait.port.out.ParfaitSummary
import parfait.persistence.repository.ParfaitRepository
import java.time.LocalDate
import parfait.persistence.entity.Parfait as ParfaitEntity

@Component
class ParfaitAdapter(
    private val parfaitRepository: ParfaitRepository,
) : ParfaitQueryPort,
    ParfaitSavePort {
    override fun findDistinctYearsByGroupId(groupId: Long): List<Int> =
        parfaitRepository.findDistinctYearsByParfaitGroupId(groupId)

    override fun findAllByGroupIdAndDateRange(
        groupId: Long,
        from: LocalDate,
        to: LocalDate,
    ): List<ParfaitSummary> =
        parfaitRepository.findAllByParfaitGroupIdAndParfaitDateBetweenOrderByParfaitDateDesc(groupId, from, to).map {
            ParfaitSummary(id = requireNotNull(it.id), date = it.parfaitDate)
        }

    override fun findByGroupIdAndDate(
        groupId: Long,
        date: LocalDate,
    ): Parfait? = parfaitRepository.findByParfaitGroupIdAndParfaitDate(groupId, date)?.toDomain()

    override fun findActiveByGroupId(groupId: Long): Parfait? =
        parfaitRepository
            .findTopByParfaitGroupIdAndStatusOrderByParfaitDateDesc(groupId, ParfaitStatus.ACTIVE.name)
            ?.toDomain()

    override fun findLastClosedDateByGroupId(groupId: Long): LocalDate? =
        parfaitRepository
            .findTopByParfaitGroupIdAndStatusOrderByParfaitDateDesc(groupId, ParfaitStatus.CLOSED.name)
            ?.parfaitDate

    override fun findByIdAndGroupId(
        parfaitId: Long,
        groupId: Long,
    ): Parfait? = parfaitRepository.findByIdAndParfaitGroupId(parfaitId, groupId)?.toDomain()

    override fun save(parfait: Parfait): Parfait = parfaitRepository.save(parfait.toEntity()).toDomain()

    private fun Parfait.toEntity(): ParfaitEntity =
        ParfaitEntity(
            parfaitGroupId = parfaitGroupId,
            parfaitDate = parfaitDate,
            status = status.name,
            backgroundType = backgroundType?.name,
            backgroundValue = backgroundValue,
            createdAt = createdAt,
            updatedAt = updatedAt,
            id = id,
        )

    private fun ParfaitEntity.toDomain(): Parfait =
        Parfait.reconstitute(
            id = requireNotNull(id),
            parfaitGroupId = parfaitGroupId,
            parfaitDate = parfaitDate,
            status = ParfaitStatus.valueOf(status),
            backgroundType = backgroundType?.let { BackgroundType.valueOf(it) },
            backgroundValue = backgroundValue,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
