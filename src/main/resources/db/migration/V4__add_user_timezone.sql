-- Добавляем колонку timezone в users
ALTER TABLE users ADD COLUMN timezone VARCHAR(50) NOT NULL DEFAULT 'Europe/Moscow';
