package com.back.sportteam.domain.review.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class ParticipantReviewRequest {

    @NotBlank
    private String revieweeId;

    @DecimalMin("0.5")
    @DecimalMax("5.0")
    private BigDecimal mannerRating;

    @DecimalMin("0.5")
    @DecimalMax("5.0")
    private BigDecimal skillRating;
}
