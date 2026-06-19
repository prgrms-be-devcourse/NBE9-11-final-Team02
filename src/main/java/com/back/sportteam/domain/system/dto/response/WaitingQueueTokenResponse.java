package com.back.sportteam.domain.system.dto.response;

import java.time.LocalDateTime;

public record WaitingQueueTokenResponse(
        String token,
        String facilitySlotId,
        long position,
        long waitingCount,
        boolean enterable,
        LocalDateTime expiresAt
) {
}
