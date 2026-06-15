#!/bin/bash
# Запустить один раз на чистом VPS: bash setup-vps.sh
set -e

echo "🚀 Setting up VPS for TS WC Scores 2026..."

# Java 17 — доступна в стандартном репозитории Debian 11
if ! java -version 2>&1 | grep -qE "17|21"; then
    echo "📦 Installing Java 17..."
    apt-get clean && rm -rf /var/lib/apt/lists/*
    apt-get update -q
    apt-get install -y openjdk-17-jre-headless
fi

java -version
echo "✅ Java OK"

# Docker
if ! command -v docker &> /dev/null; then
    echo "📦 Installing Docker..."
    curl -fsSL https://get.docker.com | sh
fi

# rsync, make
apt-get install -y rsync make 2>/dev/null || true

# Папка проекта
mkdir -p ~/ts-wc-scores/data/postgres ~/ts-wc-scores/scripts
chown -R 999:999 ~/ts-wc-scores/data/postgres

# .env если не существует
if [ ! -f ~/ts-wc-scores/.env ]; then
    cat > ~/ts-wc-scores/.env << 'ENVEOF'
TELEGRAM_BOT_TOKEN=your_bot_token_here
TELEGRAM_BOT_USERNAME=your_bot_username_here
TELEGRAM_ADMIN_CHAT_ID=0
FOOTBALL_DATA_TOKEN=your_football_data_token_here
ENVEOF
    echo "⚠️  Заполни токены: nano ~/ts-wc-scores/.env"
fi

echo ""
echo "✅ VPS готов!"
echo "  1. Заполни токены:  nano ~/ts-wc-scores/.env"
echo "  2. Задеплой:        make deploy HOST=root@$(curl -s ifconfig.me 2>/dev/null || echo 'your-ip')"
