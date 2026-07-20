.PHONY: build deploy logs logs-vps db-pull db-push db-backup init-data db-dump db-restore

GREEN  := \033[0;32m
RESET  := \033[0m

# =========================================================
# Локальная разработка
# =========================================================
build:
	@echo "$(GREEN)Building...$(RESET)"
	./gradlew clean build -x test
	@echo "✅ Build complete"

up: build
	docker compose -f docker-compose.local.yml up -d --build

down:
	docker compose -f docker-compose.local.yml down --remove-orphans

logs:
	docker compose -f docker-compose.local.yml logs -f app

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
	ssh $(HOST) "docker compose -f ~/ts-wc-scores/docker-compose.yml cp /tmp/wc_scores.dump postgres:/tmp/wc_scores.dump && \
		docker compose -f ~/ts-wc-scores/docker-compose.yml exec -T postgres \
		pg_restore -U wc_user -d wc_scores --clean --if-exists --no-owner -Fc \
		/tmp/wc_scores.dump || true"
	@echo "✅ Done"

db-dump:
	@echo "Creating dump to ./wc_scores.dump..."
	docker compose exec -T postgres pg_dump -U wc_user --clean --if-exists --no-owner -Fc wc_scores > ./wc_scores.dump
	@echo "✅ Done"

db-restore:
	@echo "Restoring from ./wc_scores.dump..."
	docker compose cp ./wc_scores.dump postgres:/tmp/wc_scores.dump
	docker compose exec -T postgres pg_restore -U wc_user -d wc_scores \
		--clean --if-exists --no-owner -Fc /tmp/wc_scores.dump
	@echo "✅ Done"

# =========================================================
# Деплой
# =========================================================
deploy-ghcr:
	@echo "Start deploy ..."
	@if [ -z "$(HOST)" ]; then echo "Usage: make deploy-ghcr HOST=user@server"; exit 1; fi
	ssh $(HOST) "\
		cd ~/ts-wc-scores && \
		docker compose -f docker-compose.prod.yml pull && \
		docker compose -f docker-compose.prod.yml up -d && \
		docker image prune -f"
	@echo "✅ Deploy DONE"

