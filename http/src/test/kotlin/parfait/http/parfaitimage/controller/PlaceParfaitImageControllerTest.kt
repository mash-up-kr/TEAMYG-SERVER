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
import org.springframework.test.web.servlet.post
import parfait.core.exception.BusinessException
import parfait.core.image.exception.ImageErrorCode
import parfait.core.parfaitgroup.domain.NameTagChipType
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.core.parfaitimage.exception.ParfaitImageErrorCode
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImagePlacedByResult
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImageResult
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImageUseCase
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [PlaceParfaitImageController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    PlaceParfaitImageControllerTest.UseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class PlaceParfaitImageControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var placeParfaitImageUseCase: PlaceParfaitImageUseCase

    private val authentication = UsernamePasswordAuthenticationToken("42", null, emptyList())

    private fun requestBody(
        borderType: String = "NONE",
        borderColor: String? = null,
        borderWidth: Double? = null,
    ) = """
        {
          "imageId": 77,
          "positionX": 120.5,
          "positionY": 340.2,
          "positionZ": 1,
          "scale": 1.0,
          "rotation": 0.0,
          "borderType": "$borderType",
          "borderColor": ${borderColor?.let { "\"$it\"" }},
          "borderWidth": $borderWidth
        }
        """.trimIndent()

    @Test
    fun `배치에 성공하면 201과 parfaitImageId·placedBy를 응답한다`() {
        every { placeParfaitImageUseCase.place(any()) } returns
            PlaceParfaitImageResult(
                parfaitImageId = 201L,
                imageId = 77L,
                imageUrl = "https://parfait-bucket.s3.../nukki/user1/550e8400.png",
                positionX = 120.5,
                positionY = 340.2,
                positionZ = 1,
                scale = 1.0,
                rotation = 0.0,
                placedBy =
                    PlaceParfaitImagePlacedByResult(
                        groupMemberId = 10L,
                        nickname = "연경이",
                        nametagChip = NameTagChipType.TYPE6,
                    ),
            )

        mockMvc
            .post("/api/v1/groups/1/parfaits/5/images") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isCreated() }
                jsonPath("$.data.parfaitImageId") { value(201) }
                jsonPath("$.data.placedBy.groupMemberId") { value(10) }
                jsonPath("$.data.placedBy.nickname") { value("연경이") }
                jsonPath("$.data.placedBy.nameTagChip") { value("TYPE6") }
            }

        verify {
            placeParfaitImageUseCase.place(
                match { it.memberId == 42L && it.groupId == 1L && it.parfaitId == 5L && it.imageId == 77L },
            )
        }
    }

    @Test
    fun `그룹에 참여하지 않았으면 403과 GROUP_NOT_JOINED로 응답한다`() {
        every { placeParfaitImageUseCase.place(any()) } throws
            ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)

        mockMvc
            .post("/api/v1/groups/1/parfaits/5/images") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("GROUP_NOT_JOINED") }
            }
    }

    @Test
    fun `존재하지 않는 파르페면 404와 PARFAIT_NOT_FOUND로 응답한다`() {
        every { placeParfaitImageUseCase.place(any()) } throws
            BusinessException(ParfaitImageErrorCode.PARFAIT_NOT_FOUND)

        mockMvc
            .post("/api/v1/groups/1/parfaits/5/images") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("PARFAIT_NOT_FOUND") }
            }
    }

    @Test
    fun `존재하지 않는 이미지면 404와 IMAGE_NOT_FOUND로 응답한다`() {
        every { placeParfaitImageUseCase.place(any()) } throws
            BusinessException(ImageErrorCode.IMAGE_NOT_FOUND)

        mockMvc
            .post("/api/v1/groups/1/parfaits/5/images") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("IMAGE_NOT_FOUND") }
            }
    }

    @Test
    fun `아직 확인되지 않은 이미지면 409와 IMAGE_NOT_CONFIRMED로 응답한다`() {
        every { placeParfaitImageUseCase.place(any()) } throws
            BusinessException(ParfaitImageErrorCode.IMAGE_NOT_CONFIRMED)

        mockMvc
            .post("/api/v1/groups/1/parfaits/5/images") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = requestBody()
            }.andExpect {
                status { isConflict() }
                jsonPath("$.code") { value("IMAGE_NOT_CONFIRMED") }
            }
    }

    @Test
    fun `SOLID 테두리인데 색상·두께가 없으면 400과 INVALID_BORDER로 응답한다`() {
        every { placeParfaitImageUseCase.place(any()) } throws
            BusinessException(ParfaitImageErrorCode.INVALID_BORDER)

        mockMvc
            .post("/api/v1/groups/1/parfaits/5/images") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = requestBody(borderType = "SOLID")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_BORDER") }
            }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun placeParfaitImageUseCase(): PlaceParfaitImageUseCase = mockk()
    }
}
