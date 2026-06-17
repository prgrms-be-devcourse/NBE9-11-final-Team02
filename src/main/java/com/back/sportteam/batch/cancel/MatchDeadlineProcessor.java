package com.back.sportteam.batch.cancel;

import com.back.sportteam.domain.match.entity.Match;
import com.back.sportteam.domain.match.entity.MatchParticipant;
import com.back.sportteam.domain.match.entity.MatchParticipantStatus;
import com.back.sportteam.domain.match.repository.MatchParticipantRepository;
import com.back.sportteam.domain.match.repository.MatchRepository;
import com.back.sportteam.domain.payment.service.PaymentRefundRequestService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchDeadlineProcessor {

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final PaymentRefundRequestService paymentRefundRequestService;

    @Transactional
    public void process(String matchId, LocalDateTime processedAt) {
        Match match = matchRepository.findByIdForUpdate(matchId).orElse(null);
        if (match == null || !match.isRecruiting() || !match.isRecruitClosed(processedAt)) {
            return;
        }

        if (match.isFull()) {
            match.confirm(processedAt);
            return;
        }

        match.cancel(processedAt);
        cancelActiveParticipants(matchId);
        paymentRefundRequestService.requestMatchRefunds(
                matchId,
                match.getReservationId(),
                PaymentRefundRequestService.MATCH_MINIMUM_PARTICIPANTS_NOT_MET,
                processedAt
        );
    }

    private void cancelActiveParticipants(String matchId) {
        matchParticipantRepository.findByMatchIdAndStatus(
                        matchId,
                        MatchParticipantStatus.ACTIVE
                )
                .forEach(MatchParticipant::cancel);
    }

}
