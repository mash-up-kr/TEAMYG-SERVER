plugins {
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.allopen") version "2.2.21" apply false
    kotlin("plugin.spring") version "2.2.21" apply false
    kotlin("plugin.jpa") version "2.2.21" apply false
    id("org.springframework.boot") version "4.0.6" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2" apply false
}

allprojects {
    group = "teamyg"
    version = "0.0.1-SNAPSHOT"
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.5.0")
    }

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
            "testImplementation"("io.mockk:mockk:1.14.11")
            "testImplementation"("io.kotest:kotest-assertions-core:6.1.11")
            "testImplementation"("org.jetbrains.kotlin:kotlin-test-junit5")
            "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        }
        configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            compilerOptions {
                freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
            }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}
