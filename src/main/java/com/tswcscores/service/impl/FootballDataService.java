package com.tswcscores.service.impl;

import com.tswcscores.dto.FootballDataMatchesResponse;
import com.tswcscores.dto.FootballDataMatchesResponse.FootballDataMatch;
import com.tswcscores.entity.Match;
import com.tswcscores.entity.Team;
import com.tswcscores.repository.MatchRepository;
import com.tswcscores.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FootballDataService {

    private final WebClient footballDataClient;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;

    @Value("${football-data.api.competition-code}")
    private String competitionCode;

    @Transactional
    public void syncMatches() {
        log.info("Syncing matches from football-data.org for competition: {}", competitionCode);
        try {
            FootballDataMatchesResponse response = footballDataClient.get()
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

            for (FootballDataMatch apiMatch : apiMatches) {
                upsertMatch(apiMatch);
            }
        } catch (Exception e) {
            log.error("Failed to sync matches from football-data.org", e);
        }
    }

    private void upsertMatch(FootballDataMatch apiMatch) {
        Team homeTeam = upsertTeam(apiMatch.getHomeTeam());
        Team awayTeam = upsertTeam(apiMatch.getAwayTeam());

        Match match = matchRepository.findByExternalId(apiMatch.getId())
                .orElseGet(() -> Match.builder()
                        .externalId(apiMatch.getId())
                        .homeTeam(homeTeam)
                        .awayTeam(awayTeam)
                        .build());

        match.setUtcDate(OffsetDateTime.parse(apiMatch.getUtcDate()).toLocalDateTime());
        match.setStatus(Match.Status.valueOf(apiMatch.getStatus()));
        match.setStage(apiMatch.getStage());
        if (apiMatch.getGroup() != null) {
            match.setGroupName(apiMatch.getGroup().getName());
        }

        // Обновляем счёт для завершённых матчей
        if ("FINISHED".equals(apiMatch.getStatus()) && apiMatch.getScore() != null
                && apiMatch.getScore().getFullTime() != null) {
            match.setHomeScore(apiMatch.getScore().getFullTime().getHome());
            match.setAwayScore(apiMatch.getScore().getFullTime().getAway());
        }

        matchRepository.save(match);
    }

    private Team upsertTeam(FootballDataMatch.TeamRef ref) {
        if (ref == null || ref.getId() == null) return null;
        return teamRepository.findByExternalId(ref.getId())
                .orElseGet(() -> teamRepository.save(Team.builder()
                        .externalId(ref.getId())
                        .name(ref.getName())
                        .shortName(ref.getShortName())
                        .tla(ref.getTla())
                        .crestUrl(ref.getCrest())
                        .build()));
    }
}
