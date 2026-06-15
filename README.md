# TS WC Scores 2026 ⚽🏆

Telegram-бот для прогнозов на матчи Чемпионата мира по футболу 2026.
Участники группы делают ставки на счёт, получают очки и соревнуются в таблице лидеров.

## Стек

| Технология | Назначение |
|---|---|
| Java 21 + Kotlin, Spring Boot 3.3 | Основа приложения |
| Spring Data JPA + PostgreSQL | Хранение данных |
| Spring Scheduler | Загрузка расписания по крону |
| Spring WebClient | Запросы к Football-Data API |
| OkHttp (свой Telegram клиент) | Бот без сторонних библиотек |
| Flyway | Миграции БД |
| Docker Compose | Запуск PostgreSQL |
| JUnit 5 + Mockito | Тесты логики подсчёта очков |

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
   PostgreSQL (Docker)
```

## Быстрый старт

### Требования
- Java 21+
- Docker + Docker Compose
- Токен Telegram бота ([@BotFather](https://t.me/BotFather))
- API ключ [football-data.org](https://www.football-data.org/client/register) (бесплатный)

### Локальный запуск

```bash
# 1. Клонировать и настроить
cp .env.example .env
nano .env   # заполнить токены

# 2. Запустить PostgreSQL
make up

# 3. Запустить приложение
./gradlew bootRun
```

### Деплой на VPS

```bash
# Один раз — настройка сервера (устанавливает Java, Docker, rsync)
scp scripts/setup-vps.sh root@YOUR_IP:~/
ssh root@YOUR_IP "bash setup-vps.sh"
ssh root@YOUR_IP "nano ~/ts-wc-scores/.env"   # заполнить токены

# Деплой (после каждого изменения)
make deploy HOST=root@YOUR_IP

# Логи на сервере
make logs-vps HOST=root@YOUR_IP
```

### Синхронизация БД

```bash
make db-pull HOST=root@YOUR_IP   # забрать БД с сервера
make db-push HOST=root@YOUR_IP   # отправить локальную на сервер
make db-backup                    # локальный бэкап в папку backups/
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

## Заметки

- **Inline Mode** в BotFather должен быть включён для кнопок с автозаполнением `/predict`
- Код соревнования `WC` в `application.yml` — проверь актуальность на [football-data.org](https://www.football-data.org/documentation/quickstart)
- Данные PostgreSQL хранятся в `./data/postgres` — копируй эту папку при переносе на новый сервер
