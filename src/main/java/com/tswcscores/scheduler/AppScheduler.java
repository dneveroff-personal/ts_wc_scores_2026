package com.tswcscores.scheduler;

import com.tswcscores.bot.WcPredictBot;
import com.tswcscores.entity.Match;
import com.tswcscores.repository.MatchRepository;
import com.tswcscores.repository.PredictionRepository;
import com.tswcscores.service.ScoringService;
import com.tswcscores.service.impl.FootballDataService;
import jakarta.annotation.PostConstruct;
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

    /** Синхронизация при старте приложения */
    @PostConstruct
    public void syncOnStartup() {
        log.info("🚀 Startup: initial match sync");
        try {
            footballDataService.syncMatches();
            log.info("✅ Initial sync complete");
        } catch (Exception e) {
            log.error("❌ Initial sync failed: {}", e.getMessage());
        }
    }

    /** Синхронизация каждые 2 часа */
    @Scheduled(cron = "${scheduler.sync-matches}")
    public void syncMatches() {
        log.info("⏰ Scheduler: syncing matches");
        footballDataService.syncMatches();
    }

    /** Подсчёт очков каждый час */
    @Scheduled(cron = "${scheduler.calculate-scores}")
    public void calculateScores() {
        log.info("⏰ Scheduler: calculating scores");
        scoringService.processFinishedMatches();
    }

    /** Напоминания за ~1 час до матча */
    @Scheduled(cron = "${scheduler.send-reminders}")
    public void sendReminders() {
        LocalDateTime from = LocalDateTime.now().plusMinutes(50);
        LocalDateTime to   = LocalDateTime.now().plusMinutes(70);

        List<Match> upcoming = matchRepository.findMatchesStartingBetween(from, to);
        if (upcoming.isEmpty()) return;

        log.info("⏰ Scheduler: sending reminders for {} matches", upcoming.size());
        for (Match match : upcoming) {
            List<Long> telegramIds = predictionRepository.findTelegramIdsWithoutPrediction(match.getId());
            for (Long telegramId : telegramIds) {
                bot.sendNotification(telegramId, String.format(
                        "⏰ Через час матч: <b>%s</b> (%s)\nТы ещё не сделал прогноз!\nНажми /matches 👇",
                        match.getTitle(), match.getUtcDate().format(FMT)));
            }
        }
    }
}
