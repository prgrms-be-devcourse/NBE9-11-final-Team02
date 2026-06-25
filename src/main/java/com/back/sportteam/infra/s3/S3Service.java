package com.back.sportteam.infra.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    public PresignedUrlResponse generatePresignedUrl(String contentType) {
        String key = "facilities/" + UUID.randomUUID();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(req -> req
                .signatureDuration(Duration.ofMinutes(s3Properties.presignedUrlExpirationMinutes()))
                .putObjectRequest(put -> put
                        .bucket(s3Properties.bucket())
                        .key(key)
                        .contentType(contentType)));

        String uploadUrl = presignedRequest.url().toString();
        String fileUrl = "https://" + s3Properties.bucket() + ".s3." + s3Properties.region() + ".amazonaws.com/" + key;

        return new PresignedUrlResponse(uploadUrl, fileUrl);
    }

    public void deleteFile(String fileUrl) {
        String key = extractKey(fileUrl);
        s3Client.deleteObject(req -> req
                .bucket(s3Properties.bucket())
                .key(key));
    }

    private String extractKey(String fileUrl) {
        String prefix = "https://" + s3Properties.bucket() + ".s3." + s3Properties.region() + ".amazonaws.com/";
        if (!fileUrl.startsWith(prefix)) {
            throw new IllegalArgumentException("이 버킷의 이미지 URL이 아닙니다. url=" + fileUrl);
        }
        return fileUrl.substring(prefix.length());
    }
}
