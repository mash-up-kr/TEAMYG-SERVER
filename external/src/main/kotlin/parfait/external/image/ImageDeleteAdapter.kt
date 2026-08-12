package parfait.external.image

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
    credentialsProvider: AwsCredentialsProvider = DefaultCredentialsProvider.create(),
    private val s3Client: S3Client =
        S3Client
            .builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider)
            .build(),
) : ImageDeletePort {
    override fun delete(imageUrl: String) {
        val key = imageUrl.removePrefix("https://$bucket.s3.$region.amazonaws.com/")
        s3Client.deleteObject(
            DeleteObjectRequest
                .builder()
                .bucket(bucket)
                .key(key)
                .build(),
        )
    }
}
