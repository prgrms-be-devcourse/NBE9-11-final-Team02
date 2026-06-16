package com.back.sportteam.domain.review.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class ParticipantReviewRequest {

    @NotBlank
    private String revieweeId;

    @NotNull
    @DecimalMin("0.5")
    @DecimalMax("5.0")
    private BigDecimal mannerRating;

    @NotNull
    @DecimalMin("0.5")
    @DecimalMax("5.0")
    private BigDecimal skillRating;
}
