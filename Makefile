.PHONY: build deploy db-push db-pull db-backup clean up-clean up down run-local rebuild-app status logs

# --- Локальная сборка ---
build:
	@echo "$(GREEN)Building Project..."
	./gradlew clean build
	@echo "✅ Build complete: build/libs/*.jar"

up:
	@echo "$(GREEN)Starting Project..."
	@$(MAKE) build
	docker compose -f docker-compose.yml up -d --remove-orphans --build

down:
	docker compose -f docker-compose.yml down --remove-orphans

clean:
	@echo "$(YELLOW)Полная очистка..."
	./gradlew clean
	rm -rf */build/ .gradle/ build/
	docker compose -f docker-compose.yml down -v --remove-orphans
	docker rmi $$(docker images "dn-quest/*:dev" -q) 2>/dev/null || true
	@echo "$(GREEN)Очистка завершена!$(RESET)"

rebuild-app:
	@echo "$(GREEN)Rebuilding and ReStarting Project..."
	docker compose stop app
	@$(MAKE) build
	docker compose up -d app --remove-orphans --build


# --- Деплой на VPS ---
# Копируем только собранный jar + docker-compose + конфиги (~15 MB вместо 105 MB)
# Использование: make deploy HOST=user@your-vps.com
deploy: build
	@if [ -z "$(HOST)" ]; then echo "❌ Usage: make deploy HOST=root@89.125.248.168"; exit 1; fi
	@echo "📦 Copying files to $(HOST)..."
	ssh $(HOST) "mkdir -p ~/ts-wc-scores/scripts"
	# Копируем только то что нужно серверу
	rsync -avz 		build/libs/ts-wc-scores-*.jar 		$(HOST):~/ts-wc-scores/app.jar
	rsync -avz 		docker-compose.yml 		Dockerfile 		scripts/setup-vps.sh 		$(HOST):~/ts-wc-scores/
	ssh $(HOST) "cd ~/ts-wc-scores && docker compose down && docker compose up -d"
	@echo "✅ Deployed to $(HOST)"

# --- Синхронизация БД ---

# Отправить локальную БД на VPS (перезаписывает данные на хосте!)
# Использование: make db-push HOST=user@your-vps.com
db-push:
	@if [ -z "$(HOST)" ]; then echo "❌ Usage: make db-push HOST=root@89.125.248.168"; exit 1; fi
	@echo "⚠️  Это перезапишет БД на сервере. Продолжить? [y/N]" && read ans && [ "$$ans" = "y" ]
	@echo "📦 Создаём дамп локальной БД..."
	docker compose exec postgres pg_dump -U wc_user wc_scores > /tmp/wc_scores_dump.sql
	@echo "🚀 Отправляем на $(HOST)..."
	scp /tmp/wc_scores_dump.sql $(HOST):/tmp/wc_scores_dump.sql
	ssh $(HOST) "cd ~/ts-wc-scores && \
		docker compose exec -T postgres psql -U wc_user -d wc_scores < /tmp/wc_scores_dump.sql"
	@echo "✅ БД отправлена на сервер"

# Забрать БД с VPS на локальную машину (перезаписывает локальные данные!)
# Использование: make db-pull HOST=user@your-vps.com
db-pull:
	@if [ -z "$(HOST)" ]; then echo "❌ Usage: make db-pull HOST=root@89.125.248.168"; exit 1; fi
	@echo "⚠️  Это перезапишет локальную БД. Продолжить? [y/N]" && read ans && [ "$$ans" = "y" ]
	@echo "📦 Создаём дамп БД на сервере..."
	ssh $(HOST) "cd ~/ts-wc-scores && \
		docker compose exec -T postgres pg_dump -U wc_user wc_scores" > /tmp/wc_scores_dump.sql
	@echo "📥 Восстанавливаем локально..."
	docker compose exec -T postgres psql -U wc_user -d wc_scores < /tmp/wc_scores_dump.sql
	@echo "✅ БД получена с сервера"

# Сделать бэкап БД локально в файл с датой
db-backup:
	@mkdir -p backups
	docker compose exec -T postgres pg_dump -U wc_user wc_scores \
		> backups/wc_scores_$$(date +%Y%m%d_%H%M%S).sql
	@echo "✅ Backup saved to backups/"

# Создать папку данных с правильными правами (один раз перед первым запуском)
init-data:
	mkdir -p data/postgres
	sudo chown -R 999:999 data/postgres
	@echo "✅ data/postgres ready"

# Просмотр логов
logs:
	docker compose logs -f app