package parfait.core.parfaitgroup.domain

enum class NameTagChipType {
    TYPE1,
    TYPE2,
    TYPE3,
    TYPE4,
    TYPE5,
    TYPE6,
    TYPE7,
    TYPE8,
    TYPE9,
    TYPE10,
    TYPE11,
    TYPE12,

    // 그룹을 탈퇴한 멤버의 칩을 나타내는 값 — assignRandom()의 배정 후보에서 항상 제외되며,
    // 같은 그룹 안에서 여러 명이 동시에 가질 수 있다(TYPE1~12와 달리 유일성 제약이 없음).
    RELEASED,
    ;

    companion object {
        private val ASSIGNABLE = entries - RELEASED

        // GroupMemberLimit.MAX(12)와 ASSIGNABLE 개수가 같아야 한다 — 그룹 정원보다 배정 가능한
        // 타입 수가 적으면 마지막 멤버가 들어올 때 배정 가능한 타입이 없어서 실패한다.
        fun assignRandom(occupied: Set<NameTagChipType>): NameTagChipType {
            val available = ASSIGNABLE - occupied
            check(available.isNotEmpty()) { "배정 가능한 Nametag-Chip 타입이 없습니다" }
            return available.random()
        }
    }
}
