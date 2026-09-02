import java.security.KeyPairGenerator
import java.util.Base64

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

dependencies {
    implementation(project(":core"))
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.nimbusds:nimbus-jose-jwt:10.7")
    implementation("software.amazon.awssdk:s3:2.46.18")
    implementation("com.google.firebase:firebase-admin:9.9.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

// 로컬·테스트에서 FcmConfig 가 읽는 가짜 서비스 계정 키를 빌드 시 생성한다.
// 실제 자격증명이 아니라(존재하지 않는 프로젝트) 형식만 유효하며, 커밋하지 않는다
// — 유출 방지 + GitHub secret scanning 우회. 운영은 컨테이너에 마운트된 실제 키를 쓴다.
val generateLocalFcmKey by tasks.registering {
    val keyFile = layout.projectDirectory.file("src/main/resources/fcm/local-firebase-key.json").asFile
    outputs.file(keyFile)
    doLast {
        keyFile.parentFile.mkdirs()
        val keyPair =
            KeyPairGenerator
                .getInstance("RSA")
                .apply { initialize(2048) }
                .generateKeyPair()
        val base64 =
            Base64
                .getMimeEncoder(64, "\n".toByteArray())
                .encodeToString(keyPair.private.encoded)
        val pem = "-----BEGIN PRIVATE KEY-----\n$base64\n-----END PRIVATE KEY-----\n"
        val privateKeyLiteral = "\"" + pem.replace("\n", "\\n") + "\""
        keyFile.writeText(
            """
            {
              "type": "service_account",
              "project_id": "parfait-local-fake",
              "private_key_id": "0000000000000000000000000000000000000000",
              "private_key": $privateKeyLiteral,
              "client_email": "parfait-local-fake@parfait-local-fake.iam.gserviceaccount.com",
              "client_id": "000000000000000000000",
              "auth_uri": "https://accounts.google.com/o/oauth2/auth",
              "token_uri": "https://oauth2.googleapis.com/token",
              "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
              "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/parfait-local-fake%40parfait-local-fake.iam.gserviceaccount.com"
            }

            """.trimIndent(),
        )
    }
}

tasks.named("processResources") { dependsOn(generateLocalFcmKey) }
tasks.named("processTestResources") { dependsOn(generateLocalFcmKey) }
