# TS WC Scores 2026 ⚽

Telegram-бот для прогнозов на матчи Чемпионата мира по футболу 2026.
Работает в групповом чате и в личке.

## Стек технологий

- Java 21 + Kotlin, Spring Boot 3.3
- Gradle 9.4.1 (Kotlin DSL)
- Spring Data JPA + PostgreSQL
- Spring Scheduler (cron jobs)
- Spring WebClient (Football-Data API)
- Telegram Bots API (Java)
- Flyway (SQL-миграции БД)
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

# Скопировать .env и заполнить токенами
cp .env.example .env
# Заполни .env своими токенами

# config/application.yml уже содержит placeholder'ы и подхватывает значения из .env
# При необходимости можно переопределить конкретные значения в config/application.yml
```

### 2. Запуск через Docker Compose

```bash
# Docker использует config/application.yml (который подхватывает переменные из .env)
docker-compose up -d
```

### 3. Запуск локально (для разработки)

```bash
# Запустить только БД
docker-compose up -d postgres

# Запустить приложение (использует config/application.yml)
make run-local
# или вручную:
./gradlew bootRun -Dspring.config.additional-location=file:config/application.yml
```

### 4. Тесты

```bash
./gradlew test
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
Синхронизация каждые 30 минут, подсчёт очков — каждые 30 минут.

> 📌 Код соревнования `WC` может измениться после объявления ЧМ 2026.
> Проверь актуальный код на [football-data.org](https://www.football-data.org/documentation/quickstart).


ДА!
Данные получены!
Но несколько моментов давай исправим.

1. Папка data/postgres - пуста, каким образом БД там должна оказаться? я кстати добавил папку data в .dockerignore, т.к. возникала ошибка при билде  => ERROR [internal] load build context                                                                                                                      0.0s
   => => transferring context: 196B                                                                                                                            0.0s
------
> [internal] load build context:
------
[+] up 0/1
⠙ Image ts-wc-scores-app Building                                                                                                                            1.0s
failed to solve: error from sender: open /home/dneveroff/sites/java/ts-wc-scores/data/postgres/base: permission denied


1. по команде /matches отображаются tla команд, давай изменим на их short_name
2. Мы можем как-то еще добавить, чтобы в телеграмм отображались флаги этих стран?
3. Сейчас когда нажимаешь на кнопку матча, выскакивает сообщение: Введи счёт для матча #1:  `/predict 1 2 1`  Замени 2 1 на свой прогноз и отправь. - Давай сделаем красивее. Давай подумаем как? Но в то же время не засорять групповой чат. Может ли при нажатии на кнопку бот вставлять в поле для ввода уже часть команды напирмер вставлять "/predict "?
4. и давай отмечать текстом кнопки, на которые матчи уже сделан прогноз, и писать на них уже счет. Но иметь возможность изменить prediction свой.

5. надо еще добавить функционал, что у нас идет рейтинг между пользователями одной группы. Но если бот добавлен в несколько групп, то сделать еще общий рейтинг отдельно, где будут все юзеры.
6. Давай подумаем над системой очков, справедливая ли она или может откорректировать? Ведь точный счет очень сложно угадать, чем победу просто.