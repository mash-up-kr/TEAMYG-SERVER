@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfait.port.`in`

interface GetTodayParfaitIfChangedUseCase {
    fun get(command: GetTodayParfaitIfChangedCommand): GetTodayParfaitIfChangedResult
}

data class GetTodayParfaitIfChangedCommand(
    val memberId: Long,
    val groupId: Long,
    val ifNoneMatch: String?,
)

sealed interface GetTodayParfaitIfChangedResult {
    val etag: String

    data class NotModified(
        override val etag: String,
    ) : GetTodayParfaitIfChangedResult

    data class Modified(
        override val etag: String,
        val body: GetTodayParfaitResult,
    ) : GetTodayParfaitIfChangedResult
}
