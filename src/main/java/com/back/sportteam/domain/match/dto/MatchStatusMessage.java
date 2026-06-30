package com.back.sportteam.domain.match.dto;

public record MatchStatusMessage(
        String matchId,
        int currentParticipants,
        int maxParticipants
) {}