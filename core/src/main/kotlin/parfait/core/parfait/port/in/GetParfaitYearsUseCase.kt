@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfait.port.`in`

interface GetParfaitYearsUseCase {
    fun getYears(
        memberId: Long,
        groupId: Long,
    ): List<Int>
}
