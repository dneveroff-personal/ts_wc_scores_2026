package com.tswcscores.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScoringResult {
  private int points;
  private boolean exactScore;
  private boolean correctOutcome;
  private boolean correctGoalDifference;

  public String describe() {
    if (exactScore) return "⭐ Точный счёт! +" + points + " очков";
    if (correctOutcome && correctGoalDifference)
      return "✅ Исход + разница голов! +" + points + " очков";
    if (correctOutcome) return "✅ Правильный исход! +" + points + " очков";
    return "❌ Мимо. +0 очков";
  }
}
