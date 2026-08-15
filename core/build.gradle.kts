plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.retry:spring-retry:2.0.12")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
