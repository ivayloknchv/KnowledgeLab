## Quick Start

### 1. Clone and configure environment

```bash
cp .env.example .env
# Edit .env with your real values
```

### 2. Generate JWT secret

```bash
chmod +x scripts/generate-jwt-secret.sh
./scripts/generate-jwt-secret.sh
# Copy output into JWT_SECRET in .env
```

### 3. Set up MongoDB Atlas

1. Create a free cluster at [cloud.mongodb.com](https://cloud.mongodb.com)
2. Create a database user
3. Allow your IP (or `0.0.0.0/0` for dev)
4. Copy the connection string into `MONGODB_URI` in `.env`

### 4. Run with Docker Compose

```bash
# Production-like (app + redis)
docker compose up -d

# Development (includes Redis Insight UI at :5540)
docker compose --profile dev up -d
```

### 5. Run locally (no Docker for app)

```bash
# Start Redis only
docker compose up redis -d

# Run Spring Boot app (reads .env automatically with IDE plugin,
# or export variables manually)
export $(cat .env | grep -v '#' | xargs)
./gradlew bootRun
```