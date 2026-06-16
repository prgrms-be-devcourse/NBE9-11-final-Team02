package com.back.sportteam.domain.review.controller;

import com.back.sportteam.domain.review.dto.response.FacilityReviewResponse;
import com.back.sportteam.domain.review.entity.FacilityReview;
import com.back.sportteam.domain.review.service.ReviewService;
import com.back.sportteam.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReviewControllerTest {

    private ReviewService reviewService;
    private MockMvc mockMvc;
    @BeforeEach
    void setUp() {
        reviewService = mock(ReviewService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReviewController(reviewService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void 리뷰를_제출하면_201을_반환한다() throws Exception {
        String body = """
                {
                  "facilityReview": { "rating": 4.5, "comment": "좋아요" },
                  "participantReviews": []
                }
                """;

        mockMvc.perform(post("/api/v1/matches/match-1/reviews")
                        .header("X-USER-ID", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(reviewService).submitReview(eq("match-1"), eq("user-1"), any());
    }

    @Test
    void X_USER_ID_헤더가_없으면_400을_반환한다() throws Exception {
        String body = """
                {
                  "facilityReview": { "rating": 4.5, "comment": "좋아요" },
                  "participantReviews": []
                }
                """;

        mockMvc.perform(post("/api/v1/matches/match-1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 별점이_0_5단위가_아닌_경우_요청_바디_자체는_통과하고_서비스에서_검증한다() throws Exception {
        // Bean Validation은 @DecimalMin/@DecimalMax만 처리, 0.5단위는 서비스에서 검증
        String body = """
                {
                  "facilityReview": { "rating": 4.3, "comment": "좋아요" },
                  "participantReviews": []
                }
                """;

        mockMvc.perform(post("/api/v1/matches/match-1/reviews")
                        .header("X-USER-ID", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void 별점이_최솟값_0_5보다_작으면_400을_반환한다() throws Exception {
        String body = """
                {
                  "facilityReview": { "rating": 0.0, "comment": "나빠요" },
                  "participantReviews": []
                }
                """;

        mockMvc.perform(post("/api/v1/matches/match-1/reviews")
                        .header("X-USER-ID", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 별점이_최댓값_5_0을_초과하면_400을_반환한다() throws Exception {
        String body = """
                {
                  "facilityReview": { "rating": 5.5, "comment": "너무 좋아요" },
                  "participantReviews": []
                }
                """;

        mockMvc.perform(post("/api/v1/matches/match-1/reviews")
                        .header("X-USER-ID", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 시설_리뷰와_참가자_리뷰_모두_없어도_제출할_수_있다() throws Exception {
        String body = """
                {
                  "participantReviews": []
                }
                """;

        mockMvc.perform(post("/api/v1/matches/match-1/reviews")
                        .header("X-USER-ID", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void 시설_리뷰_목록을_조회하면_200을_반환한다() throws Exception {
        FacilityReview review = FacilityReview.create("match-1", "user-1", "facility-1",
                new BigDecimal("4.5"), "좋아요");
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<FacilityReviewResponse> page = new PageImpl<>(
                List.of(FacilityReviewResponse.from(review)), pageRequest, 1);
        when(reviewService.getFacilityReviews(eq("facility-1"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/facilities/facility-1/reviews"))
                .andExpect(status().isOk());

        verify(reviewService).getFacilityReviews(eq("facility-1"), any(Pageable.class));
    }

    @Test
    void 시설_리뷰_목록_조회_시_페이지_파라미터를_전달할_수_있다() throws Exception {
        PageRequest pageRequest = PageRequest.of(1, 5);
        when(reviewService.getFacilityReviews(eq("facility-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        mockMvc.perform(get("/api/v1/facilities/facility-1/reviews")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(reviewService).getFacilityReviews(eq("facility-1"), any(Pageable.class));
    }

    @Test
    void 내가_남긴_시설_리뷰를_조회하면_200을_반환한다() throws Exception {
        FacilityReview review = FacilityReview.create("match-1", "user-1", "facility-1",
                new BigDecimal("4.0"), "좋아요");
        when(reviewService.getMyFacilityReviews("user-1"))
                .thenReturn(List.of(FacilityReviewResponse.from(review)));

        mockMvc.perform(get("/api/v1/users/me/reviews/facilities")
                        .header("X-USER-ID", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].rating").value(4.0));
    }

    @Test
    void 내_리뷰_조회_시_X_USER_ID_헤더가_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/reviews/facilities"))
                .andExpect(status().isBadRequest());
    }
}
