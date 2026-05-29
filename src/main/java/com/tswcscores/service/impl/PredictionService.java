package com.tswcscores.service.impl;

import com.tswcscores.entity.Match;
import com.tswcscores.entity.Prediction;
import com.tswcscores.entity.User;
import com.tswcscores.exception.DeadlinePassedException;
import com.tswcscores.exception.MatchNotFoundException;
import com.tswcscores.repository.MatchRepository;
import com.tswcscores.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final MatchRepository matchRepository;

    @Transactional
    public Prediction savePrediction(User user, long matchId, int homeScore, int awayScore) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("Match not found: " + matchId));

        if (!match.isPredictionAllowed()) {
            throw new DeadlinePassedException("Матч уже начался, дедлайн прошёл: " + match.getTitle());
        }

        Prediction prediction = predictionRepository
                .findByUserIdAndMatchId(user.getId(), match.getId())
                .orElseGet(() -> Prediction.builder().user(user).match(match).build());

        prediction.setHomeScore(homeScore);
        prediction.setAwayScore(awayScore);
        return predictionRepository.save(prediction);
    }

    public List<Prediction> getUserPredictions(User user) {
        return predictionRepository.findByUserIdWithMatch(user.getId());
    }
}
