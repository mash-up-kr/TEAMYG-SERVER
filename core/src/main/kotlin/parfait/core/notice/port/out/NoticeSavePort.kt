package parfait.core.notice.port.out

import parfait.core.notice.domain.Notice

interface NoticeSavePort {
    fun save(notice: Notice): Notice
}
