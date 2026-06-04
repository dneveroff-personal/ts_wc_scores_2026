package com.tswcscores.service.impl;

import com.tswcscores.config.ScoringProperties;
import com.tswcscores.dto.ScoringResult;
import com.tswcscores.entity.Match;
import com.tswcscores.entity.Prediction;
import com.tswcscores.entity.User;
import com.tswcscores.repository.MatchRepository;
import com.tswcscores.repository.PredictionRepository;
import com.tswcscores.repository.UserRepository;
import com.tswcscores.service.ScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringServiceImpl implements ScoringService {

    private final ScoringProperties props;
    private final MatchRepository matchRepository;
    private final PredictionRepository predictionRepository;
    private final UserRepository userRepository;

    @Override
    public ScoringResult calculatePoints(Prediction prediction, Match match) {
        if (!match.isFinished()) {
            throw new IllegalArgumentException("Match is not finished: " + match.getId());
        }
        if (match.getHomeScore() == null || match.getAwayScore() == null) {
            throw new IllegalArgumentException("Match score is not available: " + match.getId());
        }

        int predHome = prediction.getHomeScore();
        int predAway = prediction.getAwayScore();
        int realHome = match.getHomeScore();
        int realAway = match.getAwayScore();

        // Точный счёт
        boolean exactScore = predHome == realHome && predAway == realAway;
        if (exactScore) {
            return ScoringResult.builder()
                    .points(props.getExactScore())
                    .exactScore(true)
                    .correctOutcome(true)
                    .correctGoalDifference(true)
                    .build();
        }

        // Правильный исход (победитель или ничья)
        boolean correctOutcome = getOutcome(predHome, predAway) == getOutcome(realHome, realAway);

        // Правильная разница голов
        boolean correctGoalDifference = correctOutcome &&
                (predHome - predAway) == (realHome - realAway);

        int points = 0;
        if (correctOutcome) {
            points += props.getCorrectOutcome();
            // Only add goal difference points if it's not a draw (draws always have goal difference 0)
            if (correctGoalDifference && realHome != realAway) {
                points += props.getGoalDifference();
            }
        }

        return ScoringResult.builder()
                .points(points)
                .exactScore(false)
                .correctOutcome(correctOutcome)
                .correctGoalDifference(correctGoalDifference)
                .build();
    }

    @Override
    @Transactional
    public void processFinishedMatches() {
        List<Match> matches = matchRepository.findFinishedNotCalculated();
        if (matches.isEmpty()) return;

        log.info("Processing scores for {} finished matches", matches.size());

        for (Match match : matches) {
            List<Prediction> predictions = predictionRepository.findByMatchId(match.getId());
            for (Prediction prediction : predictions) {
                ScoringResult result = calculatePoints(prediction, match);
                prediction.setPointsEarned(result.getPoints());
                predictionRepository.save(prediction);

                // Обновляем общий счёт пользователя
                User user = prediction.getUser();
                user.setTotalPoints(user.getTotalPoints() + result.getPoints());
                userRepository.save(user);

                log.debug("User {} scored {} pts for match {}", user.getDisplayName(),
                        result.getPoints(), match.getTitle());
            }
            match.setScoresCalculated(true);
            matchRepository.save(match);
            log.info("Scores calculated for match: {}", match.getTitle());
        }
    }

    /** -1 = away wins, 0 = draw, 1 = home wins */
    private int getOutcome(int home, int away) {
        return Integer.compare(home, away);
    }
}
