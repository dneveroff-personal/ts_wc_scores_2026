package com.tswcscores.bot.keyboard;

import com.tswcscores.entity.Match;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class InlineKeyboardFactory {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    /**
     * Кнопка «Сделать прогноз» для конкретного матча.
     * При нажатии заполняет поле ввода: /predict {matchId}
     */
    public static InlineKeyboardMarkup predictButton(Match match) {
        String label = "⚽ Прогноз на " + match.getHomeTeam().getTla()
                + " — " + match.getAwayTeam().getTla();
        InlineKeyboardButton btn = InlineKeyboardButton.builder()
                .text(label)
                // SwitchInlineQueryCurrentChat заполняет поле ввода, не отправляет
                .switchInlineQueryCurrentChat("/predict " + match.getId() + " ")
                .build();
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(btn))
                .build();
    }

    /**
     * Список матчей с кнопками прогноза.
     */
    public static InlineKeyboardMarkup matchListKeyboard(List<Match> matches) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Match m : matches) {
            String label = m.getHomeTeam().getTla() + " — " + m.getAwayTeam().getTla()
                    + "  " + m.getUtcDate().format(FMT);
            InlineKeyboardButton btn = InlineKeyboardButton.builder()
                    .text(label)
                    .switchInlineQueryCurrentChat("/predict " + m.getId() + " ")
                    .build();
            rows.add(List.of(btn));
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }
}
