package com.back.sportteam.infra.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    private static final String BUCKET = "test-bucket";
    private static final String REGION = "ap-northeast-2";
    private static final int EXPIRATION = 10;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedPutObjectRequest presignedPutObjectRequest;

    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        S3Properties properties = new S3Properties(BUCKET, REGION, EXPIRATION);
        s3Service = new S3Service(s3Client, s3Presigner, properties);
    }

    @Test
    @SuppressWarnings("unchecked")
    void presignedUrl_발급_시_uploadUrl과_fileUrl을_반환한다() throws MalformedURLException {
        when(s3Presigner.presignPutObject(any(Consumer.class)))
                .thenReturn(presignedPutObjectRequest);
        when(presignedPutObjectRequest.url())
                .thenReturn(new URL("https://test-bucket.s3.ap-northeast-2.amazonaws.com/facilities/uuid?X-Amz=sig"));

        PresignedUrlResponse response = s3Service.generatePresignedUrl("image/jpeg");

        assertThat(response.uploadUrl()).contains("X-Amz=sig");
        assertThat(response.fileUrl()).startsWith("https://test-bucket.s3.ap-northeast-2.amazonaws.com/facilities/");
    }

    @Test
    @SuppressWarnings("unchecked")
    void 버킷_파일_삭제_시_올바른_key로_S3에_요청한다() {
        String fileUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/facilities/some-uuid";

        s3Service.deleteFile(fileUrl);

        verify(s3Client).deleteObject(any(Consumer.class));
    }

    @Test
    void 다른_버킷의_URL로_삭제_요청하면_예외가_발생한다() {
        String wrongUrl = "https://other-bucket.s3.ap-northeast-2.amazonaws.com/facilities/uuid";

        assertThatThrownBy(() -> s3Service.deleteFile(wrongUrl))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
