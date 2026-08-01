package parfait.http.parfaitgroup.controller

import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.parfaitgroup.application.port.`in`.ChangeMyParfaitGroupNicknameUseCase
import parfait.core.parfaitgroup.application.port.`in`.CreateParfaitGroupUseCase
import parfait.core.parfaitgroup.application.port.`in`.GetMyParfaitGroupDetailUseCase
import parfait.core.parfaitgroup.application.port.`in`.GetMyParfaitGroupsUseCase
import parfait.core.parfaitgroup.application.port.`in`.JoinParfaitGroupUseCase
import parfait.core.parfaitgroup.application.port.`in`.LeaveParfaitGroupUseCase
import parfait.core.parfaitgroup.application.port.`in`.PreviewParfaitGroupJoinUseCase
import parfait.core.parfaitgroup.application.port.`in`.ReportParfaitGroupUseCase

@TestConfiguration
class TestParfaitGroupUseCaseConfig {
    @Bean
    fun previewParfaitGroupJoinUseCase(): PreviewParfaitGroupJoinUseCase = mockk()

    @Bean
    fun joinParfaitGroupUseCase(): JoinParfaitGroupUseCase = mockk()

    @Bean
    fun createParfaitGroupUseCase(): CreateParfaitGroupUseCase = mockk()

    @Bean
    fun getMyParfaitGroupsUseCase(): GetMyParfaitGroupsUseCase = mockk()

    @Bean
    fun getMyParfaitGroupDetailUseCase(): GetMyParfaitGroupDetailUseCase = mockk()

    @Bean
    fun changeMyParfaitGroupNicknameUseCase(): ChangeMyParfaitGroupNicknameUseCase = mockk()

    @Bean
    fun leaveParfaitGroupUseCase(): LeaveParfaitGroupUseCase = mockk()

    @Bean
    fun reportParfaitGroupUseCase(): ReportParfaitGroupUseCase = mockk()
}
