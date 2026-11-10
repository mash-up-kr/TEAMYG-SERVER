@file:Suppress("ktlint:standard:package-name")

package parfait.core.notice.port.`in`

import parfait.core.notice.domain.Notice
import parfait.core.notice.domain.NoticePlatform

interface GetNoticesUseCase {
    /** 요청 플랫폼(IOS/ANDROID)과 COMMON 공지 중 게시일이 오늘 이전인 것을 최신순으로 반환한다. */
    fun getNotices(platform: NoticePlatform): List<Notice>
}
