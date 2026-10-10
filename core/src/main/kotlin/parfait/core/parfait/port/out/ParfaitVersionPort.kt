package parfait.core.parfait.port.out

/** 캔버스 응답이 바뀔 때마다 오르는 parfait.version 의 조회·증가. 증가는 호출자 트랜잭션 안에서 해야 한다. */
interface ParfaitVersionPort {
    fun getVersion(parfaitId: Long): Long

    fun bump(parfaitId: Long)
}
