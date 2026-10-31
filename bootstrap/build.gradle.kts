plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":http"))
    implementation(project(":persistence"))
    implementation(project(":external"))
    implementation(project(":batch"))
    implementation("org.springframework.boot:spring-boot-starter")
    testImplementation(project(":core"))
    testImplementation("net.dv8tion:JDA:5.6.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // CanvasEtagIntegrationTest 가 StringRedisTemplate 으로 캔버스 캐시 키를 직접 확인한다(런타임에는 persistence 가 이미 가져온다).
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-mysql")
    testImplementation("org.springframework.batch:spring-batch-test")
}

// 셸·IDE 에 남아 있는 디스코드/리포트 설정이 컨텍스트 테스트에 새어 들어가 실제 디스코드에 접속하지 않도록 고정한다.
tasks.withType<Test> {
    listOf(
        "DISCORD_BOT_TOKEN",
        "DISCORD_GUILD_ID",
        "DISCORD_REPORT_CHANNEL_ID",
        "DISCORD_ADMIN_ROLE_ID",
        "STATS_API_TOKEN",
    ).forEach { environment(it, "") }
    // 빈 문자열이면 report.hour 가 비어 기동에 실패하므로 값을 비우지 않고 제거해 yaml 기본값을 쓰게 한다.
    listOf("REPORT_ENABLED", "REPORT_HOUR", "REPORT_EXCLUDED_MEMBER_IDS").forEach { environment.remove(it) }
}
