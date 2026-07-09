.PHONY: build deploy logs logs-vps db-pull db-push db-backup init-data

GREEN  := \033[0;32m
RESET  := \033[0m

# =========================================================
# Локальная разработка
# =========================================================

build:
	@echo "$(GREEN)Building...$(RESET)"
	./gradlew clean build -x test
	@echo "✅ Build complete"

# Поднять локально (PostgreSQL + приложение)
up: build
	docker compose up -d --build

down:
	docker compose down --remove-orphans

logs:
	docker compose logs -f app

restart:
	docker compose restart app

# =========================================================
# Деплой на VPS — make deploy HOST=root@IP
# =========================================================

deploy: build
	@if [ -z "$(HOST)" ]; then echo "❌ Usage: make deploy HOST=root@IP"; exit 1; fi
	$(eval JAR := $(shell ls build/libs/ts-wc-scores-*.jar | grep -v plain | head -1))
	@if [ -z "$(JAR)" ]; then echo "❌ JAR not found"; exit 1; fi
	$(eval VERSION := $(shell echo $(JAR) | grep -oP '\d+\.\d+\.\d+'))
	@echo "📦 Deploying v$(VERSION) to $(HOST)..."
	# Открываем одно SSH соединение и переиспользуем его для всех команд
	ssh -o ControlMaster=yes -o ControlPath=/tmp/ssh-wc-%r@%h:%p -o ControlPersist=60 $(HOST) "mkdir -p ~/ts-wc-scores/data/postgres"
	scp -o ControlPath=/tmp/ssh-wc-%r@%h:%p \
		$(JAR) \
		Dockerfile \
		docker-compose.yml \
		$(HOST):~/ts-wc-scores/
	ssh -o ControlPath=/tmp/ssh-wc-%r@%h:%p $(HOST) "cd ~/ts-wc-scores \
		&& mkdir -p build/libs \
		&& rm -f build/libs/*.jar \
		&& mv ts-wc-scores-*.jar build/libs/app.jar \
		&& command -v docker >/dev/null 2>&1 || { echo '❌ Docker не установлен на сервере. Установите: curl -fsSL https://get.docker.com | sh'; exit 1; } \
		&& docker compose down --remove-orphans \
		&& docker image rm wc-scores-app 2>/dev/null || true \
		&& docker compose build --no-cache app \
		&& docker compose up -d"
	# Закрываем соединение
	ssh -O exit -o ControlPath=/tmp/ssh-wc-%r@%h:%p $(HOST) 2>/dev/null || true
	@echo "✅ v$(VERSION) deployed!"

logs-vps:
	@if [ -z "$(HOST)" ]; then echo "❌ Usage: make logs-vps HOST=root@IP"; exit 1; fi
	ssh $(HOST) "docker compose -f ~/ts-wc-scores/docker-compose.yml logs -f app"

# =========================================================
# Синхронизация БД
# =========================================================

db-pull:
	@if [ -z "$(HOST)" ]; then echo "❌ Usage: make db-pull HOST=root@IP"; exit 1; fi
	@echo "⚠️  Перезапишет локальную БД. Продолжить? [y/N]" && read ans && [ "$$ans" = "y" ]
	ssh $(HOST) "docker compose -f ~/ts-wc-scores/docker-compose.yml exec -T postgres \
		pg_dump -U wc_user --clean --if-exists --no-owner -Fc wc_scores" \
		> /tmp/wc_scores.dump
	docker compose exec -T postgres pg_restore -U wc_user -d wc_scores \
		--clean --if-exists --no-owner -Fc /tmp/wc_scores.dump || true
	@echo "✅ Done"

db-push:
	@if [ -z "$(HOST)" ]; then echo "❌ Usage: make db-push HOST=root@IP"; exit 1; fi
	@echo "⚠️  Перезапишет БД на сервере. Продолжить? [y/N]" && read ans && [ "$$ans" = "y" ]
	docker compose exec -T postgres \
		pg_dump -U wc_user --clean --if-exists --no-owner -Fc wc_scores > /tmp/wc_scores.dump
	scp /tmp/wc_scores.dump $(HOST):/tmp/wc_scores.dump
	ssh $(HOST) "docker compose -f ~/ts-wc-scores/docker-compose.yml exec -T postgres \
		pg_restore -U wc_user -d wc_scores --clean --if-exists --no-owner -Fc \
		/tmp/wc_scores.dump || true"
	@echo "✅ Done"

db-backup:
	@mkdir -p backups
	docker compose exec -T postgres pg_dump -U wc_user --no-owner -Fc wc_scores \
		> backups/wc_scores_$$(date +%Y%m%d_%H%M%S).dump
	@echo "✅ Saved to backups/"

init-data:
	mkdir -p data/postgres
	sudo chown -R 999:999 data/postgres
	@echo "✅ data/postgres ready"
