package com.tswcscores.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

/** Ответ от football-data.org /v4/competitions/{code}/matches */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FootballDataMatchesResponse {
  private List<FootballDataMatch> matches;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class FootballDataMatch {
    private Integer id;
    private String utcDate;
    private String status;
    private String stage;
    // API возвращает "group": "GROUP_A" — просто строка, не объект
    private String group;
    private TeamRef homeTeam;
    private TeamRef awayTeam;
    private Score score;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamRef {
      private Integer id;
      private String name;
      private String shortName;
      private String tla;
      private String crest;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Score {
      private FullTime fullTime;

      @Data
      @JsonIgnoreProperties(ignoreUnknown = true)
      public static class FullTime {
        private Integer home;
        private Integer away;
      }
    }
  }
}
