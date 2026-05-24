plugins {
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.spring") version "2.2.21" apply false
    kotlin("plugin.jpa") version "2.2.21" apply false
    id("org.springframework.boot") version "4.0.6" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "teamyg"
    version = "0.0.1-SNAPSHOT"
}

subprojects {
    repositories {
        mavenCentral()
    }

    afterEvaluate {
        extensions.findByType<JavaPluginExtension>()?.apply {
            toolchain {
                languageVersion = JavaLanguageVersion.of(21)
            }
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        dependencies {
            "implementation"(kotlin("reflect"))
        }
        configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            compilerOptions {
                freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
            }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}