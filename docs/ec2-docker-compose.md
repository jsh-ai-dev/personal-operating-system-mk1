# EC2 Docker Compose deployment

Target:

- Amazon Linux 2023
- Docker and Docker Compose plugin installed
- First phase runs only app, PostgreSQL, and Redis
- Elasticsearch stays disabled unless the optional override file is used

## Environment files

Use separate files by execution mode:

- Local direct app execution: copy `.env.example` or `.env.local.example` to `.env`
- Local Docker Compose: copy `.env.docker.example` to `.env.docker`
- EC2 Docker Compose: copy `.env.server.example` to `.env.server`

Why this split matters:

- When the app runs directly on the host, `localhost` means the host machine.
- When the app runs in Docker Compose, `localhost` means the app container itself.
- Docker Compose containers should connect with service names, for example `postgres` and `redis`.

## Required Docker values

For Docker Compose, these values must use service names:

```dotenv
POS_DB_URL=jdbc:postgresql://postgres:5432/pos_mk1
POS_REDIS_HOST=redis
POS_ELASTICSEARCH_URIS=http://elasticsearch:9200
POS_SEARCH_ELASTICSEARCH_ENABLED=false
POS_DB_BIND=127.0.0.1
POS_REDIS_BIND=127.0.0.1
```

For local direct execution, these values can stay on localhost:

```dotenv
POS_DB_URL=jdbc:postgresql://localhost:5432/pos_mk1
POS_REDIS_HOST=localhost
POS_ELASTICSEARCH_URIS=http://localhost:9200
POS_SEARCH_ELASTICSEARCH_ENABLED=false
```

## EC2 first run

From a fresh server after cloning the repo:

```bash
cd ~/personal-operating-system-mk1
cp .env.server.example .env.server
vi .env.server
```

Change at least these values:

```dotenv
POS_DB_PASSWORD=CHANGE_ME_STRONG_DB_PASSWORD
POS_SECURITY_PASSWORD=CHANGE_ME_STRONG_ADMIN_PASSWORD
POS_JWT_SECRET=CHANGE_ME_AT_LEAST_32_BYTES
```

Start the app stack:

```bash
docker compose --env-file .env.server up -d --build
```

Check status and logs:

```bash
docker compose --env-file .env.server ps
docker compose --env-file .env.server logs -f app
```

Open:

```text
http://<EC2_PUBLIC_IP>:8080
```

If EC2 security groups are not open, allow inbound TCP `8080` from the required source range.

## Stop and restart

```bash
docker compose --env-file .env.server stop
docker compose --env-file .env.server start
```

Update after pulling new code:

```bash
git pull
docker compose --env-file .env.server up -d --build
```

## Optional Elasticsearch

Elasticsearch is not part of the default stack. To enable it:

```bash
docker compose --env-file .env.server -f compose.yaml -f compose.elasticsearch.yaml up -d --build
```

Also set this in `.env.server` if you want search to use Elasticsearch:

```dotenv
POS_SEARCH_ELASTICSEARCH_ENABLED=true
```

On `t3.small`, Elasticsearch may be tight on memory. Keep it disabled for the first phase unless you have a specific need.
