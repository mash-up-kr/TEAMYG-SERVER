package parfait.external.image

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider

class ImageUploadUrlIssueAdapterTest {
    private val adapter =
        ImageUploadUrlIssueAdapter(
            bucket = "parfait-bucket",
            region = "ap-northeast-2",
            credentialsProvider =
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test-key", "test-secret"),
                ),
        )

    @Test
    fun `key와 만료시간이 반영된 presigned upload URL과 영구 imageUrl을 발급한다`() {
        val result = adapter.issue(key = "nukki/user42/uuid.png", contentType = "image/png", expirationSeconds = 900)

        result.uploadUrl shouldStartWith "https://parfait-bucket.s3.ap-northeast-2.amazonaws.com/nukki/user42/uuid.png"
        result.uploadUrl shouldContain "X-Amz-Expires=900"
        result.imageUrl shouldBe "https://parfait-bucket.s3.ap-northeast-2.amazonaws.com/nukki/user42/uuid.png"
    }
}
