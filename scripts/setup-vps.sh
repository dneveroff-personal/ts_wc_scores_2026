#!/bin/bash
# Скрипт первоначальной настройки VPS для автоматического деплоя
# Запуск: bash setup-vps.sh
set -e

echo "🚀 Setting up VPS for auto-deploy..."

# 1. Установка Docker (если не установлен)
if ! command -v docker &> /dev/null; then
    echo "📦 Installing Docker..."
    apt-get update
    apt-get install -y ca-certificates curl gnupg lsb-release
    mkdir -p /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
    apt-get update
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
    systemctl enable --now docker
else
    echo "✅ Docker already installed"
fi

# 2. Установка Docker Compose plugin (если не установлен)
if ! docker compose version &> /dev/null; then
    echo "📦 Installing Docker Compose plugin..."
    apt-get update
    apt-get install -y docker-compose-plugin
else
    echo "✅ Docker Compose already installed"
fi

# 3. Создание директории проекта
echo "📁 Creating project directory..."
mkdir -p ~/ts-wc-scores
mkdir -p ~/ts-wc-scores/data/postgres
chown -R 999:999 ~/ts-wc-scores/data/postgres

# 4. Копирование docker-compose.prod.yml (если скрипт запущен из репозитория)
if [ -f "$(dirname "$0")/../docker-compose.prod.yml" ]; then
    echo "📋 Copying docker-compose.prod.yml..."
    cp "$(dirname "$0")/../docker-compose.prod.yml" ~/ts-wc-scores/docker-compose.prod.yml
fi

# 5. Создание .env файла (если его нет)
if [ ! -f ~/ts-wc-scores/.env ]; then
    echo "⚙️  Creating .env file..."
    cat > ~/ts-wc-scores/.env << 'ENVEOF'
# Telegram Bot Configuration
TELEGRAM_BOT_TOKEN=your_bot_token_here
TELEGRAM_BOT_USERNAME=your_bot_username_here
FOOTBALL_DATA_TOKEN=your_football_data_api_key_here
TELEGRAM_ADMIN_CHAT_ID=
ENVEOF
    echo "⚠️  Please edit ~/ts-wc-scores/.env with your actual values!"
fi

# 6. Настройка firewall (если используется ufw)
if command -v ufw &> /dev/null; then
    echo "🔥 Configuring firewall..."
    ufw allow 22/tcp || true
    ufw allow 8080/tcp || true
    ufw --force enable || true
fi

# 7. Логин в GHCR (для pull образов)
echo "🔑 Configuring GHCR credentials..."
# Создаем файл с credentials для GHCR
# Пользователь должен будет добавить свой токен
mkdir -p ~/.docker
cat > ~/.docker/config.json << 'DOCKEREOF'
{
  "auths": {
    "ghcr.io": {
      "auth": "BASE64_ENCODED_CREDENTIALS"
    }
  }
}
DOCKEREOF
echo "⚠️  Please update ~/.docker/config.json with your GHCR credentials!"
echo "   Run: echo '{\"auths\":{\"ghcr.io\":{\"auth\":\"'$(echo -n 'USERNAME:TOKEN' | base64)'\"}}}' > ~/.docker/config.json"

# 8. Первый деплой (если есть credentials)
echo ""
echo "✅ VPS setup complete!"
echo ""
echo "Next steps:"
echo "1. Edit ~/ts-wc-scores/.env with your bot token and other configs"
echo "2. Configure GHCR credentials in ~/.docker/config.json"
echo "3. Run initial deploy:"
echo "   cd ~/ts-wc-scores && docker compose -f docker-compose.prod.yml pull && docker compose -f docker-compose.prod.yml up -d"
echo ""
echo "After that, every push to main branch will trigger auto-deploy! 🎉"
