package parfait.http.api.deeplink.controller

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

// 키 이름이 스펙상 snake_case로 고정되어 있어(Jackson 기본은 camelCase), data class 대신
// Map을 그대로 반환해 키 표기를 직접 제어한다.
@Hidden
@RestController
class DeepLinkWellKnownController(
    @Value("\${deeplink.android.package-name}") private val androidPackageName: String,
    @Value("\${deeplink.android.sha256-cert-fingerprints:}") private val sha256CertFingerprintsRaw: String,
    @Value("\${deeplink.ios.team-id:}") private val iosTeamId: String,
    @Value("\${deeplink.ios.bundle-id:}") private val iosBundleId: String,
) {
    @GetMapping("/.well-known/assetlinks.json", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun assetLinks(): ResponseEntity<Any> =
        ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                listOf(
                    linkedMapOf(
                        "relation" to listOf("delegate_permission/common.handle_all_urls"),
                        "target" to
                            linkedMapOf(
                                "namespace" to "android_app",
                                "package_name" to androidPackageName,
                                "sha256_cert_fingerprints" to sha256CertFingerprints(),
                            ),
                    ),
                ),
            )

    @GetMapping("/.well-known/apple-app-site-association", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun appleAppSiteAssociation(): ResponseEntity<Any> =
        ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                linkedMapOf(
                    "applinks" to
                        linkedMapOf(
                            "apps" to emptyList<String>(),
                            "details" to appleAppLinkDetails(),
                        ),
                ),
            )

    private fun sha256CertFingerprints(): List<String> =
        sha256CertFingerprintsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    private fun appleAppLinkDetails(): List<Map<String, Any>> {
        if (iosTeamId.isBlank() || iosBundleId.isBlank()) return emptyList()
        return listOf(
            linkedMapOf(
                "appID" to "$iosTeamId.$iosBundleId",
                "paths" to listOf("/link", "/link/*"),
            ),
        )
    }
}
