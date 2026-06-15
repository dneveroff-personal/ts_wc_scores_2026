.PHONY: build deploy db-push db-pull db-backup init-data logs status restart

GREEN  := \033[0;32m
YELLOW := \033[0;33m
RESET  := \033[0m

# =========================================================
# Локальная разработка
# =========================================================

build:
	@echo "$(GREEN)Building...$(RESET)"
	./gradlew clean build -x test
	@echo "✅ Build complete"

# Локальный запуск (PostgreSQL через docker-compose, приложение через gradle)
up:
	docker compose up -d
	@echo "✅ PostgreSQL started. Run: ./gradlew bootRun"

down:
	docker compose down --remove-orphans

logs:
	docker compose logs -f postgres

status:
	@docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# =========================================================
# Деплой на VPS
# Использование: make deploy HOST=root@89.125.248.168
# =========================================================

deploy: build
	@if [ -z "$(HOST)" ]; then echo "❌ Usage: make deploy HOST=root@IP"; exit 1; fi
	$(eval JAR := $(shell ls build/libs/ts-wc-scores-*.jar | grep -v plain | head -1))
	@if [ -z "$(JAR)" ]; then echo "❌ JAR not found"; exit 1; fi
	$(eval VERSION := $(shell echo $(JAR) | grep -oP '\d+\.\d+\.\d+'))
	@echo "📦 Deploying v$(VERSION) to $(HOST)..."
	ssh $(HOST) "mkdir -p ~/ts-wc-scores/scripts"
	rsync -avz $(JAR) $(HOST):~/ts-wc-scores/app.jar
	rsync -avz docker-compose.yml scripts/ $(HOST):~/ts-wc-scores/scripts/
	rsync -avz scripts/setup-vps.sh $(HOST):~/ts-wc-scores/
	# Устанавливаем systemd сервис
	ssh $(HOST) "cp ~/ts-wc-scores/scripts/ts-wc-scores.service /etc/systemd/system/ \
		&& systemctl daemon-reload \
		&& systemctl enable ts-wc-scores"
	# Убеждаемся что PostgreSQL запущен
	ssh $(HOST) "cd ~/ts-wc-scores && docker compose up -d"
	# Перезапускаем приложение
	ssh $(HOST) "systemctl restart ts-wc-scores"
	@echo "✅ v$(VERSION) deployed! Logs: make logs-vps HOST=$(HOST)"

# Логи приложения на VPS
logs-vps:
	@if [ -z "$(HOST)" ]; then echo "❌ Usage: make logs-vps HOST=root@IP"; exit 1; fi
	ssh $(HOST) "journalctl -u ts-wc-scores -f"

# Статус на VPS
status-vps:
	@if [ -z "$(HOST)" ]; then echo "❌ Usage: make status-vps HOST=root@IP"; exit 1; fi
	ssh $(HOST) "systemctl status ts-wc-scores && docker ps"

# =========================================================
# Синхронизация БД
# =========================================================

# Забрать БД с VPS
db-pull:
	@if [ -z "$(HOST)" ]; then echo "❌ Usage: make db-pull HOST=root@IP"; exit 1; fi
	@echo "⚠️  Перезапишет локальную БД. Продолжить? [y/N]" && read ans && [ "$$ans" = "y" ]
	@echo "📦 Дамп с сервера..."
	ssh $(HOST) "cd ~/ts-wc-scores \
		&& docker compose exec -T postgres pg_dump -U wc_user --clean --if-exists --no-owner -Fc wc_scores" \
		> /tmp/wc_scores_dump.dump
	@echo "📥 Восстанавливаем локально..."
	docker compose exec -T postgres pg_restore -U wc_user -d wc_scores \
		--clean --if-exists --no-owner -Fc /tmp/wc_scores_dump.dump || true
	@echo "✅ Готово"

# Отправить локальную БД на VPS
db-push:
	@if [ -z "$(HOST)" ]; then echo "❌ Usage: make db-push HOST=root@IP"; exit 1; fi
	@echo "⚠️  Перезапишет БД на сервере. Продолжить? [y/N]" && read ans && [ "$$ans" = "y" ]
	@echo "📦 Дамп локальной БД..."
	docker compose exec -T postgres pg_dump -U wc_user --clean --if-exists --no-owner -Fc wc_scores \
		> /tmp/wc_scores_dump.dump
	@echo "🚀 Отправляем..."
	scp /tmp/wc_scores_dump.dump $(HOST):/tmp/wc_scores_dump.dump
	ssh $(HOST) "cd ~/ts-wc-scores \
		&& docker compose exec -T postgres pg_restore -U wc_user -d wc_scores \
		--clean --if-exists --no-owner -Fc /tmp/wc_scores_dump.dump || true"
	@echo "✅ Готово"

# Локальный бэкап с датой
db-backup:
	@mkdir -p backups
	docker compose exec -T postgres pg_dump -U wc_user --no-owner -Fc wc_scores \
		> backups/wc_scores_$$(date +%Y%m%d_%H%M%S).dump
	@echo "✅ Backup saved to backups/"

# =========================================================
# Первоначальная настройка
# =========================================================

# Создать папку данных с правильными правами (один раз)
init-data:
	mkdir -p data/postgres
	sudo chown -R 999:999 data/postgres
	@echo "✅ data/postgres ready"
