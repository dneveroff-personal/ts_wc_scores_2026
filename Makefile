.PHONY: build deploy logs-vps status-vps db-pull db-push db-backup

GREEN  := \033[0;32m
RESET  := \033[0m

# =========================================================
# Локальная разработка
# =========================================================

build:
	@echo "$(GREEN)Building...$(RESET)"
	./gradlew clean build -x test
	@echo "✅ Build complete"

run:
	./gradlew bootRun

# =========================================================
# Деплой на VPS — make deploy HOST=root@IP
# =========================================================

deploy: build
	@if [ -z "$(HOST)" ]; then echo "❌ Usage: make deploy HOST=root@IP"; exit 1; fi
	$(eval JAR := $(shell ls build/libs/ts-wc-scores-*.jar | grep -v plain | head -1))
	@if [ -z "$(JAR)" ]; then echo "❌ JAR not found in build/libs/"; exit 1; fi
	$(eval VERSION := $(shell echo $(JAR) | grep -oP '\d+\.\d+\.\d+'))
	@echo "📦 Deploying v$(VERSION) to $(HOST)..."
	ssh $(HOST) "mkdir -p ~/ts-wc-scores/scripts"
	rsync -avz --checksum $(JAR) $(HOST):~/ts-wc-scores/app.jar
	rsync -avz scripts/ts-wc-scores.service scripts/setup-vps.sh $(HOST):~/ts-wc-scores/scripts/
	ssh $(HOST) " \
		cp ~/ts-wc-scores/scripts/ts-wc-scores.service /etc/systemd/system/ && \
		systemctl daemon-reload && \
		systemctl enable ts-wc-scores && \
		systemctl restart ts-wc-scores"
	@echo "✅ v$(VERSION) deployed!"
	@echo "   Logs: make logs-vps HOST=$(HOST)"

logs-vps:
	@if [ -z "$(HOST)" ]; then echo "❌ Usage: make logs-vps HOST=root@IP"; exit 1; fi
	ssh $(HOST) "journalctl -u ts-wc-scores -f"

status-vps:
	@if [ -z "$(HOST)" ]; then echo "❌ Usage: make status-vps HOST=root@IP"; exit 1; fi
	ssh $(HOST) "systemctl status ts-wc-scores && df -h /"

# =========================================================
# Синхронизация БД (PostgreSQL на хосте)
# =========================================================

# Забрать БД с VPS на локальную машину
db-pull:
	@if [ -z "$(HOST)" ]; then echo "❌ Usage: make db-pull HOST=root@IP"; exit 1; fi
	@echo "⚠️  Перезапишет локальную БД. Продолжить? [y/N]" && read ans && [ "$$ans" = "y" ]
	@echo "📦 Дамп с сервера..."
	ssh $(HOST) "pg_dump -U wc_user -d wc_scores --clean --if-exists --no-owner -Fc" \
		> /tmp/wc_scores_dump.dump
	@echo "📥 Восстанавливаем локально..."
	docker compose exec -T postgres pg_restore -U wc_user -d wc_scores \
		--clean --if-exists --no-owner /tmp/wc_scores_dump.dump || true
	@echo "✅ Готово"

# Отправить локальную БД на VPS
db-push:
	@if [ -z "$(HOST)" ]; then echo "❌ Usage: make db-push HOST=root@IP"; exit 1; fi
	@echo "⚠️  Перезапишет БД на сервере. Продолжить? [y/N]" && read ans && [ "$$ans" = "y" ]
	@echo "📦 Дамп локальной БД..."
	docker compose exec -T postgres pg_dump -U wc_user \
		--clean --if-exists --no-owner -Fc wc_scores > /tmp/wc_scores_dump.dump
	@echo "🚀 Отправляем и восстанавливаем..."
	scp /tmp/wc_scores_dump.dump $(HOST):/tmp/wc_scores_dump.dump
	ssh $(HOST) "pg_restore -U wc_user -d wc_scores \
		--clean --if-exists --no-owner /tmp/wc_scores_dump.dump || true"
	@echo "✅ Готово"

# Локальный бэкап
db-backup:
	@mkdir -p backups
	docker compose exec -T postgres pg_dump -U wc_user --no-owner -Fc wc_scores \
		> backups/wc_scores_$$(date +%Y%m%d_%H%M%S).dump
	@echo "✅ Saved to backups/"
