package com.tswcscores.service;

import com.tswcscores.dto.ScoringResult;
import com.tswcscores.entity.Match;
import com.tswcscores.entity.Prediction;

public interface ScoringService {

  /**
   * Подсчитывает очки за один прогноз на основе финального счёта матча.
   *
   * @param prediction прогноз пользователя
   * @param match      матч с финальным счётом
   * @return результат подсчёта с деталями
   * @throws IllegalArgumentException если матч не завершён или счёт не известен
   */
  ScoringResult calculatePoints(Prediction prediction, Match match);

  /**
   * Запускает подсчёт для всех непосчитанных завершённых матчей.
   */
  void processFinishedMatches();
}
