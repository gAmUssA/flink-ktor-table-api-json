# Colors and emojis for better readability
BLUE=\033[0;34m
GREEN=\033[0;32m
YELLOW=\033[0;33m
RED=\033[0;31m
NC=\033[0m # No Color

.PHONY: setup start stop clean build run-simulator run-processor run-api run-frontend install-frontend build-frontend demo status help run-all destroy-all generate-docs

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
run-processor: ## 🔄 Run all Flink processor jobs
	@echo "${BLUE}🔄 Running all Flink processor jobs...${NC}"
	@./gradlew :processor:run --args="all"
	@echo "${GREEN}✅ Flink jobs submitted!${NC}"

# Run Flink delay detection job
run-delay-job: ## 🕒 Run Flink delay detection job
	@echo "${BLUE}🕒 Running Flink delay detection job...${NC}"
	@./gradlew :processor:run --args="delay"
	@echo "${GREEN}✅ Delay detection job submitted!${NC}"

# Run Flink density aggregation job
run-density-job: ## 🗺️ Run Flink density aggregation job
	@echo "${BLUE}🗺️ Running Flink density aggregation job...${NC}"
	@./gradlew :processor:run --args="density"
	@echo "${GREEN}✅ Density aggregation job submitted!${NC}"

# Run Ktor API
run-api: ## 🌐 Run Ktor API
	@echo "${BLUE}🌐 Running Ktor API...${NC}"
	@./gradlew :api:run
	@echo "${GREEN}✅ API started!${NC}"

# Run demo
demo: ## 🎮 Run complete demo
	@echo "${BLUE}🎮 Running complete demo...${NC}"
	@./scripts/demo.sh

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

# Install frontend dependencies
install-frontend: ## 📦 Install frontend dependencies
	@echo "${BLUE}📦 Installing frontend dependencies...${NC}"
	@cd frontend && npm install
	@echo "${GREEN}✅ Frontend dependencies installed!${NC}"

# Build frontend
build-frontend: ## 🔨 Build frontend
	@echo "${BLUE}🔨 Building frontend...${NC}"
	@cd frontend && npm run build
	@echo "${GREEN}✅ Frontend built!${NC}"

# Run frontend development server
run-frontend: ## 🌐 Run frontend development server
	@echo "${BLUE}🌐 Running frontend development server...${NC}"
	@cd frontend && npm start
	@echo "${GREEN}✅ Frontend server started!${NC}"

# Run all components (API, frontend, and Flink jobs)
run-all: ## 🚀 Run complete application (API, frontend, and Flink jobs)
	@echo "${BLUE}🚀 Starting complete application...${NC}"
	@echo "${YELLOW}⏳ Starting Docker services...${NC}"
	@docker-compose -f docker/docker-compose.yaml up -d
	@echo "${YELLOW}⏳ Waiting for services to be ready...${NC}"
	@sleep 5
	@echo "${YELLOW}⏳ Starting Flink density aggregation job...${NC}"
	@./gradlew :processor:run --args="density" &
	@echo "${YELLOW}⏳ Starting Flink delay detection job...${NC}"
	@./gradlew :processor:run --args="delay" &
	@echo "${YELLOW}⏳ Starting Ktor API...${NC}"
	@./gradlew :api:run &
	@echo "${YELLOW}⏳ Starting frontend development server...${NC}"
	@cd frontend && npm start
	@echo "${GREEN}✅ All components started!${NC}"

# Destroy all components and clean up everything
destroy-all: ## 💥 Stop and clean up all components of the application
	@echo "${RED}💥 Destroying all components...${NC}"
	@echo "${YELLOW}⏳ Stopping all running processes...${NC}"
	@pkill -f "gradle" || true
	@pkill -f "npm start" || true
	@echo "${YELLOW}⏳ Cleaning Docker containers and volumes...${NC}"
	@docker-compose -f docker/docker-compose.yaml down -v
	@echo "${YELLOW}⏳ Removing temporary files...${NC}"
	@./gradlew clean
	@rm -rf frontend/node_modules frontend/build || true
	@echo "${GREEN}✅ All components destroyed and cleaned up!${NC}"

# Generate project documentation
generate-docs: ## 📚 Generate and update project documentation
	@echo "${BLUE}📚 Generating project documentation...${NC}"
	@mkdir -p docs/api
	@echo "${YELLOW}⏳ Generating API documentation...${NC}"
	@./gradlew :api:dokkaHtml || echo "${RED}⚠️ Dokka HTML generation failed, but continuing...${NC}"
	@cp -r api/build/dokka/html/* docs/api/ 2>/dev/null || echo "${YELLOW}⚠️ No API docs to copy${NC}"
	@echo "${YELLOW}⏳ Updating project status document...${NC}"
	@echo "Last updated: $(shell date)" >> docs/PROJECT_STATUS.md
	@echo "${GREEN}✅ Documentation generated successfully!${NC}"

# Default target
.DEFAULT_GOAL := help
