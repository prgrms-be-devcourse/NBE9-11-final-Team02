package com.back.sportteam.infra.s3;

public record PresignedUrlResponse(
        String uploadUrl,
        String fileUrl
) {}
