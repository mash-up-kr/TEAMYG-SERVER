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

    fun existsByParfaitGroupIdAndMemberId(
        parfaitGroupId: Long,
        memberId: Long,
    ): Boolean

    fun existsByParfaitGroupIdAndGroupNicknameAndLeftAtIsNull(
        parfaitGroupId: Long,
        groupNickname: String,
    ): Boolean

    fun countByParfaitGroupIdAndLeftAtIsNull(parfaitGroupId: Long): Long

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
                ) AS recentImageUploadedAt
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
}
