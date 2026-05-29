package com.tswcscores.scheduler;

import com.tswcscores.repository.MatchRepository;
import com.tswcscores.repository.PredictionRepository;
import com.tswcscores.service.ScoringService;
import com.tswcscores.service.impl.FootballDataService;
import com.tswcscores.bot.WcPredictBot;
import com.tswcscores.entity.Match;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppScheduler {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    private final FootballDataService footballDataService;
    private final ScoringService scoringService;
    private final MatchRepository matchRepository;
    private final PredictionRepository predictionRepository;
    private final WcPredictBot bot;

    /** Синхронизация расписания и результатов с API */
    @Scheduled(cron = "${scheduler.sync-matches}")
    public void syncMatches() {
        log.info("⏰ Scheduler: syncing matches");
        footballDataService.syncMatches();
    }

    /** Подсчёт очков для завершённых матчей */
    @Scheduled(cron = "${scheduler.calculate-scores}")
    public void calculateScores() {
        log.info("⏰ Scheduler: calculating scores");
        scoringService.processFinishedMatches();
    }

    /** Напоминания за ~1 час до матча */
    @Scheduled(cron = "${scheduler.send-reminders}")
    public void sendReminders() {
        LocalDateTime from = LocalDateTime.now().plusMinutes(50);
        LocalDateTime to = LocalDateTime.now().plusMinutes(70);

        List<Match> upcoming = matchRepository.findMatchesStartingBetween(from, to);
        if (upcoming.isEmpty()) return;

        log.info("⏰ Scheduler: sending reminders for {} matches", upcoming.size());
        for (Match match : upcoming) {
            List<Long> telegramIds = predictionRepository.findTelegramIdsWithoutPrediction(match.getId());
            for (Long telegramId : telegramIds) {
                String msg = String.format(
                        "⏰ Через час матч: <b>%s</b> (%s)\nТы ещё не сделал прогноз!\n" +
                        "Нажми /matches и сделай ставку 👇",
                        match.getTitle(),
                        match.getUtcDate().format(FMT)
                );
                bot.sendNotification(telegramId, msg);
            }
        }
    }
}
