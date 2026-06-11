package com.tswcscores.bot.keyboard;

import com.tswcscores.entity.Match;
import com.tswcscores.entity.Prediction;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
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
     * Список матчей. В 10.x кнопки создаются через new, не builder().
     * InlineKeyboardMarkup принимает List<InlineKeyboardRow>.
     */
    public static InlineKeyboardMarkup matchListKeyboard(List<Match> matches, List<Prediction> userPredictions) {
        Map<Long, Prediction> predMap = userPredictions.stream()
                .collect(Collectors.toMap(p -> p.getMatch().getId(), p -> p));

        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (Match m : matches) {
            String homeFlag = FlagEmoji.fromTla(m.getHomeTeam().getTla());
            String awayFlag = FlagEmoji.fromTla(m.getAwayTeam().getTla());
            String homeName = m.getHomeTeam().getDisplayName();
            String awayName = m.getAwayTeam().getDisplayName();

            Prediction existing = predMap.get(m.getId());
            String label;
            if (existing != null) {
                label = String.format("✅ %s %s — %s %s  [%d:%d]",
                        homeFlag, homeName, awayName, awayFlag,
                        existing.getHomeScore(), existing.getAwayScore());
            } else {
                label = String.format("%s %s — %s %s  %s",
                        homeFlag, homeName, awayName, awayFlag,
                        m.getUtcDate().format(FMT));
            }

            // В 10.x: new InlineKeyboardButton(text), затем setCallbackData или setSwitchInlineQueryCurrentChat
            InlineKeyboardButton btn = new InlineKeyboardButton(label);
            btn.setCallbackData(PREDICT_PREFIX + m.getId());

            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(btn);
            rows.add(row);
        }
        return new InlineKeyboardMarkup(rows);
    }

    /**
     * Постоянная ReplyKeyboard с основными командами.
     * В 10.x: new KeyboardButton(text), KeyboardRow как ArrayList.
     */
    public static ReplyKeyboardMarkup mainMenuKeyboard() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("⚽ matches"));
        row1.add(new KeyboardButton("📋 my predictions"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("🏆 leaderboard"));
        row2.add(new KeyboardButton("❓ help"));

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(List.of(row1, row2));
        markup.setResizeKeyboard(true);
        markup.setIsPersistent(true);
        return markup;
    }
}
