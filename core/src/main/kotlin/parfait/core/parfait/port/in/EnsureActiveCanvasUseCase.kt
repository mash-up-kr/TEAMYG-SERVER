@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfait.port.`in`

import parfait.core.parfait.domain.Parfait
import java.time.LocalDate

interface EnsureActiveCanvasUseCase {
    fun ensure(
        groupId: Long,
        targetDate: LocalDate,
    ): Parfait
}
