#!/bin/bash
# Запустить один раз на чистом VPS: bash setup-vps.sh
set -e

echo "🚀 Setting up VPS for TS WC Scores 2026..."

# Чистим место перед установкой
apt-get clean
rm -rf /var/lib/apt/lists/*
journalctl --vacuum-size=10M 2>/dev/null || true

# Java 17
if ! java -version 2>&1 | grep -qE "17|21"; then
    echo "📦 Installing Java 17..."
    apt-get update -q
    apt-get install -y --no-install-recommends openjdk-17-jre-headless
fi
echo "✅ $(java -version 2>&1 | head -1)"

# PostgreSQL напрямую на хост (без Docker — экономит ~150MB)
if ! command -v psql &> /dev/null; then
    echo "📦 Installing PostgreSQL..."
    apt-get install -y --no-install-recommends postgresql
fi

# Создаём БД и пользователя если не существуют
echo "🗄️ Setting up database..."
su - postgres -c "psql -tc \"SELECT 1 FROM pg_user WHERE usename='wc_user'\"" | grep -q 1 || \
    su - postgres -c "psql -c \"CREATE USER wc_user WITH PASSWORD 'wc_pass';\""
su - postgres -c "psql -tc \"SELECT 1 FROM pg_database WHERE datname='wc_scores'\"" | grep -q 1 || \
    su - postgres -c "psql -c \"CREATE DATABASE wc_scores OWNER wc_user;\""
echo "✅ Database ready"

# rsync, make
apt-get install -y --no-install-recommends rsync make 2>/dev/null || true

# Папка проекта
mkdir -p ~/ts-wc-scores/scripts ~/ts-wc-scores/data

# .env если не существует
if [ ! -f ~/ts-wc-scores/.env ]; then
    cat > ~/ts-wc-scores/.env << 'ENVEOF'
TELEGRAM_BOT_TOKEN=8881344242:AAGBvNXTGN7JPRkMqlVNdY4z4d-XQ-gr8v0
TELEGRAM_BOT_USERNAME=dn_football_bot
TELEGRAM_ADMIN_CHAT_ID=397032148
FOOTBALL_DATA_TOKEN=564592df81d443f8af04923a69759de6
DB_URL=jdbc:postgresql://localhost:5432/wc_scores
DB_USER=wc_user
DB_PASSWORD=wc_pass
ENVEOF
    echo "⚠️  Заполни токены: nano ~/ts-wc-scores/.env"
fi

echo ""
echo "✅ VPS готов! Свободное место:"
df -h /
echo ""
echo "  1. Заполни токены:  nano ~/ts-wc-scores/.env"
echo "  2. Задеплой:        make deploy HOST=root@$(curl -s ifconfig.me 2>/dev/null || echo 'your-ip')"
