@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfait.port.`in`

interface RotateParfaitCanvasesUseCase {
    fun rotateAll(): RotateParfaitCanvasesResult
}

// 테스트 트리거 엔드포인트 전용. rotateAll()과 달리 ParfaitDay 가드를 건너뛰고 그룹별
// ACTIVE 캔버스를 즉시 마감·다음 날짜 캔버스를 생성한다.
interface ForceRotateParfaitCanvasesUseCase {
    fun forceRotateAll(): RotateParfaitCanvasesResult
}

data class RotateParfaitCanvasesResult(
    val closedCount: Int,
    val emptyCount: Int,
    val createdCount: Int,
    val failedCount: Int,
)
