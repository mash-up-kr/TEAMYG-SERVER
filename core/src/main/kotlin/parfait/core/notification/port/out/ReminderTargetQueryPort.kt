package parfait.core.notification.port.out

interface ReminderTargetQueryPort {
    /**
     * left_at IS NULL 인 그룹 멤버십이 1개 이상이고 기기 토큰을 보유한 이용자의 전체 토큰 목록.
     * device_token 기준 조회라 한 이용자가 그룹 N개여도 토큰 중복이 생기지 않는다.
     * 권한 거부 이용자는 토큰이 없어 결과에 미포함 (정책 E-02).
     */
    fun findActiveGroupMemberDeviceTokens(): List<String>
}
