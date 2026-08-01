package parfait.persistence.adapter

import org.springframework.stereotype.Component
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupReportSavePort
import parfait.core.parfaitgroup.domain.ParfaitGroupReport
import parfait.persistence.entity.ParfaitGroupReporting
import parfait.persistence.repository.ParfaitGroupReportingRepository

@Component
class ParfaitGroupReportingAdapter(
    private val parfaitGroupReportingRepository: ParfaitGroupReportingRepository,
) : ParfaitGroupReportSavePort {
    override fun save(report: ParfaitGroupReport): ParfaitGroupReport =
        parfaitGroupReportingRepository.save(report.toEntity()).toDomain()

    private fun ParfaitGroupReport.toEntity(): ParfaitGroupReporting =
        ParfaitGroupReporting(
            parfaitGroupId = parfaitGroupId,
            reporterMemberId = reporterMemberId,
            reason = reason,
            createdAt = createdAt,
            id = id,
        )

    private fun ParfaitGroupReporting.toDomain(): ParfaitGroupReport =
        ParfaitGroupReport.reconstitute(
            id = requireNotNull(id),
            parfaitGroupId = parfaitGroupId,
            reporterMemberId = reporterMemberId,
            reason = reason,
            createdAt = createdAt,
        )
}
