package com.back.sportteam.domain.review.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum SelfReportedLevel {
    BEGINNER(new BigDecimal("1.5")),
    INTERMEDIATE(new BigDecimal("3.0")),
    ADVANCED(new BigDecimal("4.5"));

    private final BigDecimal initialScore;
}
