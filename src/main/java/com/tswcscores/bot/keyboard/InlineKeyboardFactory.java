package com.tswcscores.bot.keyboard;

import com.tswcscores.entity.Match;
import com.tswcscores.entity.Prediction;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InlineKeyboardFactory {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM HH:mm");
    public static final String PREDICT_PREFIX = "predict:";

    /**
     * Список матчей с учётом уже сделанных прогнозов пользователя.
     *
     * Кнопка без прогноза:
     *   🇫🇷 Франция — Бразилия 🇧🇷  14.06 18:00
     *   При нажатии — шлёт callback боту, бот отвечает подсказкой с командой
     *
     * Кнопка с прогнозом:
     *   ✅ 🇫🇷 Франция — Бразилия 🇧🇷  2:1
     *   При нажатии — тоже вставляет команду (можно изменить прогноз)
     */
    public static InlineKeyboardMarkup matchListKeyboard(List<Match> matches, List<Prediction> userPredictions) {
        // Быстрый lookup matchId → prediction
        Map<Long, Prediction> predMap = userPredictions.stream()
                .collect(Collectors.toMap(p -> p.getMatch().getId(), p -> p));

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Match m : matches) {
            String homeFlag = FlagEmoji.fromTla(m.getHomeTeam().getTla());
            String awayFlag = FlagEmoji.fromTla(m.getAwayTeam().getTla());
            String homeName = m.getHomeTeam().getDisplayName();
            String awayName = m.getAwayTeam().getDisplayName();

            Prediction existing = predMap.get(m.getId());
            String label;
            if (existing != null) {
                // Уже есть прогноз — показываем счёт и галочку
                label = String.format("✅ %s %s — %s %s  [%d:%d]",
                        homeFlag, homeName, awayName, awayFlag,
                        existing.getHomeScore(), existing.getAwayScore());
            } else {
                // Прогноза нет
                label = String.format("%s %s — %s %s  %s",
                        homeFlag, homeName, awayName, awayFlag,
                        m.getUtcDate().format(FMT));
            }

            // switchInlineQueryCurrentChat вставляет текст в поле ввода пользователя.
            // В группе Telegram добавляет "@botname " перед текстом — это нормально,
            // бот умеет обрабатывать команды вида "@botname /predict 42 2 1".
            InlineKeyboardButton btn = InlineKeyboardButton.builder()
                    .text(label)
                    .switchInlineQueryCurrentChat("/predict " + m.getId() + " ")
                    .build();
            rows.add(List.of(btn));
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }
}
