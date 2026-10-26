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
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImagesResult
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImagesUseCase
import parfait.core.parfaittext.exception.ParfaitTextErrorCode
import parfait.core.parfaittext.port.`in`.UpdateParfaitTextResult
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [UpdateParfaitImagesController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    UpdateParfaitImagesControllerTest.UseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class UpdateParfaitImagesControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var updateParfaitImagesUseCase: UpdateParfaitImagesUseCase

    private val authentication = UsernamePasswordAuthenticationToken("42", null, emptyList())

    @Test
    fun `여러 토핑을 한 번에 수정하면 200과 갱신된 목록을 응답한다`() {
        every { updateParfaitImagesUseCase.updateAll(any()) } returns
            UpdateParfaitImagesResult(
                images =
                    listOf(
                        UpdateParfaitImageResult(
                            parfaitImageId = 201L,
                            positionX = 200.0,
                            positionY = 400.0,
                            positionZ = 1,
                            scale = 1.5,
                            rotation = 45.0,
                        ),
                        UpdateParfaitImageResult(
                            parfaitImageId = 202L,
                            positionX = 10.0,
                            positionY = 20.0,
                            positionZ = 2,
                            scale = 1.0,
                            rotation = 0.0,
                        ),
                    ),
                texts = emptyList(),
            )

        mockMvc
            .patch("/api/v1/groups/1/parfaits/5/images") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                      "items": [
                        {"parfaitImageId":201,"positionX":200.0,"positionY":400.0,"scale":1.5,"rotation":45.0},
                        {"parfaitImageId":202,"positionX":10.0,"positionY":20.0}
                      ]
                    }
                    """.trimIndent()
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.images.length()") { value(2) }
                jsonPath("$.data.images[0].parfaitImageId") { value(201) }
                jsonPath("$.data.images[1].parfaitImageId") { value(202) }
            }

        verify {
            updateParfaitImagesUseCase.updateAll(
                match {
                    it.memberId == 42L &&
                        it.groupId == 1L &&
                        it.parfaitId == 5L &&
                        it.items.size == 2 &&
                        it.items[0].parfaitImageId == 201L &&
                        it.items[1].parfaitImageId == 202L
                },
            )
        }
    }

    @Test
    fun `존재하지 않는 배치 ID가 포함되면 404와 PARFAIT_IMAGE_NOT_FOUND로 응답한다`() {
        every { updateParfaitImagesUseCase.updateAll(any()) } throws
            BusinessException(ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_FOUND)

        mockMvc
            .patch("/api/v1/groups/1/parfaits/5/images") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"items":[{"parfaitImageId":999,"positionX":200.0}]}"""
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("PARFAIT_IMAGE_NOT_FOUND") }
            }
    }

    @Test
    fun `본인이 배치하지 않은 토핑이 포함되면 403과 PARFAIT_IMAGE_NOT_OWNED로 응답한다`() {
        every { updateParfaitImagesUseCase.updateAll(any()) } throws
            BusinessException(ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_OWNED)

        mockMvc
            .patch("/api/v1/groups/1/parfaits/5/images") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"items":[{"parfaitImageId":201,"positionX":200.0}]}"""
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("PARFAIT_IMAGE_NOT_OWNED") }
            }
    }

    @Test
    fun `이미지와 텍스트를 함께 수정하면 images 와 texts 를 모두 응답한다`() {
        every { updateParfaitImagesUseCase.updateAll(any()) } returns
            UpdateParfaitImagesResult(
                images = listOf(UpdateParfaitImageResult(201L, 200.0, 400.0, 1, 1.5, 45.0)),
                texts =
                    listOf(
                        UpdateParfaitTextResult(
                            parfaitTextId = 301L,
                            text = "오늘도 행복한 하루!",
                            positionX = 10.0,
                            positionY = 20.0,
                            positionZ = 2,
                            scale = 1.5,
                            rotation = -30.0,
                            color = "#FF5733",
                            fontSize = 16.0,
                            isFill = true,
                            isStroke = false,
                        ),
                    ),
            )

        mockMvc
            .patch("/api/v1/groups/1/parfaits/5/images") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                      "items": [{"parfaitImageId":201,"positionX":200.0}],
                      "textItems": [{"parfaitTextId":301,"positionX":10.0,"positionY":20.0,"positionZ":2,"scale":1.5,"rotation":-30.0}]
                    }
                    """.trimIndent()
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.images[0].parfaitImageId") { value(201) }
                jsonPath("$.data.texts[0].parfaitTextId") { value(301) }
                jsonPath("$.data.texts[0].text") { value("오늘도 행복한 하루!") }
                jsonPath("$.data.texts[0].isFill") { value(true) }
                jsonPath("$.data.texts[0].isStroke") { value(false) }
                jsonPath("$.data.texts[0].textAlign") { value("CENTER") }
            }

        verify {
            updateParfaitImagesUseCase.updateAll(
                match {
                    it.items.firstOrNull()?.parfaitImageId == 201L &&
                        it.textItems.firstOrNull()?.parfaitTextId == 301L &&
                        it.textItems.firstOrNull()?.rotation == -30.0
                },
            )
        }
    }

    @Test
    fun `textItems 만 보내도 요청이 받아들여진다`() {
        every { updateParfaitImagesUseCase.updateAll(any()) } returns
            UpdateParfaitImagesResult(images = emptyList(), texts = emptyList())

        mockMvc
            .patch("/api/v1/groups/1/parfaits/5/images") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"textItems":[{"parfaitTextId":301,"positionX":10.0}]}"""
            }.andExpect {
                status { isOk() }
            }

        verify { updateParfaitImagesUseCase.updateAll(match { it.items.isEmpty() && it.textItems.size == 1 }) }
    }

    @Test
    fun `본인이 배치하지 않은 텍스트가 포함되면 403과 PARFAIT_TEXT_NOT_OWNED로 응답한다`() {
        every { updateParfaitImagesUseCase.updateAll(any()) } throws
            BusinessException(ParfaitTextErrorCode.PARFAIT_TEXT_NOT_OWNED)

        mockMvc
            .patch("/api/v1/groups/1/parfaits/5/images") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"textItems":[{"parfaitTextId":301,"positionX":10.0}]}"""
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("PARFAIT_TEXT_NOT_OWNED") }
            }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun updateParfaitImagesUseCase(): UpdateParfaitImagesUseCase = mockk()
    }
}
