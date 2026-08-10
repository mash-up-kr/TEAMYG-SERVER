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
import parfait.core.parfaitimage.domain.BorderType
import parfait.core.parfaitimage.exception.ParfaitImageErrorCode
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageBorderResult
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageBorderUseCase
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [UpdateParfaitImageBorderController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    UpdateParfaitImageBorderControllerTest.UseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class UpdateParfaitImageBorderControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var updateParfaitImageBorderUseCase: UpdateParfaitImageBorderUseCase

    private val authentication = UsernamePasswordAuthenticationToken("42", null, emptyList())

    @Test
    fun `수정에 성공하면 200과 갱신된 테두리 정보를 응답한다`() {
        every { updateParfaitImageBorderUseCase.update(any()) } returns
            UpdateParfaitImageBorderResult(
                parfaitImageId = 201L,
                borderType = BorderType.SOLID,
                borderColor = "#FF0000",
                borderWidth = 3.0,
            )

        mockMvc
            .patch("/api/v1/groups/1/parfaits/5/images/201/border") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"borderType":"SOLID","borderColor":"#FF0000","borderWidth":3}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.parfaitImageId") { value(201) }
                jsonPath("$.data.borderType") { value("SOLID") }
                jsonPath("$.data.borderColor") { value("#FF0000") }
            }

        verify {
            updateParfaitImageBorderUseCase.update(
                match {
                    it.memberId == 42L &&
                        it.groupId == 1L &&
                        it.parfaitId == 5L &&
                        it.parfaitImageId == 201L &&
                        it.borderType == BorderType.SOLID
                },
            )
        }
    }

    @Test
    fun `존재하지 않는 배치 ID면 404와 PARFAIT_IMAGE_NOT_FOUND로 응답한다`() {
        every { updateParfaitImageBorderUseCase.update(any()) } throws
            BusinessException(ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_FOUND)

        mockMvc
            .patch("/api/v1/groups/1/parfaits/5/images/999/border") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"borderType":"NONE"}"""
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("PARFAIT_IMAGE_NOT_FOUND") }
            }
    }

    @Test
    fun `본인이 배치한 토핑이 아니면 403과 PARFAIT_IMAGE_NOT_OWNED로 응답한다`() {
        every { updateParfaitImageBorderUseCase.update(any()) } throws
            BusinessException(ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_OWNED)

        mockMvc
            .patch("/api/v1/groups/1/parfaits/5/images/201/border") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"borderType":"NONE"}"""
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("PARFAIT_IMAGE_NOT_OWNED") }
            }
    }

    @Test
    fun `SOLID인데 색상·두께가 없으면 400과 INVALID_BORDER로 응답한다`() {
        every { updateParfaitImageBorderUseCase.update(any()) } throws
            BusinessException(ParfaitImageErrorCode.INVALID_BORDER)

        mockMvc
            .patch("/api/v1/groups/1/parfaits/5/images/201/border") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"borderType":"SOLID"}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_BORDER") }
            }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun updateParfaitImageBorderUseCase(): UpdateParfaitImageBorderUseCase = mockk()
    }
}
