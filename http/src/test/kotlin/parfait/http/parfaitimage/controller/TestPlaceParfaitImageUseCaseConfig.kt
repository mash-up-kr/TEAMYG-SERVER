package parfait.http.parfaitimage.controller

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImageCommand
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImageResult
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImageUseCase
import parfait.core.parfaitimage.port.`in`.PlacedByResult

/**
 * 컨텍스트 로딩만 필요한 테스트(actuator, openapi, security 화이트리스트 등)에서
 * `PlaceParfaitImageUseCase` 빈 부재로 인한 `NoSuchBeanDefinitionException`을 막기 위한 항상-존재 스텁이다.
 */
@TestConfiguration
class TestPlaceParfaitImageUseCaseConfig {
    @Bean
    fun placeParfaitImageUseCase(): PlaceParfaitImageUseCase =
        object : PlaceParfaitImageUseCase {
            override fun place(command: PlaceParfaitImageCommand): PlaceParfaitImageResult =
                PlaceParfaitImageResult(
                    parfaitImageId = 0L,
                    imageId = command.imageId,
                    imageUrl = "stub-image-url",
                    positionX = command.positionX,
                    positionY = command.positionY,
                    positionZ = command.positionZ,
                    scale = command.scale,
                    rotation = command.rotation,
                    placedBy = PlacedByResult(groupMemberId = 0L, nickname = "stub"),
                )
        }
}
