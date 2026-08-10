package parfait.http.parfaitimage.controller

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.patch
import parfait.core.exception.BusinessException
import parfait.core.parfaitimage.exception.ParfaitImageErrorCode
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageResult
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageUseCase
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [UpdateParfaitImageController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    UpdateParfaitImageControllerTest.UseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class UpdateParfaitImageControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var updateParfaitImageUseCase: UpdateParfaitImageUseCase

    private val authentication = UsernamePasswordAuthenticationToken("42", null, emptyList())

    @Test
    fun `수정에 성공하면 200과 갱신된 필드를 응답한다`() {
        every { updateParfaitImageUseCase.update(any()) } returns
            UpdateParfaitImageResult(
                parfaitImageId = 201L,
                positionX = 200.0,
                positionY = 400.0,
                positionZ = 1,
                scale = 1.5,
                rotation = 45.0,
            )

        mockMvc
            .patch("/api/v1/groups/1/parfaits/5/images/201") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"positionX":200.0,"positionY":400.0,"scale":1.5,"rotation":45.0}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.parfaitImageId") { value(201) }
                jsonPath("$.data.positionX") { value(200.0) }
                jsonPath("$.data.rotation") { value(45.0) }
            }

        verify {
            updateParfaitImageUseCase.update(
                match {
                    it.memberId == 42L &&
                        it.groupId == 1L &&
                        it.parfaitId == 5L &&
                        it.parfaitImageId == 201L &&
                        it.positionX == 200.0 &&
                        it.positionZ == null
                },
            )
        }
    }

    @Test
    fun `존재하지 않는 배치 ID면 404와 PARFAIT_IMAGE_NOT_FOUND로 응답한다`() {
        every { updateParfaitImageUseCase.update(any()) } throws
            BusinessException(ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_FOUND)

        mockMvc
            .patch("/api/v1/groups/1/parfaits/5/images/999") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"positionX":200.0}"""
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("PARFAIT_IMAGE_NOT_FOUND") }
            }
    }

    @Test
    fun `본인이 배치한 토핑이 아니면 403과 PARFAIT_IMAGE_NOT_OWNED로 응답한다`() {
        every { updateParfaitImageUseCase.update(any()) } throws
            BusinessException(ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_OWNED)

        mockMvc
            .patch("/api/v1/groups/1/parfaits/5/images/201") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"positionX":200.0}"""
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("PARFAIT_IMAGE_NOT_OWNED") }
            }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun updateParfaitImageUseCase(): UpdateParfaitImageUseCase = mockk()
    }
}
