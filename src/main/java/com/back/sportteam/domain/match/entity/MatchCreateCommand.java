package com.back.sportteam.domain.match.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MatchCreateCommand {

    private String reservationId;
    private String hostId;
    private String title;
    private SportType sportType;
    private int minParticipants;
    private int maxParticipants;
    private int feePerPerson;
    private SkillLevel minSkillLevel;
    private SkillLevel maxSkillLevel;
    private RequiredGender requiredGender;
    private LocalDateTime recruitDeadline;
    private LocalDateTime cancelDeadline;
}
