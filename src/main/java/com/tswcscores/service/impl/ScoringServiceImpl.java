package com.tswcscores.service.impl;

import com.tswcscores.config.ScoringProperties;
import com.tswcscores.dto.ScoringResult;
import com.tswcscores.entity.*;
import com.tswcscores.repository.*;
import com.tswcscores.service.ScoringService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringServiceImpl implements ScoringService {

  private final ScoringProperties props;
  private final MatchRepository matchRepository;
  private final PredictionRepository predictionRepository;
  private final UserRepository userRepository;
  private final UserGroupPointsRepository userGroupPointsRepository;

  @Override
  public ScoringResult calculatePoints(Prediction prediction, Match match) {
    if (!match.isFinished())
      throw new IllegalArgumentException("Match is not finished: " + match.getId());
    if (match.getHomeScore() == null || match.getAwayScore() == null)
      throw new IllegalArgumentException("Match score not available: " + match.getId());

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
    boolean correctGoalDifference =
        correctOutcome && (predHome - predAway) == (realHome - realAway);

    int points = 0;
    if (correctOutcome) {
      points += props.getCorrectOutcome();
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

        if (result.getPoints() > 0) {
          User user = prediction.getUser();

          // Глобальные очки
          user.setTotalPoints(user.getTotalPoints() + result.getPoints());
          userRepository.save(user);

          // Групповые очки — начисляем во все группы, где есть этот пользователь
          List<UserGroupPoints> groupEntries =
              userGroupPointsRepository.findAllByUserId(user.getId());
          for (UserGroupPoints ugp : groupEntries) {
            ugp.setPoints(ugp.getPoints() + result.getPoints());
            userGroupPointsRepository.save(ugp);
          }

          log.debug(
              "User {} +{} pts for match {}",
              user.getDisplayName(),
              result.getPoints(),
              match.getTitle());
        }
      }
      match.setScoresCalculated(true);
      matchRepository.save(match);
      log.info("Scores calculated for match: {}", match.getTitle());
    }
  }

  private int getOutcome(int home, int away) {
    return Integer.compare(home, away);
  }
}
