package parfait.core.parfait.service

import org.springframework.stereotype.Service
import parfait.core.parfait.domain.CanvasEtag
import parfait.core.parfait.domain.ParfaitDay
import parfait.core.parfait.port.`in`.GetTodayParfaitCommand
import parfait.core.parfait.port.`in`.GetTodayParfaitIfChangedCommand
import parfait.core.parfait.port.`in`.GetTodayParfaitIfChangedResult
import parfait.core.parfait.port.`in`.GetTodayParfaitIfChangedUseCase
import parfait.core.parfait.port.`in`.GetTodayParfaitUseCase

/**
 * 권한 → 오늘 캔버스(id·version) → 그룹 version → ETag 비교. 일치하면 304, 아니면 기존 v1 유스케이스로 본문을 만든다.
 * version 은 반드시 본문을 만들기 전에 읽는다. 본문 뒤에 읽으면 "옛 본문 + 새 ETag" 가 만들어져 잘못된 304 가 나갈 수 있다.
 */
@Service
class GetTodayParfaitIfChangedService(
    private val canvasStateReader: CanvasStateReader,
    private val getTodayParfaitUseCase: GetTodayParfaitUseCase,
) : GetTodayParfaitIfChangedUseCase {
    override fun get(command: GetTodayParfaitIfChangedCommand): GetTodayParfaitIfChangedResult {
        canvasStateReader.requireMember(command.groupId, command.memberId)

        val canvas = canvasStateReader.todayCanvas(command.groupId, ParfaitDay.current())
        val groupVersion = canvasStateReader.groupVersion(command.groupId)
        val etag = CanvasEtag.of(canvas.parfaitId, canvas.version, groupVersion, command.memberId)

        if (CanvasEtag.matches(command.ifNoneMatch, etag)) {
            return GetTodayParfaitIfChangedResult.NotModified(etag)
        }

        val body =
            getTodayParfaitUseCase.get(
                GetTodayParfaitCommand(memberId = command.memberId, groupId = command.groupId),
            )
        return GetTodayParfaitIfChangedResult.Modified(etag, body)
    }
}
