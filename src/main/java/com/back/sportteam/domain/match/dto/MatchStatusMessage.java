package com.back.sportteam.domain.match.dto;

public record MatchStatusMessage(
        Long matchId,
        int currentParticipants,
        int maxParticipants
) {}