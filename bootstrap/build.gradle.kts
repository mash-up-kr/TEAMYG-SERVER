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
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-mysql")
    testImplementation("org.springframework.batch:spring-batch-test")
}
