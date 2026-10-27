package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import parfait.persistence.entity.ParfaitGroupMember
import java.time.LocalDate
import java.time.LocalDateTime

interface ParfaitGroupMemberRepository : JpaRepository<ParfaitGroupMember, Long> {
    fun findByParfaitGroupIdAndMemberIdAndLeftAtIsNull(
        parfaitGroupId: Long,
        memberId: Long,
    ): ParfaitGroupMember?

    fun findByParfaitGroupIdAndMemberId(
        parfaitGroupId: Long,
        memberId: Long,
    ): ParfaitGroupMember?

    fun findAllByParfaitGroupIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(parfaitGroupId: Long): List<ParfaitGroupMember>

    fun findAllByMemberIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(memberId: Long): List<ParfaitGroupMember>

    fun existsByParfaitGroupIdAndMemberIdAndLeftAtIsNull(
        parfaitGroupId: Long,
        memberId: Long,
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
                    JOIN parfait_image pi ON pi.parfait_id = p.id
                    WHERE p.parfait_group_id = g.id
                      AND p.parfait_date = :today
                    ORDER BY pi.created_at DESC, pi.id DESC
                    LIMIT 1
                ) AS recentImageUrl,
                (
                    SELECT pi.border_type
                    FROM parfait p
                    JOIN parfait_image pi ON pi.parfait_id = p.id
                    WHERE p.parfait_group_id = g.id
                      AND p.parfait_date = :today
                    ORDER BY pi.created_at DESC, pi.id DESC
                    LIMIT 1
                ) AS recentImageBorderType,
                (
                    SELECT pi.border_color
                    FROM parfait p
                    JOIN parfait_image pi ON pi.parfait_id = p.id
                    WHERE p.parfait_group_id = g.id
                      AND p.parfait_date = :today
                    ORDER BY pi.created_at DESC, pi.id DESC
                    LIMIT 1
                ) AS recentImageBorderColor,
                (
                    SELECT pi.border_width
                    FROM parfait p
                    JOIN parfait_image pi ON pi.parfait_id = p.id
                    WHERE p.parfait_group_id = g.id
                      AND p.parfait_date = :today
                    ORDER BY pi.created_at DESC, pi.id DESC
                    LIMIT 1
                ) AS recentImageBorderWidth,
                COALESCE(
                    (
                        SELECT pi.created_at
                        FROM parfait p
                        JOIN parfait_image pi ON pi.parfait_id = p.id
                        WHERE p.parfait_group_id = g.id
                        ORDER BY pi.created_at DESC, pi.id DESC
                        LIMIT 1
                    ),
                    g.created_at
                ) AS recentImageUploadedAt,
                COALESCE(
                    (
                        SELECT placer.nametag_chip
                        FROM parfait p
                        JOIN parfait_image pi ON pi.parfait_id = p.id
                        LEFT JOIN parfait_group_member placer ON placer.id = pi.placed_by_group_member_id
                        WHERE p.parfait_group_id = g.id
                        ORDER BY pi.created_at DESC, pi.id DESC
                        LIMIT 1
                    ),
                    (
                        SELECT creator.nametag_chip
                        FROM parfait_group_member creator
                        WHERE creator.parfait_group_id = g.id
                        ORDER BY creator.joined_at ASC, creator.id ASC
                        LIMIT 1
                    )
                ) AS lastPlacedByNametagChip
            FROM parfait_group_member gm
            INNER JOIN parfait_group g ON g.id = gm.parfait_group_id
            WHERE gm.member_id = :memberId
              AND gm.left_at IS NULL
            ORDER BY COALESCE(
                (
                    SELECT pi.created_at
                    FROM parfait p
                    JOIN parfait_image pi ON pi.parfait_id = p.id
                    WHERE p.parfait_group_id = g.id
                    ORDER BY pi.created_at DESC, pi.id DESC
                    LIMIT 1
                ),
                g.created_at
            ) DESC, g.id DESC
            """,
        nativeQuery = true,
    )
    fun findMyGroupSummaries(
        @Param("memberId") memberId: Long,
        @Param("today") today: LocalDate,
    ): List<MyParfaitGroupSummaryProjection>

    /**
     * [findMyGroupSummaries]와 같은 요약을 이미지·텍스트를 모두 토핑으로 보고 계산한다.
     * 오늘 가장 최근 토핑이 텍스트면 이미지 관련 컬럼은 null이고 recentToppingType만 TEXT로 채워진다.
     * 최근 토핑 시각·정렬·마지막 배치자 chip도 텍스트를 포함한 가장 최근 토핑 기준이다.
     */
    @Query(
        value =
            """
            WITH my_groups AS (
                SELECT gm.parfait_group_id AS group_id
                FROM parfait_group_member gm
                WHERE gm.member_id = :memberId
                  AND gm.left_at IS NULL
            ),
            toppings AS (
                SELECT p.parfait_group_id AS group_id, p.parfait_date, 'IMAGE' AS topping_type,
                       pi.image_url, pi.border_type, pi.border_color, pi.border_width,
                       pi.placed_by_group_member_id, pi.created_at, pi.id
                FROM parfait p
                JOIN parfait_image pi ON pi.parfait_id = p.id
                WHERE p.parfait_group_id IN (SELECT group_id FROM my_groups)
                UNION ALL
                SELECT p.parfait_group_id AS group_id, p.parfait_date, 'TEXT' AS topping_type,
                       NULL, NULL, NULL, NULL,
                       pt.placed_by_group_member_id, pt.created_at, pt.id
                FROM parfait p
                JOIN parfait_text pt ON pt.parfait_id = p.id
                WHERE p.parfait_group_id IN (SELECT group_id FROM my_groups)
            ),
            ranked AS (
                SELECT t.*,
                       ROW_NUMBER() OVER (
                           PARTITION BY t.group_id ORDER BY t.created_at DESC, t.id DESC
                       ) AS rn_any,
                       ROW_NUMBER() OVER (
                           PARTITION BY t.group_id, t.parfait_date = :today ORDER BY t.created_at DESC, t.id DESC
                       ) AS rn_by_today
                FROM toppings t
            )
            SELECT
                g.id AS groupId,
                g.name AS groupName,
                today_latest.topping_type AS recentToppingType,
                today_latest.image_url AS recentImageUrl,
                today_latest.border_type AS recentImageBorderType,
                today_latest.border_color AS recentImageBorderColor,
                today_latest.border_width AS recentImageBorderWidth,
                COALESCE(any_latest.created_at, g.created_at) AS recentImageUploadedAt,
                COALESCE(
                    placer.nametag_chip,
                    (
                        SELECT creator.nametag_chip
                        FROM parfait_group_member creator
                        WHERE creator.parfait_group_id = g.id
                        ORDER BY creator.joined_at ASC, creator.id ASC
                        LIMIT 1
                    )
                ) AS lastPlacedByNametagChip
            FROM my_groups mg
            INNER JOIN parfait_group g ON g.id = mg.group_id
            LEFT JOIN ranked today_latest
                ON today_latest.group_id = g.id AND today_latest.parfait_date = :today AND today_latest.rn_by_today = 1
            LEFT JOIN ranked any_latest
                ON any_latest.group_id = g.id AND any_latest.rn_any = 1
            LEFT JOIN parfait_group_member placer ON placer.id = any_latest.placed_by_group_member_id
            ORDER BY COALESCE(any_latest.created_at, g.created_at) DESC, g.id DESC
            """,
        nativeQuery = true,
    )
    fun findMyGroupToppingSummaries(
        @Param("memberId") memberId: Long,
        @Param("today") today: LocalDate,
    ): List<MyParfaitGroupToppingSummaryProjection>
}

interface MyParfaitGroupSummaryProjection {
    val groupId: Long
    val groupName: String
    val recentImageUrl: String?
    val recentImageBorderType: String?
    val recentImageBorderColor: String?
    val recentImageBorderWidth: Double?
    val recentImageUploadedAt: LocalDateTime
    val lastPlacedByNametagChip: String
}

interface MyParfaitGroupToppingSummaryProjection : MyParfaitGroupSummaryProjection {
    val recentToppingType: String?
}
