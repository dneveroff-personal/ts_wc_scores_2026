package com.tswcscores.bot.handler;

import com.tswcscores.entity.Match;
import com.tswcscores.entity.Prediction;
import com.tswcscores.entity.User;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Форматирование всех сообщений бота в одном месте */
public class BotMessageBuilder {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public static String welcome(User user) {
        return String.format("""
                🏆 <b>TS WC Scores 2026</b>
                
                Привет, %s! Ты зарегистрирован.
                
                📋 Команды:
                /matches — матчи ближайших 24 ч.
                /mypredictions — мои прогнозы
                /leaderboard — таблица лидеров
                
                За прогнозы начисляются очки:
                ⭐ Точный счёт — 4 очка
                ✅ Правильный исход — 2 очка
                🎯 + разница голов — +1 очко
                """, user.getDisplayName());
    }

    public static String alreadyRegistered() {
        return "Ты уже зарегистрирован! 👍\n\n/matches — посмотреть ближайшие матчи";
    }

    public static String matchList(List<Match> matches) {
        if (matches.isEmpty()) {
            return "😴 Ближайших матчей нет. Проверь позже!";
        }
        StringBuilder sb = new StringBuilder("⚽ <b>Ближайшие матчи (24 ч):</b>\n\n");
        for (Match m : matches) {
            sb.append(String.format("🆔 <code>%d</code>  <b>%s</b>\n📅 %s UTC\n",
                    m.getId(), m.getTitle(), m.getUtcDate().format(FMT)));
            if (m.getGroupName() != null) {
                sb.append("📌 ").append(m.getGroupName()).append("\n");
            }
            sb.append("\n");
        }
        sb.append("\nНажми кнопку матча или введи:\n<code>/predict {id} {гол1} {гол2}</code>");
        return sb.toString();
    }

    public static String predictionSaved(Prediction p) {
        return String.format("""
                ✅ Прогноз принят!
                
                ⚽ <b>%s</b>
                📊 Твой прогноз: <b>%d : %d</b>
                """,
                p.getMatch().getTitle(),
                p.getHomeScore(),
                p.getAwayScore());
    }

    public static String myPredictions(List<Prediction> predictions) {
        if (predictions.isEmpty()) {
            return "У тебя ещё нет прогнозов.\n/matches — посмотреть матчи";
        }
        StringBuilder sb = new StringBuilder("📋 <b>Твои прогнозы:</b>\n\n");
        for (Prediction p : predictions) {
            Match m = p.getMatch();
            sb.append(String.format("<b>%s</b>  %s\n", m.getTitle(), m.getUtcDate().format(FMT)));
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
            sb.append(String.format("%s  <b>%s</b>  —  %d pts\n", medal, u.getDisplayName(), u.getTotalPoints()));
        }
        return sb.toString();
    }

    public static String notRegistered() {
        return "Ты ещё не зарегистрирован.\n/register — начать играть";
    }

    public static String predictHelp() {
        return """
                ❓ <b>Как делать прогноз:</b>
                
                <code>/predict {id_матча} {гол1} {гол2}</code>
                
                Пример: <code>/predict 42 2 1</code>
                
                ID матча видно в /matches 👆
                """;
    }
}
