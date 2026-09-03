package parfait.core.notification.port.out

interface DeviceTokenDeletePort {
    fun delete(
        memberId: Long,
        sessionId: String,
    )

    fun deleteAllByMemberId(memberId: Long)

    /** 죽은 토큰 회수(E-10/E-12). token 은 uk_device_token_token 으로 유일. */
    fun deleteByToken(token: String)

    /** 여러 토큰을 한 번에 삭제하고 삭제된 행 수를 반환한다 (리마인드 배치의 죽은 토큰 일괄 회수). */
    fun deleteByTokenIn(tokens: Collection<String>): Int
}
