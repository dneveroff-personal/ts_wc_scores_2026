package com.tswcscores.bot.handler;

import com.tswcscores.entity.Match;
import com.tswcscores.entity.Prediction;
import com.tswcscores.entity.User;
import com.tswcscores.service.impl.TimezoneService;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BotMessageBuilder {

  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

  public static String welcome(User user) {
    return String.format(
        """
        🏆 <b>TS WC Scores 2026</b>

        Привет, %s! Ты зарегистрирован.

        Используй /help чтобы увидеть все команды.

        За прогнозы начисляются очки:
        ⭐ Точный счёт — 5 очков
        ✅ Правильный исход — 2 очка
        🎯 + разница голов — +1 очко
        """,
        user.getDisplayName());
  }

  public static String alreadyRegistered() {
    return "Ты уже зарегистрирован! 👍\n\nИспользуй /help для списка команд.";
  }

  public static String help(String botVersion) {
    return String.format(
        """
        🏆 <b>TS WC Scores 2026 — команды:</b>

        👤 <b>Участие</b>
        /register — зарегистрироваться в игре

        ⚽ <b>Матчи и прогнозы</b>
        /matches — матчи ближайших 24 часов
        /predict {id} {гол1} {гол2} — сделать прогноз
            пример: <code>/predict 42 2 1</code>
        /mypredictions — мои прогнозы и очки

        🏅 <b>Рейтинг</b>
        /leaderboard — таблица лидеров

        🕐 <b>Настройки</b>
        /timezone — установить часовой пояс для отображения матчей

        🔧 <b>Служебные (для админа)</b>
        /sync — принудительная синхронизация матчей с API
        /calcscore — принудительный подсчёт очков

        /help — эта справка

        ver. %s
        """,
        botVersion);
  }

  public static String matchList(List<Match> matches) {
    return matchList(null, matches);
  }

  public static String matchList(User user, List<Match> matches) {
    if (matches.isEmpty()) {
      return "😴 Ближайших матчей нет. Проверь позже!\n\nМатчи синхронизируются каждые 2 часа.";
    }
    ZoneId zone = user != null ? TimezoneService.getUserZone(user) : ZoneId.of("Europe/Moscow");
    StringBuilder sb = new StringBuilder("⚽ <b>Ближайшие матчи (24 ч):</b>\n\n");
    for (Match m : matches) {
      String localTime =
          m.getUtcDate().atZone(ZoneId.of("UTC")).withZoneSameInstant(zone).format(FMT);
      String abbr = TimezoneService.getZoneAbbreviation(zone);
      sb.append(
          String.format(
              "🆔 <code>%d</code>  <b>%s</b>\n📅 %s (%s)\n",
              m.getId(), m.getTitle(), localTime, abbr));
      if (m.getGroupName() != null) {
        sb.append("📌 ").append(m.getGroupName()).append("\n");
      }
      sb.append("\n");
    }
    sb.append("Нажми кнопку ниже чтобы сделать прогноз 👇");
    return sb.toString();
  }

  public static String predictionSaved(Prediction p) {
    return String.format(
        """
        ✅ Прогноз принят!

        ⚽ <b>%s</b>
        📊 Твой прогноз: <b>%d : %d</b>
        """,
        p.getMatch().getTitle(), p.getHomeScore(), p.getAwayScore());
  }

  public static String myPredictions(List<Prediction> predictions) {
    return myPredictions(null, predictions);
  }

  public static String myPredictions(User user, List<Prediction> predictions) {
    if (predictions.isEmpty()) {
      return "📋 Нет прогнозов на ближайшие матчи.\n\n"
          + "/matches — посмотреть матчи и сделать прогноз";
    }
    ZoneId zone = user != null ? TimezoneService.getUserZone(user) : ZoneId.of("Europe/Moscow");
    String abbr = TimezoneService.getZoneAbbreviation(zone);
    StringBuilder sb = new StringBuilder("📋 <b>Твои прогнозы (ближайшие 24ч):</b>\n\n");
    for (Prediction p : predictions) {
      Match m = p.getMatch();
      String predLocalTime =
          m.getUtcDate().atZone(ZoneId.of("UTC")).withZoneSameInstant(zone).format(FMT);
      sb.append(String.format("<b>%s</b>  %s (%s)\n", m.getTitle(), predLocalTime, abbr));
      sb.append(String.format("Прогноз: <b>%d:%d</b>", p.getHomeScore(), p.getAwayScore()));
      if (m.isFinished() && m.getHomeScore() != null) {
        sb.append(String.format("  |  Факт: %d:%d", m.getHomeScore(), m.getAwayScore()));
        if (p.getPointsEarned() != null) {
          sb.append(String.format("  →  <b>+%d pts</b>", p.getPointsEarned()));
        }
      } else {
        sb.append("  ⏳");
      }
      sb.append("\n\n");
    }
    return sb.toString();
  }

  public static String leaderboard(List<User> users) {
    if (users.isEmpty()) return "Пока никто не зарегистрирован 😅";
    StringBuilder sb = new StringBuilder("🏆 <b>Таблица лидеров:</b>\n\n");
    String[] medals = {"🥇", "🥈", "🥉"};
    for (int i = 0; i < users.size(); i++) {
      User u = users.get(i);
      String medal = i < medals.length ? medals[i] : (i + 1) + ".";
      sb.append(
          String.format(
              "%s  <b>%s</b>  —  %d pts\n", medal, u.getDisplayName(), u.getTotalPoints()));
    }
    return sb.toString();
  }

  public static String notRegistered() {
    return "Ты ещё не зарегистрирован.\n/register — начать играть";
  }

  public static String syncStarted() {
    return "🔄 Синхронизация матчей запущена...";
  }

  public static String syncDone() {
    return "✅ Синхронизация завершена. Используй /matches чтобы проверить.";
  }

  public static String calcScoreStarted() {
    return "🔄 Подсчёт очков запущен...";
  }

  public static String calcScoreDone() {
    return "✅ Подсчёт очков завершён. Используй /leaderboard чтобы проверить.";
  }

  public static String groupLeaderboard(
      String groupTitle, java.util.List<com.tswcscores.entity.UserGroupPoints> entries) {
    if (entries.isEmpty())
      return "В этой группе пока нет участников 😅\n/register — зарегистрироваться";
    StringBuilder sb = new StringBuilder();
    sb.append(
        String.format("🏆 <b>Рейтинг группы %s:</b>\n\n", groupTitle != null ? groupTitle : ""));
    String[] medals = {"🥇", "🥈", "🥉"};
    for (int i = 0; i < entries.size(); i++) {
      var ugp = entries.get(i);
      String medal = i < medals.length ? medals[i] : (i + 1) + ".";
      sb.append(
          String.format(
              "%s  <b>%s</b>  —  %d pts\n",
              medal, ugp.getUser().getDisplayName(), ugp.getPoints()));
    }
    return sb.toString();
  }
}
