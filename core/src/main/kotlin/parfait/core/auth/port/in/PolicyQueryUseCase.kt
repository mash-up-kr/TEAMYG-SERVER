@file:Suppress("ktlint:standard:package-name")

package parfait.core.auth.port.`in`

import parfait.core.auth.port.out.CurrentTerms

interface PolicyQueryUseCase {
    fun getPolicies(): List<CurrentTerms>
}
