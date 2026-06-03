-- Users: участники прогнозов
CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    telegram_id   BIGINT      NOT NULL UNIQUE,
    username      VARCHAR(64),
    first_name    VARCHAR(64),
    last_name     VARCHAR(64),
    registered_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    total_points  INT         NOT NULL DEFAULT 0,
    active        BOOLEAN     NOT NULL DEFAULT TRUE
);

-- Teams: команды турнира
CREATE TABLE teams
(
    id          BIGSERIAL PRIMARY KEY,
    external_id INT         NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    short_name  VARCHAR(20),
    tla         VARCHAR(5),
    crest_url   VARCHAR(255),
    group_name  VARCHAR(10)
);

-- Matches: матчи
CREATE TABLE matches
(
    id                BIGSERIAL PRIMARY KEY,
    external_id       INT         NOT NULL UNIQUE,
    home_team_id      BIGINT      NOT NULL REFERENCES teams (id),
    away_team_id      BIGINT      NOT NULL REFERENCES teams (id),
    utc_date          TIMESTAMP   NOT NULL,
    status            VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    stage             VARCHAR(50),
    group_name        VARCHAR(10),
    home_score        INT,
    away_score        INT,
    scores_calculated BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_match_utc_date ON matches (utc_date);
CREATE INDEX idx_match_status ON matches (status);

-- Predictions: прогнозы пользователей
CREATE TABLE predictions
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT    NOT NULL REFERENCES users (id),
    match_id      BIGINT    NOT NULL REFERENCES matches (id),
    home_score    INT       NOT NULL,
    away_score    INT       NOT NULL,
    points_earned INT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_prediction_user_match UNIQUE (user_id, match_id)
);
