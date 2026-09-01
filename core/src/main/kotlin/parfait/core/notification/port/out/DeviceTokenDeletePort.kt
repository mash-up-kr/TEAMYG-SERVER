package parfait.core.notification.port.out

interface DeviceTokenDeletePort {
    fun delete(
        memberId: Long,
        sessionId: String,
    )
}
