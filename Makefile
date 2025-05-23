# Colors and emojis for better readability
BLUE=\033[0;34m
GREEN=\033[0;32m
YELLOW=\033[0;33m
RED=\033[0;31m
NC=\033[0m # No Color

.PHONY: setup start stop clean build run-simulator run-processor run-api demo status help

# Setup development environment
setup: ## 🚀 Setup development environment
	@echo "${BLUE}🚀 Setting up flight demo environment...${NC}"
	@docker-compose -f docker/docker-compose.yaml up -d
	@echo "${YELLOW}⏳ Waiting for services to be ready...${NC}"
	@sleep 10
	@echo "${GREEN}✅ Environment setup complete!${NC}"

# Start all services
start: ## 🚀 Start all services
	@echo "${BLUE}🚀 Starting all services...${NC}"
	@docker-compose -f docker/docker-compose.yaml up -d
	@echo "${GREEN}✅ Services started!${NC}"

# Stop all services
stop: ## 🛑 Stop all services
	@echo "${BLUE}🛑 Stopping all services...${NC}"
	@docker-compose -f docker/docker-compose.yaml down
	@echo "${GREEN}✅ Services stopped!${NC}"

# Clean environment
clean: ## 🧹 Clean environment
	@echo "${BLUE}🧹 Cleaning environment...${NC}"
	@docker-compose -f docker/docker-compose.yaml down -v
	@echo "${GREEN}✅ Environment cleaned!${NC}"

# Build all modules
build: ## 🔨 Build all modules
	@echo "${BLUE}🔨 Building all modules...${NC}"
	@./gradlew clean build
	@echo "${GREEN}✅ Build complete!${NC}"

# Run flight simulator
run-simulator: ## ✈️ Run flight simulator
	@echo "${BLUE}✈️ Running flight simulator...${NC}"
	@./gradlew :simulator:run
	@echo "${GREEN}✅ Simulator started!${NC}"

# Run Flink processor
run-processor: ## 🔄 Run Flink processor
	@echo "${BLUE}🔄 Running Flink processor...${NC}"
	@./gradlew :processor:run
	@echo "${GREEN}✅ Processor started!${NC}"

# Run Ktor API
run-api: ## 🌐 Run Ktor API
	@echo "${BLUE}🌐 Running Ktor API...${NC}"
	@./gradlew :api:run
	@echo "${GREEN}✅ API started!${NC}"

# Run demo
demo: ## 🎮 Run complete demo
	@echo "${BLUE}🎮 Running complete demo...${NC}"
	@echo "${YELLOW}Step 1: Starting services...${NC}"
	@make start
	@echo "${YELLOW}Step 2: Running processor...${NC}"
	@./gradlew :processor:run &
	@echo "${YELLOW}Step 3: Running API...${NC}"
	@./gradlew :api:run &
	@echo "${YELLOW}Step 4: Running simulator...${NC}"
	@./gradlew :simulator:run
	@echo "${GREEN}✅ Demo running!${NC}"

# Show status
status: ## 📊 Show status of all components
	@echo "${BLUE}📊 Demo Component Status:${NC}"
	@echo "${YELLOW}Docker Services:${NC}"
	@docker-compose -f docker/docker-compose.yaml ps
	@echo "${GREEN}✅ Status check complete!${NC}"

# Help
help: ## 📚 Show this help
	@echo "${BLUE}📚 Available commands:${NC}"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "${YELLOW}%-20s${NC} %s\n", $$1, $$2}'

# Default target
.DEFAULT_GOAL := help
