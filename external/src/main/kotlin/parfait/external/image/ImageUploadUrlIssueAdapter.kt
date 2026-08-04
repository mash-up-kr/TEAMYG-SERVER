package parfait.external.image

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import parfait.core.image.port.out.ImageUploadUrlIssuePort
import parfait.core.image.port.out.PresignedUpload
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration

@Component
class ImageUploadUrlIssueAdapter(
    @Value("\${aws.s3.bucket}") private val bucket: String,
    @Value("\${aws.s3.region}") private val region: String,
    credentialsProvider: AwsCredentialsProvider = DefaultCredentialsProvider.create(),
) : ImageUploadUrlIssuePort {
    private val s3Presigner: S3Presigner =
        S3Presigner
            .builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider)
            .build()

    override fun issue(
        key: String,
        contentType: String,
        expirationSeconds: Long,
    ): PresignedUpload {
        val putObjectRequest =
            PutObjectRequest
                .builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build()
        val presignRequest =
            PutObjectPresignRequest
                .builder()
                .signatureDuration(Duration.ofSeconds(expirationSeconds))
                .putObjectRequest(putObjectRequest)
                .build()
        val uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString()
        val imageUrl = "https://$bucket.s3.$region.amazonaws.com/$key"
        return PresignedUpload(uploadUrl = uploadUrl, imageUrl = imageUrl)
    }
}
