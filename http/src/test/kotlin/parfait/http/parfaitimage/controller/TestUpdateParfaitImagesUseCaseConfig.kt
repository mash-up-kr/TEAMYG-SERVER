package parfait.http.parfaitimage.controller

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageResult
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImagesCommand
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImagesUseCase

/**
 * 컨텍스트 로딩만 필요한 테스트(actuator, openapi, security 화이트리스트 등)에서
 * `UpdateParfaitImagesUseCase` 빈 부재로 인한 `NoSuchBeanDefinitionException`을 막기 위한 항상-존재 스텁이다.
 */
@TestConfiguration
class TestUpdateParfaitImagesUseCaseConfig {
    @Bean
    fun updateParfaitImagesUseCase(): UpdateParfaitImagesUseCase =
        object : UpdateParfaitImagesUseCase {
            override fun updateAll(command: UpdateParfaitImagesCommand): List<UpdateParfaitImageResult> =
                command.items.map { item ->
                    UpdateParfaitImageResult(
                        parfaitImageId = item.parfaitImageId,
                        positionX = item.positionX ?: 0.0,
                        positionY = item.positionY ?: 0.0,
                        positionZ = item.positionZ ?: 0,
                        scale = item.scale ?: 1.0,
                        rotation = item.rotation ?: 0.0,
                    )
                }
        }
}
