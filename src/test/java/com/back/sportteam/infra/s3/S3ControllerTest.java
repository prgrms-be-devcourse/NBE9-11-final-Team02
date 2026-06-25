package com.back.sportteam.infra.s3;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.sportteam.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class S3ControllerTest {

    private S3Service s3Service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        s3Service = mock(S3Service.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new S3Controller(s3Service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void presignedUrl_요청_시_uploadUrl과_fileUrl을_반환한다() throws Exception {
        when(s3Service.generatePresignedUrl("image/jpeg"))
                .thenReturn(new PresignedUrlResponse("https://upload-url", "https://file-url"));

        mockMvc.perform(get("/api/v1/s3/presigned-url")
                        .param("contentType", "image/jpeg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uploadUrl").value("https://upload-url"))
                .andExpect(jsonPath("$.data.fileUrl").value("https://file-url"));
    }

    @Test
    void contentType_없이_요청하면_400_응답을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/s3/presigned-url"))
                .andExpect(status().isBadRequest());
    }
}
