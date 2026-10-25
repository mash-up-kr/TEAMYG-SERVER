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
            publicBaseUrl = "https://cdn.example.com",
            credentialsProvider =
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test-key", "test-secret"),
                ),
            s3Client = s3Client,
        )

    @Test
    fun `공개 베이스 URL에서 key를 역산해 같은 버킷의 객체를 삭제한다`() {
        adapter.delete("https://cdn.example.com/nukki/user42/uuid.png")

        val request = deletedRequest()
        request.bucket() shouldBe "parfait-bucket"
        request.key() shouldBe "nukki/user42/uuid.png"
    }

    @Test
    fun `베이스 URL 전환 전에 저장된 S3 원본 주소에서도 key를 역산해 삭제한다`() {
        adapter.delete("https://parfait-bucket.s3.ap-northeast-2.amazonaws.com/nukki/user42/uuid.png")

        val request = deletedRequest()
        request.bucket() shouldBe "parfait-bucket"
        request.key() shouldBe "nukki/user42/uuid.png"
    }

    @Test
    fun `알 수 없는 도메인의 URL이면 S3 삭제를 호출하지 않는다`() {
        adapter.delete("https://other.example.com/nukki/user42/uuid.png")

        verify(exactly = 0) { s3Client.deleteObject(any<DeleteObjectRequest>()) }
    }

    private fun deletedRequest(): DeleteObjectRequest {
        val requestSlot = slot<DeleteObjectRequest>()
        verify { s3Client.deleteObject(capture(requestSlot)) }
        return requestSlot.captured
    }
}
