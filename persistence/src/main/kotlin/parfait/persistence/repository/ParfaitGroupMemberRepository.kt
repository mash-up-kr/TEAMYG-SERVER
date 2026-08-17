package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import parfait.persistence.entity.ParfaitGroupMember
import java.time.LocalDateTime

interface ParfaitGroupMemberRepository : JpaRepository<ParfaitGroupMember, Long> {
    fun findByParfaitGroupIdAndMemberIdAndLeftAtIsNull(
        parfaitGroupId: Long,
        memberId: Long,
    ): ParfaitGroupMember?

    fun findAllByParfaitGroupIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(parfaitGroupId: Long): List<ParfaitGroupMember>

    fun findAllByMemberIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(memberId: Long): List<ParfaitGroupMember>

    fun existsByParfaitGroupIdAndMemberId(
        parfaitGroupId: Long,
        memberId: Long,
    ): Boolean

    fun existsByParfaitGroupIdAndGroupNicknameAndLeftAtIsNull(
        parfaitGroupId: Long,
        groupNickname: String,
    ): Boolean

    fun countByParfaitGroupIdAndLeftAtIsNull(parfaitGroupId: Long): Long

    fun findAllByIdIn(ids: Collection<Long>): List<ParfaitGroupMember>

    @Query(
        value =
            """
            SELECT
                gm.parfait_group_id AS groupId,
                g.name AS groupName,
                (
                    SELECT pi.image_url
                    FROM parfait p
                    LEFT JOIN parfait_image pi ON pi.parfait_id = p.id
                    WHERE p.parfait_group_id = g.id
                    ORDER BY p.parfait_date DESC, pi.created_at DESC, pi.id DESC
                    LIMIT 1
                ) AS recentImageUrl,
                (
                    SELECT pi.created_at
                    FROM parfait p
                    LEFT JOIN parfait_image pi ON pi.parfait_id = p.id
                    WHERE p.parfait_group_id = g.id
                    ORDER BY p.parfait_date DESC, pi.created_at DESC, pi.id DESC
                    LIMIT 1
                ) AS recentImageUploadedAt,
                -- placer는 현재 시점의 parfait_group_member 상태를 조인한다(배치 당시가 아님).
                -- 탈퇴한 멤버는 삭제되지 않고 nametag_chip이 RELEASED로 남으므로, 이 서브쿼리는
                -- placer가 이미 탈퇴했어도 자연스럽게 'RELEASED' 문자열을 반환한다 — 의도된 동작이며,
                -- INNER JOIN이나 placer.left_at IS NULL 조건을 추가하면 안 된다.
                (
                    SELECT placer.nametag_chip
                    FROM parfait p
                    LEFT JOIN parfait_image pi ON pi.parfait_id = p.id
                    LEFT JOIN parfait_group_member placer ON placer.id = pi.placed_by_group_member_id
                    WHERE p.parfait_group_id = g.id
                    ORDER BY p.parfait_date DESC, pi.created_at DESC, pi.id DESC
                    LIMIT 1
                ) AS lastPlacedByNametagChip
            FROM parfait_group_member gm
            INNER JOIN parfait_group g ON g.id = gm.parfait_group_id
            WHERE gm.member_id = :memberId
              AND gm.left_at IS NULL
            ORDER BY COALESCE(
                (
                    SELECT pi.created_at
                    FROM parfait p
                    LEFT JOIN parfait_image pi ON pi.parfait_id = p.id
                    WHERE p.parfait_group_id = g.id
                    ORDER BY p.parfait_date DESC, pi.created_at DESC, pi.id DESC
                    LIMIT 1
                ),
                g.created_at
            ) DESC, g.id DESC
            """,
        nativeQuery = true,
    )
    fun findMyGroupSummaries(
        @Param("memberId") memberId: Long,
    ): List<MyParfaitGroupSummaryProjection>
}

interface MyParfaitGroupSummaryProjection {
    val groupId: Long
    val groupName: String
    val recentImageUrl: String?
    val recentImageUploadedAt: LocalDateTime?
    val lastPlacedByNametagChip: String?
}
