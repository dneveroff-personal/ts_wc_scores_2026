# TS WC Scores 2026 ⚽🏆
[![Build](https://github.com/dneveroff-personal/ts_wc_scores_2026/actions/workflows/build.yml/badge.svg)](https://github.com/dneveroff-personal/ts_wc_scores_2026/actions/workflows/build.yml)

Telegram-бот для прогнозов на матчи Чемпионата мира по футболу 2026.
Участники группы делают ставки на счёт, получают очки и соревнуются в таблице лидеров.

## Стек

### Язык и фреймворк
![Java](https://img.shields.io/badge/Java-17-blue?logo=openjdk)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-orange?logo=kotlin)
![Spring%20Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen?logo=spring)

### База данных и миграции
![Spring%20Data%20JPA](https://img.shields.io/badge/Spring%20Data%20JPA-3.3-green?logo=spring)
![Flyway](https://img.shields.io/badge/Flyway-10.x-green?logo=flyway)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)

### Интеграции
![WebClient](https://img.shields.io/badge/Spring%20WebClient-3.3-brightgreen?logo=spring)
![OkHttp](https://img.shields.io/badge/OkHttp-4.12-green?logo=square)
![Telegram%20Bot%20API](https://img.shields.io/badge/Telegram%20Bot%20API-blue?logo=telegram)

### Тестирование и качество
![JUnit%205](https://img.shields.io/badge/JUnit%205-5.10-red?logo=junit)
![Mockito](https://img.shields.io/badge/Mockito-5.x-green?logo=mockito)
![Jacoco](https://img.shields.io/badge/Jacoco-0.8.13-green?logo=codecov)

## Команды бота

| Команда | Описание |
|---|---|
| `/register` | Зарегистрироваться в игре |
| `/matches` | Матчи ближайших 24 часов |
| `/predict {id} {гол1} {гол2}` | Сделать прогноз (`/predict 42 2 1`) |
| `/mypredictions` | Мои прогнозы и очки |
| `/leaderboard` | Таблица лидеров группы |
| `/leaderboard global` | Общий рейтинг всех участников |
| `/sync` | Принудительная синхронизация матчей |
| `/calcscore` | Принудительный подсчёт очков |
| `/help` | Справка |

Кнопки в `/matches` вставляют `/predict {id} ` в поле ввода — пользователь добавляет только счёт.
Бот автоматически напоминает за час до матча тем, кто не сделал прогноз.

## Система очков

| Результат | Очки |
|---|---|
| ⭐ Точный счёт | 5 |
| ✅ Правильный исход (победитель / ничья) | 2 |
| 🎯 + верная разница голов | +1 |

## Архитектура

```
football-data.org API
        │
        ▼ (каждые 2 часа)
┌─────────────────────────────────────────┐
│           Spring Boot App               │
│                                         │
│  FootballDataService → MatchRepository  │
│  ScoringService      → PredictionRepo   │
│  GroupService        → UserRepository   │
│  AppScheduler (cron)                    │
│                                         │
│  TelegramUpdatePoller ←──────────────── │◄── Telegram API
│  TelegramBotClient   ──────────────────►│──► Telegram API
│  WcPredictBot (логика команд)           │
└─────────────────────────────────────────┘
        │
        ▼
    POstgreSQL
```

## Быстрый старт

### Требования
- Java 17+
- Токен Telegram бота ([@BotFather](https://t.me/BotFather))
- API ключ [football-data.org](https://www.football-data.org/client/register) (бесплатный)

### Локальный запуск

```bash
# 1. Клонировать и настроить
cp .env.example .env
nano .env   # заполнить токены

# 2. Инициализировать папку БД
make init-data

# 3. Запустить приложение
make run
```


### Деплой на VPS

```bash
# Один раз — настройка сервера (устанавливает Java, rsync)
scp scripts/setup-vps.sh root@HOST_IP:~/
ssh root@HOST_IP "bash setup-vps.sh"
ssh root@HOST_IP "nano ~/ts-wc-scores/.env"   # заполнить токены

# Деплой (после каждого изменения)
make deploy HOST=root@HOST_IP

# Логи на сервере
make logs-vps HOST=root@HOST_IP

## Перенос БД
# Dump on vps
ssh root@HOST_IP "docker exec wc_scores_db pg_dump -U wc_user -d wc_scores" > backup_from_vps.sql

# Pull on local
docker exec -i wc_scores_db psql -U wc_user -d postgres -c "DROP DATABASE IF EXISTS wc_scores;"
docker exec -i wc_scores_db psql -U wc_user -d postgres -c "CREATE DATABASE wc_scores OWNER wc_user;"
docker exec -i wc_scores_db psql -U wc_user -d wc_scores < backup_from_vps.sql

# Make local dump 
docker exec wc_scores_db pg_dump -U wc_user -d wc_scores > backup_local_fixed.sql

# Pull on vps
cat backup_local_fixed.sql | ssh root@HOST_IP "cd ~/ts-wc-scores/ && docker compose stop app && docker exec -i wc_scores_db psql -U wc_user -d postgres -c \"SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='wc_scores' AND pid<>pg_backend_pid();\" && docker exec -i wc_scores_db psql -U wc_user -d postgres -c \"DROP DATABASE IF EXISTS wc_scores;\" && docker exec -i wc_scores_db psql -U wc_user -d postgres -c \"CREATE DATABASE wc_scores OWNER wc_user;\" && docker exec -i wc_scores_db psql -U wc_user -d wc_scores && docker compose start app"
cat backup_local_fixed.sql | ssh root@HOST_IP "cd ~/ts-wc-scores/ && docker exec -i wc_scores_db psql -U wc_user -d wc_scores"
```

## Переменные окружения

```env
TELEGRAM_BOT_TOKEN=        # токен от @BotFather
TELEGRAM_BOT_USERNAME=     # username бота без @
TELEGRAM_ADMIN_CHAT_ID=    # твой Telegram ID (уведомления о старте/стопе)
FOOTBALL_DATA_TOKEN=       # ключ от football-data.org
```

## Структура проекта

```
src/main/java/com/tswcscores/
├── bot/
│   ├── WcPredictBot.java              ← логика всех команд
│   ├── handler/BotMessageBuilder.java ← форматирование сообщений
│   └── keyboard/InlineKeyboardFactory.java
├── config/
│   ├── AppConfig.java                 ← WebClient бин
│   ├── ScoringProperties.java         ← настройки очков
│   └── TelegramBotConfig.java         ← регистрация команд, уведомления
├── entity/        User, Match, Team, Prediction, ChatGroup, UserGroupPoints
├── repository/    JPA репозитории
├── service/impl/
│   ├── FootballDataService.java       ← синхронизация с API
│   ├── ScoringServiceImpl.java        ← алгоритм подсчёта очков
│   ├── PredictionService.java         ← приём прогнозов
│   ├── UserService.java
│   └── GroupService.java              ← групповой рейтинг
├── scheduler/AppScheduler.java        ← все cron задачи
└── telegram/
    ├── TelegramBotClient.java         ← HTTP клиент (OkHttp, без рекламы)
    ├── TelegramUpdatePoller.java      ← long polling
    └── TelegramUpdate.java            ← DTO апдейтов

scripts/
├── ts-wc-scores.service   ← systemd сервис для VPS
└── setup-vps.sh           ← первоначальная настройка сервера
```

CI/CD
git push
│
▼
GitHub Actions
│
▼
Build + Test
│
▼
Docker Build
│
▼
Push Image to GHCR
│
┌───────┴────────┐
│                │
▼                ▼
Manual Deploy     Auto Deploy
make deploy       SSH Workflow

На сервере:
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
