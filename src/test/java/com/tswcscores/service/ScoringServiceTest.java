package com.tswcscores.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.tswcscores.config.ScoringProperties;
import com.tswcscores.dto.ScoringResult;
import com.tswcscores.entity.Match;
import com.tswcscores.entity.Prediction;
import com.tswcscores.entity.User;
import com.tswcscores.repository.MatchRepository;
import com.tswcscores.repository.PredictionRepository;
import com.tswcscores.repository.UserGroupPointsRepository;
import com.tswcscores.repository.UserRepository;
import com.tswcscores.service.impl.ScoringServiceImpl;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScoringService: подсчёт очков")
class ScoringServiceTest {

  private ScoringServiceImpl scoringService;
  private ScoringProperties props;

  @Mock private MatchRepository matchRepository;
  @Mock private PredictionRepository predictionRepository;
  @Mock private UserRepository userRepository;
  @Mock private UserGroupPointsRepository userGroupPointsRepository;

  @BeforeEach
  void setUp() {
    props = new ScoringProperties();
    props.setExactScore(5);
    props.setCorrectOutcome(2);
    props.setGoalDifference(1);
    scoringService =
        new ScoringServiceImpl(
            props,
            matchRepository,
            predictionRepository,
            userRepository,
            userGroupPointsRepository);
  }

  @Test
  @DisplayName("Точный счёт → 5 очков")
  void exactScore_returns4Points() {
    Prediction pred = prediction(2, 1);
    Match match = finishedMatch(2, 1);

    ScoringResult result = scoringService.calculatePoints(pred, match);

    assertThat(result.getPoints()).isEqualTo(5);
    assertThat(result.isExactScore()).isTrue();
    assertThat(result.isCorrectOutcome()).isTrue();
  }

  @Test
  @DisplayName("Правильный исход + разница голов → 3 очка")
  void correctOutcomeAndDifference_returns3Points() {
    Prediction pred = prediction(3, 1); // разница 2
    Match match = finishedMatch(2, 0); // разница 2, победитель тот же

    ScoringResult result = scoringService.calculatePoints(pred, match);

    assertThat(result.getPoints()).isEqualTo(3);
    assertThat(result.isExactScore()).isFalse();
    assertThat(result.isCorrectOutcome()).isTrue();
    assertThat(result.isCorrectGoalDifference()).isTrue();
  }

  @Test
  @DisplayName("Правильный исход, неверная разница → 2 очка")
  void correctOutcomeWrongDifference_returns2Points() {
    Prediction pred = prediction(2, 1); // разница 1
    Match match = finishedMatch(3, 1); // разница 2, победитель тот же

    ScoringResult result = scoringService.calculatePoints(pred, match);

    assertThat(result.getPoints()).isEqualTo(2);
    assertThat(result.isCorrectOutcome()).isTrue();
    assertThat(result.isCorrectGoalDifference()).isFalse();
  }

  @Test
  @DisplayName("Ничья угадана → 2 очка (исход)")
  void correctDraw_returns2Points() {
    Prediction pred = prediction(1, 1);
    Match match = finishedMatch(2, 2);

    ScoringResult result = scoringService.calculatePoints(pred, match);

    assertThat(result.getPoints()).isEqualTo(2);
    assertThat(result.isCorrectOutcome()).isTrue();
  }

  @Test
  @DisplayName("Точная ничья → 5 очков")
  void exactDraw_returns5Points() {
    Prediction pred = prediction(0, 0);
    Match match = finishedMatch(0, 0);

    ScoringResult result = scoringService.calculatePoints(pred, match);

    assertThat(result.getPoints()).isEqualTo(5);
    assertThat(result.isExactScore()).isTrue();
  }

  @Test
  @DisplayName("Неверный исход → 0 очков")
  void wrongOutcome_returns0Points() {
    Prediction pred = prediction(2, 0); // победа хозяев
    Match match = finishedMatch(0, 1); // победа гостей

    ScoringResult result = scoringService.calculatePoints(pred, match);

    assertThat(result.getPoints()).isEqualTo(0);
    assertThat(result.isCorrectOutcome()).isFalse();
  }

  @Test
  @DisplayName("Незавершённый матч → исключение")
  void notFinishedMatch_throwsException() {
    Prediction pred = prediction(1, 0);
    Match match = finishedMatch(1, 0);
    match.setStatus(Match.Status.SCHEDULED);

    assertThatThrownBy(() -> scoringService.calculatePoints(pred, match))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Матч без счёта → исключение")
  void matchWithoutScore_throwsException() {
    Prediction pred = prediction(1, 0);
    Match match =
        Match.builder().status(Match.Status.FINISHED).homeScore(null).awayScore(null).build();

    assertThatThrownBy(() -> scoringService.calculatePoints(pred, match))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // --- helpers ---

  private Prediction prediction(int home, int away) {
    return Prediction.builder()
        .homeScore(home)
        .awayScore(away)
        .user(mock(User.class))
        .match(mock(Match.class))
        .build();
  }

  private Match finishedMatch(int home, int away) {
    return Match.builder()
        .status(Match.Status.FINISHED)
        .homeScore(home)
        .awayScore(away)
        .utcDate(LocalDateTime.now().minusHours(2))
        .build();
  }
}
