package com.back.sportteam.domain.match.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SkillLevel {
    ANY(0),
    LEVEL_1(1),
    LEVEL_2(2),
    LEVEL_3(3),
    LEVEL_4(4),
    LEVEL_5(5);

    private final int score;

    public boolean isAny() {
        return this == ANY;
    }

    public boolean isHigherThan(SkillLevel other) {
        return this.score > other.score;
    }
}
