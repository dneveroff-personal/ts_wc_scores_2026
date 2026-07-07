package com.tswcscores.service.impl;

import com.tswcscores.dto.FootballDataMatchesResponse;
import com.tswcscores.dto.FootballDataMatchesResponse.FootballDataMatch;
import com.tswcscores.entity.Match;
import com.tswcscores.entity.Team;
import com.tswcscores.repository.MatchRepository;
import com.tswcscores.repository.TeamRepository;
import jakarta.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class FootballDataService {

  private final WebClient footballDataClient;
  private final MatchRepository matchRepository;
  private final TeamRepository teamRepository;

  @Value("${football-data.api.competition-code}")
  private String competitionCode;

  /** Синхронизация при старте приложения */
  @PostConstruct
  public void syncOnStartup() {
    log.info("🚀 Initial match sync on startup...");
    syncMatches();
  }

  @Transactional
  public void syncMatches() {
    log.info("Syncing matches from football-data.org, competition: {}", competitionCode);
    try {
      FootballDataMatchesResponse response =
          footballDataClient
              .get()
              .uri("/competitions/{code}/matches", competitionCode)
              .retrieve()
              .bodyToMono(FootballDataMatchesResponse.class)
              .block();

      if (response == null || response.getMatches() == null) {
        log.warn("Empty response from football-data API");
        return;
      }

      List<FootballDataMatch> apiMatches = response.getMatches();
      log.info("Fetched {} matches from API", apiMatches.size());

      AtomicInteger saved = new AtomicInteger(0);
      for (FootballDataMatch apiMatch : apiMatches) {
        if (upsertMatch(apiMatch)) saved.incrementAndGet();
      }
      log.info("✅ Sync complete: {}/{} matches saved/updated", saved.get(), apiMatches.size());
      saved.get();

    } catch (Exception e) {
      log.error("Failed to sync matches from football-data.org: {}", e.getMessage(), e);
    }
  }

  /** @return true если матч был сохранён/обновлён */
  private boolean upsertMatch(FootballDataMatch apiMatch) {
    Team homeTeam = upsertTeam(apiMatch.getHomeTeam());
    Team awayTeam = upsertTeam(apiMatch.getAwayTeam());

    // Плей-офф матчи до жеребьёвки — команды ещё не известны, пропускаем
    if (homeTeam == null || awayTeam == null) {
      log.debug(
          "Skipping match {} — teams not yet determined (stage: {})",
          apiMatch.getId(),
          apiMatch.getStage());
      return false;
    }

    Match match =
        matchRepository
            .findByExternalId(apiMatch.getId())
            .orElseGet(
                () ->
                    Match.builder()
                        .externalId(apiMatch.getId())
                        .homeTeam(homeTeam)
                        .awayTeam(awayTeam)
                        .build());

    match.setUtcDate(OffsetDateTime.parse(apiMatch.getUtcDate()).toLocalDateTime());
    match.setStatus(Match.Status.valueOf(apiMatch.getStatus()));
    match.setStage(apiMatch.getStage());

    // group — просто строка ("GROUP_A"), не объект
    if (apiMatch.getGroup() != null) {
      match.setGroupName(apiMatch.getGroup());
    }

    if ("FINISHED".equals(apiMatch.getStatus())
        && apiMatch.getScore() != null
        && apiMatch.getScore().getFullTime() != null) {
      match.setHomeScore(apiMatch.getScore().getFullTime().getHome());
      match.setAwayScore(apiMatch.getScore().getFullTime().getAway());
    }

    matchRepository.save(match);
    return true;
  }

  private Team upsertTeam(FootballDataMatch.TeamRef ref) {
    if (ref == null || ref.getId() == null) return null;
    return teamRepository
        .findByExternalId(ref.getId())
        .orElseGet(
            () ->
                teamRepository.save(
                    Team.builder()
                        .externalId(ref.getId())
                        .name(ref.getName())
                        .shortName(ref.getShortName())
                        .tla(ref.getTla())
                        .crestUrl(ref.getCrest())
                        .build()));
  }
}
