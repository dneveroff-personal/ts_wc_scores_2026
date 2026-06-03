.PHONY: build clean up-clean up down run-local rebuild-app status logs
GREEN  := \033[32m
YELLOW := \033[33m

down:
	docker compose -f docker-compose.yml down --remove-orphans

clean:
	@echo "$(YELLOW)Полная очистка..."
	./gradlew clean
	rm -rf */build/ .gradle/ build/
	docker compose -f docker-compose.yml down -v --remove-orphans
	docker rmi $$(docker images "dn-quest/*:dev" -q) 2>/dev/null || true
	@echo "$(GREEN)Очистка завершена!$(RESET)"

up-clean: clean up

build:
	@echo "$(GREEN)Building Project..."
	./gradlew clean build -x test

up:
	@echo "$(GREEN)Starting Project..."
	@$(MAKE) build
	docker compose -f docker-compose.yml up -d --remove-orphans --build

status:
	@echo "$(YELLOW)Containers Status"
	@docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

## SHow last 100 rows of set CONTAINER (C)
logs:
	docker logs $(C) --tail=100

## Run application locally with config override
run-local:
	@echo "$(GREEN)Running application locally with config override..."
	@if [ ! -f config/application.yml ]; then \
		echo "$(YELLOW)Warning: config/application.yml not found, using defaults"; \
	fi
	./gradlew bootRun -Dspring.config.additional-location=file:config/application.yml

rebuild-app:
	@echo "$(GREEN)Rebuilding and ReStarting Project..."
	docker compose stop app
	@$(MAKE) build
	docker compose up -d app --remove-orphans --build