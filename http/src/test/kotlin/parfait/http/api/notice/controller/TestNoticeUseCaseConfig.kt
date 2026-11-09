package parfait.http.api.notice.controller

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.exception.BusinessException
import parfait.core.notice.domain.Notice
import parfait.core.notice.domain.NoticePlatform
import parfait.core.notice.exception.NoticeErrorCode
import parfait.core.notice.port.`in`.GetNoticesUseCase
import parfait.core.notice.port.`in`.RegisterNoticeCommand
import parfait.core.notice.port.`in`.RegisterNoticeUseCase
import parfait.core.notice.port.`in`.UpdateNoticeCommand
import parfait.core.notice.port.`in`.UpdateNoticeUseCase

/**
 * 공지사항 UseCase 의 실제 구현체(`NoticeService`)는 `core` 에 있고 `http` 의 `TestApplication` 은
 * `parfait.http` 만 스캔하므로, 컨텍스트 로딩만 필요한 테스트를 위한 항상-존재 스텁이다.
 */
@TestConfiguration
class TestNoticeUseCaseConfig {
    @Bean
    fun getNoticesUseCase(): GetNoticesUseCase =
        object : GetNoticesUseCase {
            override fun getNotices(platform: NoticePlatform): List<Notice> = emptyList()
        }

    @Bean
    fun registerNoticeUseCase(): RegisterNoticeUseCase =
        object : RegisterNoticeUseCase {
            override fun register(command: RegisterNoticeCommand): Notice =
                throw BusinessException(NoticeErrorCode.INVALID_NOTICE)
        }

    @Bean
    fun updateNoticeUseCase(): UpdateNoticeUseCase =
        object : UpdateNoticeUseCase {
            override fun update(
                id: Long,
                command: UpdateNoticeCommand,
            ): Notice = throw BusinessException(NoticeErrorCode.NOTICE_NOT_FOUND)
        }
}
