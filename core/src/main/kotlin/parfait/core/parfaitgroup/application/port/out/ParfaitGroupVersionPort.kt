package parfait.core.parfaitgroup.application.port.out

/** 멤버·닉네임이 바뀔 때마다 오르는 parfait_group.version 의 조회·증가. 증가는 호출자 트랜잭션 안에서 해야 한다. */
interface ParfaitGroupVersionPort {
    fun getVersion(groupId: Long): Long

    fun bump(groupId: Long)
}
