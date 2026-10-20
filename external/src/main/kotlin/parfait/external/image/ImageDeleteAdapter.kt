package parfait.external.image

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import parfait.core.image.port.out.ImageDeletePort
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest

@Component
class ImageDeleteAdapter(
    @Value("\${aws.s3.bucket}") private val bucket: String,
    @Value("\${aws.s3.region}") private val region: String,
    @Value("\${aws.s3.public-base-url}") publicBaseUrl: String,
    credentialsProvider: AwsCredentialsProvider = DefaultCredentialsProvider.create(),
    private val s3Client: S3Client =
        S3Client
            .builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider)
            .build(),
) : ImageDeletePort {
    private val log = LoggerFactory.getLogger(javaClass)

    // 공개 URL을 CloudFront로 옮기기 전에 저장된 S3 원본 주소도 key로 되돌릴 수 있어야 한다.
    private val urlPrefixes =
        listOf(
            "${publicBaseUrl.trimEnd('/')}/",
            "https://$bucket.s3.$region.amazonaws.com/",
        ).distinct()

    override fun delete(imageUrl: String) {
        val prefix = urlPrefixes.firstOrNull { imageUrl.startsWith(it) }
        // DeleteObject는 없는 key에도 성공하므로, 엉뚱한 key로 호출하면 실패를 알 수 없다.
        if (prefix == null) {
            log.warn("알 수 없는 이미지 URL이라 S3 삭제를 건너뜁니다. imageUrl={}", imageUrl)
            return
        }
        s3Client.deleteObject(
            DeleteObjectRequest
                .builder()
                .bucket(bucket)
                .key(imageUrl.removePrefix(prefix))
                .build(),
        )
    }
}
