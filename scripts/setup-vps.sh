#!/bin/bash
# Запустить один раз на чистом VPS: bash setup-vps.sh
set -e

echo "🚀 Setting up VPS for TS WC Scores..."

# Docker
if ! command -v docker &> /dev/null; then
    echo "📦 Installing Docker..."
    curl -fsSL https://get.docker.com | sh
    sudo usermod -aG docker $USER
    echo "⚠️  Log out and back in, then re-run this script"
    exit 0
fi

# Docker Compose plugin
if ! docker compose version &> /dev/null; then
    sudo apt-get update && sudo apt-get install -y docker-compose-plugin
fi

command -v make  &> /dev/null || apt-get install -y make
command -v rsync &> /dev/null || apt-get install -y rsync

mkdir -p ~/ts-wc-scores/data/postgres
sudo chown -R 999:999 ~/ts-wc-scores/data/postgres

if [ ! -f ~/ts-wc-scores/.env ]; then
    cat > ~/ts-wc-scores/.env << 'ENVEOF'
TELEGRAM_BOT_TOKEN=your_bot_token_here
TELEGRAM_BOT_USERNAME=your_bot_username_here
TELEGRAM_ADMIN_CHAT_ID=0
FOOTBALL_DATA_TOKEN=your_football_data_token_here
ENVEOF
fi

echo ""
echo "✅ VPS ready! Next steps:"
echo "  1. Fill tokens:  nano ~/ts-wc-scores/.env"
echo "  2. Deploy:       make deploy HOST=user@$(curl -s ifconfig.me 2>/dev/null || echo 'your-vps-ip')"
