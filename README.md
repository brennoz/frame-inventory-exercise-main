# Frame Inventory

Frame Inventory is a small full-stack service for managing out-of-home media frames. Operators can search the inventory, inspect a frame, create or edit records, review field-level history, and upsert frames from CSV.

## Quick start

Requirements: Docker Engine with Docker Compose v2. Ports `3000`, `8080`, and `3306` must be available.

```bash
docker compose up --build
```

Wait for the backend and database health checks, then open:

- Application: <http://localhost:3000/frames>
- Backend health: <http://localhost:8080/actuator/health>
- Readiness, including MariaDB: <http://localhost:8080/actuator/health/readiness>

MariaDB data is retained in the `mariadb-primary-data` volume. To stop the stack, run `docker compose down`. To also reset all inventory data, run `docker compose down -v`.

## Architecture

| Module | Technology | Responsibility |
|---|---|---|
| `src/backend` | Java 21, Spring Boot, JPA, Flyway | REST API, validation, transactions, audit history, CSV upsert |
| `src/frontend` | React, TypeScript, Vite | Inventory, detail, edit, history, and import workflows |
| `mariadb-primary` | MariaDB 11.4 | Frames, revisions, and field changes |

The backend is a modular monolith. HTTP controllers delegate to application services, domain entities own frame state and diffing, and Spring Data repositories isolate persistence. Flyway owns the schema. The frontend follows a similar boundary between API clients, pages, reusable components, hooks, and typed models.

MariaDB is deliberately the only datastore. At the current inventory size, database-backed filtering and case-insensitive substring search are simpler to operate than a separate search index. See the [architecture decisions](docs/adr/README.md) for the trade-offs.

## Application workflow

1. Open the Frames workspace to search by frame ID, site, station, address, town, postcode, or format.
2. Combine text search with status, environment, and media-type filters.
3. Open a frame to inspect it, edit it, or view its newest-first field history.
4. Use **New frame** for manual creation. Frame IDs are immutable.
5. Use **Import** to upload CSV inventory and inspect created, updated, unchanged, and failed counts.

Manual writes are attributed to `demo-user`; CSV writes are attributed to `csv-import`. Editing requires the version read with the frame. A stale update returns `409 Conflict` and the UI offers to reload the current record.

## CSV import

Start with [data/inventory_frame.csv](data/inventory_frame.csv), which contains 1,000 frames. The import is an upsert keyed by `frame_id` and accepts RFC 4180 quoted values.

For a compact partial-success demonstration, first import the full inventory and then upload [data/gate4c_mixed_import.csv](data/gate4c_mixed_import.csv). On that baseline, its first run returns one created, one updated, one unchanged, and one intentionally failed row.

The following headers are required; additional headers are ignored:

```text
frame_id,type_classic_digital,format,environment,site_no,station,address,region,country_code,town,postcode,longitude,latitude,status,status_reason,number_of_slots,distance_to_closest_school,pixel_height,pixel_width,premium
```

Files are limited to 2 MB and 5,000 data rows. The complete structure is validated before writes begin. Rows are then processed sequentially in independent transactions, so one invalid row does not roll back successful rows. At most 100 row errors are returned to the client while the failed count remains complete.

## API

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/frames` | Paginated search and filters |
| `GET` | `/api/frames/{frameId}` | Frame detail |
| `POST` | `/api/frames` | Create a frame |
| `PUT` | `/api/frames/{frameId}` | Update using the supplied `version` |
| `GET` | `/api/frames/{frameId}/history` | Newest-first field revisions |
| `POST` | `/api/frames/import` | Multipart CSV upload in the `file` part |

Search parameters are `q`, `status`, `environment`, `mediaType`, `page`, and `size`. Page numbers are zero-based and size is limited to 100. Validation, missing records, duplicates, stale versions, and invalid CSV files use RFC 9457-style `ProblemDetail` responses.

## Development and tests

Backend tests use Testcontainers and require a running Docker daemon:

```bash
cd src/backend
./gradlew test
```

Frontend development requires Node.js 22:

```bash
cd src/frontend
npm ci
npm test
npm run lint
npm run build
```

The same frontend checks can run without a host Node installation:

```bash
docker compose run --rm --no-deps frontend npm test
docker compose run --rm --no-deps frontend npm run lint
docker compose run --rm --no-deps frontend npm run build
```

For local module development, start MariaDB with `docker compose up -d mariadb-primary`, run `./gradlew bootRun` from `src/backend`, and run `npm run dev` from `src/frontend`.

## Operations and safeguards

- Java virtual threads are enabled for request handling. They improve blocking-I/O concurrency but do not replace transactions, optimistic locking, or the bounded database connection pool.
- Every API response includes a safe `X-Request-ID`. Request logs contain method, path, status, duration, and request ID, but not bodies or query values.
- Actuator exposes only health and info; readiness includes the database and health requests log at debug level.
- The database port binds to loopback, the backend container runs as a non-root user, and uploads and request values are bounded and validated.
- Compose credentials are development-only and must be replaced outside a local environment.

## Current limitations

- Authentication and authorization are not implemented; the displayed user and audit actor are fixed demo identities.
- CSV import is synchronous and has no background progress or cancellation.
- Substring search is SQL-backed and intentionally optimized for the current scale, not millions of frames or fuzzy/geospatial queries.
- There is no frame deletion, map view, master-data administration, or event broker.
- Compose runs the Vite development server. A production deployment should serve the built static assets behind a hardened web server.
- Frontend workflows have component tests and manual responsive verification, but no browser automation suite.
