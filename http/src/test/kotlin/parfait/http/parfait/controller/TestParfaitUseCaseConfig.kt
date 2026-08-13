package parfait.http.parfait.controller

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.parfait.port.`in`.GetParfaitYearsUseCase
import parfait.core.parfait.port.`in`.GetPastParfaitsCommand
import parfait.core.parfait.port.`in`.GetPastParfaitsUseCase
import parfait.core.parfait.port.`in`.GetTodayParfaitCommand
import parfait.core.parfait.port.`in`.GetTodayParfaitResult
import parfait.core.parfait.port.`in`.GetTodayParfaitUseCase
import parfait.core.parfait.port.`in`.PastParfaitResult

/**
 * `GetParfaitYearsUseCase`/`GetPastParfaitsUseCase`/`GetTodayParfaitUseCase`의 실제 구현체는
 * `core` 모듈에 있고, `http`의 `TestApplication`은 `parfait.http` 패키지만 스캔하므로
 * 컨텍스트에 존재하지 않는다.
 *
 * 컨텍스트 로딩만 필요한 테스트(actuator, openapi, security 화이트리스트 등)에서 빈 부재로 인한
 * `NoSuchBeanDefinitionException`을 막기 위한 항상-존재 스텁이다.
 * 실제 조회 로직 검증 목적이 아니므로 고정된 빈 값을 반환한다.
 */
@TestConfiguration
class TestParfaitUseCaseConfig {
    @Bean
    fun getParfaitYearsUseCase(): GetParfaitYearsUseCase =
        object : GetParfaitYearsUseCase {
            override fun getYears(
                memberId: Long,
                groupId: Long,
            ): List<Int> = emptyList()
        }

    @Bean
    fun getPastParfaitsUseCase(): GetPastParfaitsUseCase =
        object : GetPastParfaitsUseCase {
            override fun getPastParfaits(command: GetPastParfaitsCommand): List<PastParfaitResult> = emptyList()
        }

    @Bean
    fun getTodayParfaitUseCase(): GetTodayParfaitUseCase =
        object : GetTodayParfaitUseCase {
            override fun get(command: GetTodayParfaitCommand): GetTodayParfaitResult =
                throw UnsupportedOperationException("stub")
        }
}
