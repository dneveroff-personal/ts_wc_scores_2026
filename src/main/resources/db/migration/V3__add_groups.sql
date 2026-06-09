-- Telegram-группы где работает бот
CREATE TABLE chat_groups
(
    id         BIGSERIAL PRIMARY KEY,
    chat_id    BIGINT       NOT NULL UNIQUE,  -- Telegram chat_id группы (отрицательное число)
    title      VARCHAR(255),                   -- Название группы
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Очки пользователя в конкретной группе
-- Отдельно от users.total_points (тот — глобальный)
CREATE TABLE user_group_points
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users (id),
    chat_group_id BIGINT NOT NULL REFERENCES chat_groups (id),
    points        INT    NOT NULL DEFAULT 0,
    CONSTRAINT uq_user_group UNIQUE (user_id, chat_group_id)
);

CREATE INDEX idx_ugp_group ON user_group_points (chat_group_id, points DESC);
