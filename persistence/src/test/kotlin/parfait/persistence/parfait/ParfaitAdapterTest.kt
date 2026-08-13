package parfait.persistence.parfait

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import parfait.core.parfait.domain.Parfait
import parfait.core.parfait.port.out.ParfaitSummary
import parfait.persistence.repository.ParfaitRepository
import java.time.LocalDate
import java.time.LocalDateTime
import parfait.persistence.entity.Parfait as ParfaitEntity

class ParfaitAdapterTest {
    private val parfaitRepository = mockk<ParfaitRepository>()
    private val adapter = ParfaitAdapter(parfaitRepository)
    private val now = LocalDateTime.of(2026, 8, 1, 12, 0)

    @Test
    fun `그룹 id로 토핑이 존재하는 연도 목록 조회를 리포지토리에 위임한다`() {
        every { parfaitRepository.findDistinctYearsByParfaitGroupId(1L) } returns listOf(2026, 2027)

        val result = adapter.findDistinctYearsByGroupId(1L)

        result shouldBe listOf(2026, 2027)
    }

    @Test
    fun `날짜 범위로 조회한 JPA 엔티티 목록을 ParfaitSummary로 변환한다`() {
        val from = LocalDate.of(2026, 7, 1)
        val to = LocalDate.of(2026, 7, 31)
        every {
            parfaitRepository.findAllByParfaitGroupIdAndParfaitDateBetweenOrderByParfaitDateDesc(
                1L,
                from,
                to,
            )
        } returns
            listOf(
                ParfaitEntity(
                    parfaitGroupId = 1L,
                    parfaitDate = LocalDate.of(2026, 7, 7),
                    status = "ACTIVE",
                    backgroundType = null,
                    backgroundValue = null,
                    createdAt = now,
                    updatedAt = now,
                    id = 98L,
                ),
            )

        val result = adapter.findAllByGroupIdAndDateRange(1L, from, to)

        result shouldBe listOf(ParfaitSummary(id = 98L, date = LocalDate.of(2026, 7, 7)))
    }

    @Test
    fun `그룹과 날짜로 조회한 JPA 엔티티를 도메인으로 변환한다`() {
        every { parfaitRepository.findByParfaitGroupIdAndParfaitDate(1L, LocalDate.of(2026, 7, 9)) } returns
            parfaitEntity()

        val result = adapter.findByGroupIdAndDate(1L, LocalDate.of(2026, 7, 9))!!

        result.id shouldBe 100L
        result.parfaitGroupId shouldBe 1L
    }

    @Test
    fun `해당 날짜의 파르페가 없으면 null을 반환한다`() {
        every { parfaitRepository.findByParfaitGroupIdAndParfaitDate(1L, LocalDate.of(2026, 7, 9)) } returns null

        adapter.findByGroupIdAndDate(1L, LocalDate.of(2026, 7, 9)) shouldBe null
    }

    @Test
    fun `CLOSED 상태인 파르페 중 가장 최근 날짜를 조회한다`() {
        every {
            parfaitRepository.findTopByParfaitGroupIdAndStatusOrderByParfaitDateDesc(1L, "CLOSED")
        } returns parfaitEntity(parfaitDate = LocalDate.of(2026, 7, 8), status = "CLOSED")

        val result = adapter.findLastClosedDateByGroupId(1L)

        result shouldBe LocalDate.of(2026, 7, 8)
    }

    @Test
    fun `CLOSED된 파르페가 없으면 null을 반환한다`() {
        every {
            parfaitRepository.findTopByParfaitGroupIdAndStatusOrderByParfaitDateDesc(1L, "CLOSED")
        } returns null

        adapter.findLastClosedDateByGroupId(1L) shouldBe null
    }

    @Test
    fun `도메인 파르페를 JPA 엔티티로 저장하고 다시 도메인으로 반환한다`() {
        val parfait = Parfait.createToday(parfaitGroupId = 1L, date = LocalDate.of(2026, 7, 9), now = now)
        every { parfaitRepository.save(any()) } answers {
            firstArg<ParfaitEntity>().apply { id = 100L }
        }

        val saved = adapter.save(parfait)

        saved.id shouldBe 100L
        saved.parfaitGroupId shouldBe 1L
    }

    private fun parfaitEntity(
        parfaitDate: LocalDate = LocalDate.of(2026, 7, 9),
        status: String = "ACTIVE",
    ): ParfaitEntity =
        ParfaitEntity(
            parfaitGroupId = 1L,
            parfaitDate = parfaitDate,
            status = status,
            backgroundType = null,
            backgroundValue = null,
            createdAt = now,
            updatedAt = now,
            id = 100L,
        )
}
