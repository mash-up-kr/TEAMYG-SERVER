package parfait.http.parfaitimage.controller

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import parfait.core.exception.BusinessException
import parfait.core.parfaitimage.exception.ParfaitImageErrorCode
import parfait.core.parfaitimage.port.`in`.DeleteParfaitImageUseCase
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [DeleteParfaitImageController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    DeleteParfaitImageControllerTest.UseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class DeleteParfaitImageControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var deleteParfaitImageUseCase: DeleteParfaitImageUseCase

    private val authentication = UsernamePasswordAuthenticationToken("42", null, emptyList())

    @Test
    fun `삭제에 성공하면 200과 null data를 응답한다`() {
        every { deleteParfaitImageUseCase.delete(any()) } returns Unit

        mockMvc
            .delete("/api/v1/groups/1/parfaits/5/images/201") {
                principal = authentication
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data") { value(nullValue()) }
            }

        verify {
            deleteParfaitImageUseCase.delete(
                match { it.memberId == 42L && it.groupId == 1L && it.parfaitId == 5L && it.parfaitImageId == 201L },
            )
        }
    }

    @Test
    fun `존재하지 않는 배치 ID면 404와 PARFAIT_IMAGE_NOT_FOUND로 응답한다`() {
        every { deleteParfaitImageUseCase.delete(any()) } throws
            BusinessException(ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_FOUND)

        mockMvc
            .delete("/api/v1/groups/1/parfaits/5/images/999") {
                principal = authentication
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("PARFAIT_IMAGE_NOT_FOUND") }
            }
    }

    @Test
    fun `본인이 배치한 토핑이 아니면 403과 PARFAIT_IMAGE_NOT_OWNED로 응답한다`() {
        every { deleteParfaitImageUseCase.delete(any()) } throws
            BusinessException(ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_OWNED)

        mockMvc
            .delete("/api/v1/groups/1/parfaits/5/images/201") {
                principal = authentication
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("PARFAIT_IMAGE_NOT_OWNED") }
            }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun deleteParfaitImageUseCase(): DeleteParfaitImageUseCase = mockk()
    }
}
