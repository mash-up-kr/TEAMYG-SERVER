package parfait.external.image

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest

class ImageDeleteAdapterTest {
    private val s3Client = mockk<S3Client>(relaxed = true)
    private val adapter =
        ImageDeleteAdapter(
            bucket = "parfait-bucket",
            region = "ap-northeast-2",
            credentialsProvider =
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test-key", "test-secret"),
                ),
            s3Client = s3Client,
        )

    @Test
    fun `imageUrl에서 key를 역산해 같은 버킷의 객체를 삭제한다`() {
        adapter.delete("https://parfait-bucket.s3.ap-northeast-2.amazonaws.com/nukki/user42/uuid.png")

        val requestSlot = slot<DeleteObjectRequest>()
        verify { s3Client.deleteObject(capture(requestSlot)) }
        requestSlot.captured.bucket() shouldBe "parfait-bucket"
        requestSlot.captured.key() shouldBe "nukki/user42/uuid.png"
    }
}
