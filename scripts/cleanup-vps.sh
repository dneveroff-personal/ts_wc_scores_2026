#!/bin/bash
# Очистка VPS от всего лишнего перед переходом на Docker
# Запуск: bash cleanup-vps.sh
set -e

echo "🧹 Cleaning up VPS..."

# 1. Останавливаем и удаляем systemd сервис приложения
echo "Removing systemd service..."
systemctl stop ts-wc-scores 2>/dev/null || true
systemctl disable ts-wc-scores 2>/dev/null || true
rm -f /etc/systemd/system/ts-wc-scores.service
systemctl daemon-reload

# 2. Удаляем PostgreSQL с хоста (данные сохраним в дамп перед этим!)
echo "⚠️  Removing host PostgreSQL..."
systemctl stop postgresql 2>/dev/null || true
apt-get remove -y postgresql postgresql-* 2>/dev/null || true
apt-get autoremove -y 2>/dev/null || true
# Данные PostgreSQL НЕ удаляем — они в /var/lib/postgresql
# Если нужно перенести — сделай дамп ДО запуска этого скрипта

# 3. Чистим логи которые забили диск
echo "Cleaning logs..."
> /var/log/syslog
> /var/log/daemon.log
> /var/log/kern.log
> /var/log/messages 2>/dev/null || true
journalctl --vacuum-size=50M

# 4. Ограничиваем размер логов на будущее
mkdir -p /etc/systemd/journald.conf.d
cat > /etc/systemd/journald.conf.d/size.conf << 'JEOF'
[Journal]
SystemMaxUse=100M
SystemMaxFileSize=20M
MaxRetentionSec=7day
JEOF
systemctl restart systemd-journald

# 5. Чистим Docker мусор
echo "Cleaning Docker..."
docker system prune -af 2>/dev/null || true

# 6. Чистим apt кэш
apt-get clean
rm -rf /var/lib/apt/lists/*

# 7. Готовим папку для данных БД
mkdir -p ~/ts-wc-scores/data/postgres
chown -R 999:999 ~/ts-wc-scores/data/postgres

echo ""
echo "✅ Cleanup done!"
df -h /
