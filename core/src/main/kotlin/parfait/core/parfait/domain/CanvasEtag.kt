package parfait.core.parfait.domain

/**
 * today 응답의 ETag 조립·비교를 한 곳에 모은다. 형식이 바뀌어도 클라이언트는 불투명한 문자열로만 다루므로
 * 여기만 고치면 된다. 버전을 하나로 합치기로 하면 `g` 요소를 이 클래스에서 빼면 된다.
 */
object CanvasEtag {
    fun of(
        parfaitId: Long,
        parfaitVersion: Long,
        groupVersion: Long,
        memberId: Long,
    ): String = "W/\"p$parfaitId-v$parfaitVersion-g$groupVersion-m$memberId\""

    /** `If-None-Match` 헤더 값이 현재 ETag 와 일치하는지. 헤더가 없거나 깨졌으면 false(= 200). */
    fun matches(
        ifNoneMatch: String?,
        etag: String,
    ): Boolean {
        if (ifNoneMatch.isNullOrBlank()) return false
        val target = opaque(etag)
        return ifNoneMatch
            .split(',')
            .map { it.trim() }
            .any { it == "*" || opaque(it) == target }
    }

    private fun opaque(tag: String): String = tag.removePrefix("W/").trim().removeSurrounding("\"")
}
