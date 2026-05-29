# TS WC Scores 2026 ⚽

Telegram-бот для прогнозов на матчи Чемпионата мира по футболу 2026.
Работает в групповом чате и в личке.

## Стек технологий

- Java 21, Spring Boot 3.3
- Spring Data JPA + PostgreSQL
- Spring Scheduler (cron jobs)
- Spring WebClient (Football-Data API)
- Telegram Bots API (Java)
- Liquibase (миграции БД)
- Docker / Docker Compose
- JUnit 5 + Mockito (тесты)

## Команды бота

| Команда | Описание |
|---------|----------|
| `/register` | Зарегистрироваться в игре |
| `/matches` | Матчи ближайших 24 часов с кнопками прогноза |
| `/predict {id} {гол1} {гол2}` | Сделать прогноз (`/predict 42 2 1`) |
| `/mypredictions` | Мои прогнозы и очки |
| `/leaderboard` | Таблица лидеров |

## Система очков

| Результат | Очки |
|-----------|------|
| Точный счёт | 4 |
| Правильный исход | 2 |
| + верная разница голов | +1 |

## Быстрый старт

### 1. Подготовка

```bash
# Создай бота в @BotFather и получи токен
# Получи API ключ на https://www.football-data.org/client/register

cp .env.example .env
# Заполни .env своими токенами
```

### 2. Запуск через Docker Compose

```bash
docker-compose up -d
```

### 3. Запуск локально (для разработки)

```bash
# Запустить только БД
docker-compose up -d postgres

# Запустить приложение
./mvnw spring-boot:run \
  -DTELEGRAM_BOT_TOKEN=... \
  -DTELEGRAM_BOT_USERNAME=... \
  -DFOOT BALL_DATA_TOKEN=...
```

### 4. Тесты

```bash
./mvnw test
```

## Структура проекта

```
src/main/java/com/tswcscores/
├── TsWcScoresApplication.java
├── bot/
│   ├── WcPredictBot.java          ← главный класс бота
│   ├── handler/
│   │   └── BotMessageBuilder.java ← форматирование сообщений
│   └── keyboard/
│       └── InlineKeyboardFactory.java ← кнопки
├── config/
│   ├── AppConfig.java             ← WebClient bean
│   └── ScoringProperties.java     ← настройки очков
├── entity/
│   ├── User.java
│   ├── Match.java
│   ├── Team.java
│   └── Prediction.java
├── repository/
│   ├── UserRepository.java
│   ├── MatchRepository.java
│   ├── TeamRepository.java
│   └── PredictionRepository.java
├── service/
│   ├── ScoringService.java        ← интерфейс
│   └── impl/
│       ├── ScoringServiceImpl.java  ← алгоритм подсчёта
│       ├── FootballDataService.java ← синхронизация с API
│       ├── PredictionService.java   ← прогнозы
│       └── UserService.java
├── scheduler/
│   └── AppScheduler.java          ← все cron jobs
├── dto/
│   ├── FootballDataMatchesResponse.java
│   └── ScoringResult.java
└── exception/
    ├── DeadlinePassedException.java
    └── MatchNotFoundException.java
```

## Добавить бота в группу

1. Добавь бота в Telegram-группу
2. Дай боту права на чтение сообщений (отключи режим Privacy Mode в @BotFather)
3. Участники делают `/register` в личке, затем `/predict` через кнопки из `/matches`

## API Football-Data

Используется эндпоинт `/v4/competitions/WC/matches`.
Синхронизация каждые 30 минут, подсчёт очков — каждые 5 минут.

> 📌 Код соревнования `WC` может измениться после объявления ЧМ 2026.
> Проверь актуальный код на [football-data.org](https://www.football-data.org/documentation/quickstart).
