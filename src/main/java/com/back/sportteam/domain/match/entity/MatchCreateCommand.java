package com.back.sportteam.domain.match.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class MatchCreateCommand {

    private String reservationId;
    private String hostId;
    private String title;
    private SportType sportType;
    private int capacity;
    private int feePerPerson;
    private SkillLevel minSkillLevel;
    private SkillLevel maxSkillLevel;
    private RequiredGender requiredGender;
    private LocalDate matchDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDateTime recruitDeadline;
    private LocalDateTime participantCancelDeadline;
    private LocalDateTime hostCancelDeadline;   
}
