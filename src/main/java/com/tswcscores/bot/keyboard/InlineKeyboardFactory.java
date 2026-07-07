package com.tswcscores.bot.keyboard;

import com.tswcscores.entity.Match;
import com.tswcscores.entity.Prediction;
import com.tswcscores.entity.User;
import com.tswcscores.service.impl.TimezoneService;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InlineKeyboardFactory {

  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM HH:mm");
  public static final String PREDICT_PREFIX = "predict:";

  /** Inline-клавиатура со списком матчей. Возвращает структуру для Telegram API JSON. */
  public static List<List<Map<String, String>>> matchListKeyboard(
      List<Match> matches, List<Prediction> userPredictions) {
    return matchListKeyboard(null, matches, userPredictions);
  }

  public static List<List<Map<String, String>>> matchListKeyboard(
      User user, List<Match> matches, List<Prediction> userPredictions) {

    Map<Long, Prediction> predMap =
        userPredictions.stream().collect(Collectors.toMap(p -> p.getMatch().getId(), p -> p));

    ZoneId zone = user != null ? TimezoneService.getUserZone(user) : ZoneId.of("Europe/Moscow");

    List<List<Map<String, String>>> rows = new ArrayList<>();
    for (Match m : matches) {
      String homeFlag = FlagEmoji.fromTla(m.getHomeTeam().getTla());
      String awayFlag = FlagEmoji.fromTla(m.getAwayTeam().getTla());
      String homeName = m.getHomeTeam().getDisplayName();
      String awayName = m.getAwayTeam().getDisplayName();

      Prediction existing = predMap.get(m.getId());
      String label;
      if (existing != null) {
        label =
            String.format(
                "✅ %s %s — %s %s  [%d:%d]",
                homeFlag,
                homeName,
                awayName,
                awayFlag,
                existing.getHomeScore(),
                existing.getAwayScore());
      } else {
        String localTime =
            m.getUtcDate().atZone(ZoneId.of("UTC")).withZoneSameInstant(zone).format(FMT);
        label =
            String.format("%s %s — %s %s  %s", homeFlag, homeName, awayName, awayFlag, localTime);
      }

      // callbackData — работает везде (личка и группа), не требует Inline Mode.
      // При нажатии бот получает callback и отвечает подсказкой с командой.
      rows.add(List.of(Map.of("text", label, "callback_data", PREDICT_PREFIX + m.getId())));
    }
    return rows;
  }

  /** Кнопки для ReplyKeyboard — возвращает строки с текстами кнопок. */
  public static List<List<String>> mainMenuButtons() {
    return List.of(List.of("⚽ Матчи", "📋 Мои прогнозы"), List.of("🏆 Рейтинг", "❓ Помощь"));
  }
}
