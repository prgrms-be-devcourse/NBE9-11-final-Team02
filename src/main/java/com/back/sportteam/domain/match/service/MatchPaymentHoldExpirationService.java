package com.back.sportteam.domain.match.service;

import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@Profile("!test")
@RequiredArgsConstructor
public class MatchPaymentHoldExpirationService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private final MatchParticipantRepository matchParticipantRepository;

    @Scheduled(fixedDelayString = "${match.payment-hold.expiration-check-delay-millis}")
    @Transactional
    public void cancelExpiredPaymentHolds() {
        LocalDateTime now = LocalDateTime.now(SERVICE_ZONE);
        List<MatchParticipant> expiredParticipants =
                matchParticipantRepository.findByStatusAndPaymentDeadlineBefore(
                        MatchParticipantStatus.PAYMENT_PENDING,
                        now
                );

        expiredParticipants.forEach(participant -> cancelExpiredPaymentHold(participant, now));
    }

    private void cancelExpiredPaymentHold(MatchParticipant participant, LocalDateTime expiredAt) {
        if (participant.cancel()) {
            participant.getMatch().decreaseCurrentCount();
        }
        if (participant.isHost()) {
            participant.getMatch().cancel(expiredAt);
        }
    }
}
