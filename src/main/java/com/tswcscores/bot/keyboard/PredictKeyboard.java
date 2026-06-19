package com.tswcscores.bot.keyboard;

import com.tswcscores.entity.Match;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Клавиатуры для пошагового выбора счёта.
 *
 * Формат callback_data:
 *   "ph:{matchId}"          — выбор голов хозяев (pick home)
 *   "pa:{matchId}:{home}"   — выбор голов гостей (pick away)
 *   "pc:{matchId}:{home}:{away}" — подтверждение прогноза (pick confirm)
 */
public class PredictKeyboard {

    public static final String PICK_HOME    = "ph:";
    public static final String PICK_AWAY    = "pa:";
    public static final String PICK_CONFIRM = "pc:";

    private static final int[] GOALS_ROW1 = {0, 1, 2, 3, 4};
    private static final int[] GOALS_ROW2 = {5, 6, 7, 8, 9};

    /** Шаг 1 — выбор голов хозяев */
    public static List<List<Map<String, String>>> homeGoalsKeyboard(long matchId) {
        List<List<Map<String, String>>> rows = new ArrayList<>();
        List<Map<String, String>> row1 = new ArrayList<>();
        for (int g : GOALS_ROW1) row1.add(Map.of("text", String.valueOf(g),
                "callback_data", PICK_AWAY + matchId + ":" + g));
        List<Map<String, String>> row2 = new ArrayList<>();
        for (int g : GOALS_ROW2) row2.add(Map.of("text", String.valueOf(g),
                "callback_data", PICK_AWAY + matchId + ":" + g));
        rows.add(row1);
        rows.add(row2);
        rows.add(List.of(Map.of("text", "❌ Отмена", "callback_data", "cancel")));
        return rows;
    }

    /** Шаг 2 — выбор голов гостей */
    public static List<List<Map<String, String>>> awayGoalsKeyboard(long matchId, int homeGoals) {
        List<List<Map<String, String>>> rows = new ArrayList<>();
        List<Map<String, String>> row1 = new ArrayList<>();
        for (int g : GOALS_ROW1) row1.add(Map.of("text", String.valueOf(g),
                "callback_data", PICK_CONFIRM + matchId + ":" + homeGoals + ":" + g));
        List<Map<String, String>> row2 = new ArrayList<>();
        for (int g : GOALS_ROW2) row2.add(Map.of("text", String.valueOf(g),
                "callback_data", PICK_CONFIRM + matchId + ":" + homeGoals + ":" + g));
        rows.add(row1);
        rows.add(row2);
        rows.add(List.of(
                Map.of("text", "← Назад", "callback_data", PICK_HOME + matchId),
                Map.of("text", "❌ Отмена", "callback_data", "cancel")
        ));
        return rows;
    }

    /** Сообщение шага 1 */
    public static String homePrompt(Match match) {
        return String.format(
                "⚽ <b>%s</b>\n\nСколько голов забьёт <b>%s</b>?",
                match.getTitle(),
                match.getHomeTeam().getDisplayName()
        );
    }

    /** Сообщение шага 2 */
    public static String awayPrompt(Match match, int homeGoals) {
        return String.format(
                "⚽ <b>%s</b>\n\n%s: <b>%d</b>\n\nСколько голов забьёт <b>%s</b>?",
                match.getTitle(),
                match.getHomeTeam().getDisplayName(), homeGoals,
                match.getAwayTeam().getDisplayName()
        );
    }
}
