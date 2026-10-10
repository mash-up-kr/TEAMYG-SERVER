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
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // CanvasEtagIntegrationTest 가 StringRedisTemplate 으로 캔버스 캐시 키를 직접 확인한다(런타임에는 persistence 가 이미 가져온다).
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-mysql")
    testImplementation("org.springframework.batch:spring-batch-test")
}
