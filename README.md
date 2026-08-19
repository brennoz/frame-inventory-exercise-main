# Frame Inventory

Monorepo scaffold for the frame-inventory take-home exercise. Bring the whole stack up with one command:

```bash
docker compose up --build
```

## Modules

| Module    | Stack                               | Path            |
|-----------|-------------------------------------|-----------------|
| backend   | Java 21, Spring Boot 3.5, Gradle    | `src/backend/`  |
| frontend  | React 19, Vite, TypeScript          | `src/frontend/` |

## Services

`docker compose up` starts every dependency the exercise needs:

| Service               | Host port | Container port | Notes                                                  |
|-----------------------|-----------|----------------|--------------------------------------------------------|
| frontend              | 3000      | 3000           | Vite dev server, proxies `/api` to the backend         |
| backend               | 8080      | 8080           | Spring Boot API and health endpoint                    |
| mariadb-primary       | 3306      | 3306           | Frame data for Docker and local backend development    |

### Credentials

| Service               | User       | Password       | Database         |
|-----------------------|------------|----------------|------------------|
| mariadb-primary       | `app`      | `app`          | `frames`         |

## Smoke test

Once the stack is up:

- Open <http://localhost:3000> &mdash; the page calls `/api/health` and prints the JSON response.
- Hit the backend directly: <http://localhost:8080/api/health>.

## Running modules without Docker

### Backend

```bash
cd src/backend
./gradlew bootRun
```

### Frontend

```bash
cd src/frontend
npm install
npm run dev
```
