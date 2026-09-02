package parfait.core.notification.port.out

interface DeviceTokenDeletePort {
    fun delete(
        memberId: Long,
        sessionId: String,
    )

    fun deleteAllByMemberId(memberId: Long)

    /** 죽은 토큰 회수(E-10/E-12). token 은 uk_device_token_token 으로 유일. */
    fun deleteByToken(token: String)
}
