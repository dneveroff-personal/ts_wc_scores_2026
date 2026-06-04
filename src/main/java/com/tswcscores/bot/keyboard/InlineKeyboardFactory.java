package com.tswcscores.bot.keyboard;

import com.tswcscores.entity.Match;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class InlineKeyboardFactory {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    // Префикс callback data для прогнозов
    public static final String PREDICT_PREFIX = "predict:";

    /**
     * Список матчей — каждая кнопка отправляет callback "predict:{matchId}".
     * Бот поймает его и попросит пользователя ввести счёт.
     */
    public static InlineKeyboardMarkup matchListKeyboard(List<Match> matches) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Match m : matches) {
            String label = "⚽ " + m.getHomeTeam().getTla()
                    + " — " + m.getAwayTeam().getTla()
                    + "  " + m.getUtcDate().format(FMT);
            InlineKeyboardButton btn = InlineKeyboardButton.builder()
                    .text(label)
                    // callbackData работает без Inline Mode — просто шлёт событие боту
                    .callbackData(PREDICT_PREFIX + m.getId())
                    .build();
            rows.add(List.of(btn));
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }
}
