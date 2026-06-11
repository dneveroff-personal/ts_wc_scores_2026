package com.tswcscores.bot.keyboard;

import com.tswcscores.entity.Match;
import com.tswcscores.entity.Prediction;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

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

    /**
     * Постоянная клавиатура с основными командами — появляется вместо обычной клавиатуры телефона.
     * Отправляется один раз при /register или /start.
     */
    public static ReplyKeyboardMarkup mainMenuKeyboard() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("⚽ Матчи"));
        row1.add(new KeyboardButton("📋 Мои прогнозы"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("🏆 Рейтинг"));
        row2.add(new KeyboardButton("❓ Помощь"));

        return ReplyKeyboardMarkup.builder()
                .keyboard(java.util.List.of(row1, row2))
                .resizeKeyboard(true)      // компактный размер кнопок
                .isPersistent(true)        // не скрывается автоматически
                .build();
    }
}