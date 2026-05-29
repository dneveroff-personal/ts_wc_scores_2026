package com.tswcscores.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "matches")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Match {

    public enum Status {
        SCHEDULED, TIMED, IN_PLAY, PAUSED, FINISHED, POSTPONED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private Integer externalId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    @Column(name = "utc_date", nullable = false)
    private LocalDateTime utcDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.SCHEDULED;

    @Column(name = "stage")
    private String stage;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "scores_calculated", nullable = false)
    @Builder.Default
    private boolean scoresCalculated = false;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Prediction> predictions;

    public boolean isFinished() {
        return status == Status.FINISHED;
    }

    public boolean isPredictionAllowed() {
        return utcDate.isAfter(LocalDateTime.now());
    }

    public String getTitle() {
        return homeTeam.getDisplayName() + " — " + awayTeam.getDisplayName();
    }
}
