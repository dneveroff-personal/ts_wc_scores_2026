# TS WC Scores 2026 ⚽🏆

Telegram-бот для прогнозов на матчи Чемпионата мира по футболу 2026.
Участники группы делают ставки на счёт, получают очки и соревнуются в таблице лидеров.

## Стек

| Технология | Назначение |
|---|---|
| Java 21 + Kotlin, Spring Boot 3.3 | Основа приложения |
| Spring Data JPA + H2 | Хранение данных (встроенная БД, файловая) |
| Spring Scheduler | Загрузка расписания по крону |
| Spring WebClient | Запросы к Football-Data API |
| OkHttp (свой Telegram клиент) | Бот без сторонних библиотек |
| Flyway | Миграции БД |
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
    H2 (файловая БД, ./data/h2)
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

H2 Console доступна по адресу: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/h2/wc_scores`
- User: `sa`
- Password: (пустой)

### Деплой на VPS

```bash
# Один раз — настройка сервера (устанавливает Java, rsync)
scp scripts/setup-vps.sh root@YOUR_IP:~/
ssh root@YOUR_IP "bash setup-vps.sh"
ssh root@YOUR_IP "nano ~/ts-wc-scores/.env"   # заполнить токены

# Деплой (после каждого изменения)
make deploy HOST=root@YOUR_IP

# Логи на сервере
make logs-vps HOST=root@YOUR_IP
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
- Данные H2 хранятся в `./data/h2/` — копируй эту папку при переносе на новый сервер
- Для production на VPS H2 работает в file-based режиме с `AUTO_SERVER=TRUE`, что позволяет нескольким процессам подключаться к одной БД

## ОЧИСТКА
ssh root@89.125.248.168 "
# Очищаем syslog и daemon.log (они пересоздадутся автоматически)
> /var/log/syslog
> /var/log/daemon.log
> /var/log/kern.log
> /var/log/messages

# Journald
journalctl --vacuum-size=50M

df -h /
"

Надо еще вот что сделать:
- надо нам пользователям выставить временную зону, чтобы отображать время начала матчей правильно и все остальные рассчеты чтобы правильно были, например нотификейшен за час до матча, если не сделан прогноз еще
- не работает сейчас нотификейшен юзеру в личку за час до игры, если он не сделал прогноз
- надо еще одну команду "Мои очки", где будет максимально сжато все матчи за которые ты получил очки