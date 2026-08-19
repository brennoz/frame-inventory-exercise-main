# Implementation Journal

## Provenance and scope

This document is a retrospective record of the AI-assisted implementation completed on 19 August 2026. It was reconstructed from the Git history and the visible conversation milestones after discovering that Entire had not captured the Codex sessions.

It is not a verbatim transcript. Earlier assistant responses, tool output, and session metadata are not completely recoverable, and private system instructions and authentication details are deliberately excluded. The commit history remains the authoritative record of code changes; this journal records the main requests, decisions, review gates, and outcomes that led to them.

## Delivery approach

The work was intentionally split into small, reviewable gates. At each gate, implementation and tests were completed, manual verification steps were provided, and work stopped for user review before committing. Additional review-agent passes were requested after the larger backend and frontend slices, and their findings were fixed before approval.

The agreed priorities were:

- Deliver a runnable end-to-end application in one working day.
- Spend more time on backend correctness than visual polish or browser automation.
- Keep each approved slice in a separate public commit.
- Use a modular Spring Boot monolith with recognizable controller, service, repository, model, DTO, configuration, and web boundaries.
- Use MariaDB as the only datastore, with Flyway migrations and SQL-backed search.
- Use optimistic locking to prevent lost updates and Java 21 virtual threads to improve blocking-I/O concurrency.
- Process CSV rows sequentially in independent transactions for deterministic partial success.
- Keep supplied exercise instructions out of the public implementation history where possible.

## Architectural decisions

### Persistence and search

MariaDB stores frames and their field-level revision history. Database-backed filtering, pagination, and case-insensitive substring search were selected because the supplied inventory contains approximately 1,000 rows. OpenSearch, MongoDB, Kafka, and Redis were removed or deferred to avoid unnecessary operational complexity.

### Concurrency

Frames use JPA optimistic locking through a version column. Updates require the version previously read by the client, and stale writes return `409 Conflict`. A virtual-thread integration test runs concurrent updates using the same version and verifies that exactly one succeeds, one conflicts, and only the successful update is audited. Virtual threads improve concurrency for blocking work but do not replace transactions, locking, or connection-pool limits.

### Audit history

Create and update operations write the frame and its revision in the same transaction. Revisions identify the action, source, actor, and timestamp and store old/new values for each changed field. No-op updates do not create misleading history entries.

### CSV import

CSV files are parsed by header name with Apache Commons CSV and upserted by immutable `frame_id`. The file structure is validated before writes begin; valid rows are then processed sequentially, each in its own transaction. The response distinguishes created, updated, unchanged, and failed rows. Upload size, row count, and reported error count are bounded.

### Frontend

The React application follows the supplied Direction A design and separates API clients, typed models, hooks, reusable components, layout, and pages. It implements inventory search, filters, pagination, detail, create, edit, history, and CSV import, including loading, error, empty, stale-edit, and partial-import states. Responsive table containment was specifically reviewed and corrected.

## Implementation timeline

| Commit | Gate | Key outcome |
|---|---|---|
| `91bdda9` | Setup | Created the public project scaffold, Compose stack, Java and React modules, supplied data/design assets, ignore rules, and initial Entire configuration. |
| `7a3f187` | Gate 1 | Added the MariaDB/Flyway/JPA foundation, layered backend packages, frame entity and enums, repository specifications, paginated read/search API, `ProblemDetail` handling, and MariaDB integration tests. |
| `93e256a` | Gate 2 | Added validated create/update APIs, optimistic locking, transactional field-level history, duplicate and stale-write conflicts, no-op handling, and virtual-thread concurrency coverage. |
| `43c9b39` | Gate 2 review fixes | Preserved eight-decimal coordinate precision, rejected frame IDs unsafe for path-based APIs, handled unreadable request bodies consistently, and added regressions for all three findings. |
| `7aacfde` | Operational hardening | Added request-ID correlation, bounded access logging, database-backed readiness, non-root backend execution, loopback-only database exposure, Compose health dependencies, and observability integration coverage. |
| `dd55824` | Gate 3 | Added multipart CSV import, header validation, per-row transactional upsert, partial-success reporting, import audit history, quoted-field coverage, and malformed/oversized upload handling. |
| `714d2e4` | Gate 3 review fixes | Added the 5,000-row guard, capped returned row errors while retaining total failures, separated invalid-row failures from infrastructure failures, and introduced focused import service unit tests. |
| `036e5e5` | Gate 4A | Built the responsive inventory and detail workspace with filters, pagination, preserved navigation state, local Inter fonts, supplied icons, typed API boundaries, and frontend regression tests. |
| `1824cbb` | Gate 4B | Added reusable frame forms, create/edit pages, stale-write recovery, history presentation, API/form tests, and navigation integration. |
| `c9a9458` | Gate 4C | Added the CSV upload workflow, result and row-error presentation, API/page tests, and a compact mixed-outcome demonstration file. |
| `f89bcbe` | Gate 5 | Reworked the README, added three ADRs, documented setup, testing, operations, safeguards, limitations, concurrency, CSV behavior, and final architectural trade-offs. |
| `1bca5c4` | Entire clarification | Documented that Entire was enabled but had captured zero checkpoints, no linked sessions, and no remote checkpoint branch. |

## Review and verification checkpoints

### Gate 1

The first review focused on backend package structure and the MariaDB port. The backend was reorganized into meaningful architectural folders, port `3306` availability was verified, and manual `curl` instructions were supplied for list, search, filter, pagination, detail, not-found, and health behavior before approval.

### Gate 2

The second review covered frame creation, duplicate IDs, updates, version conflicts, revision ordering, and concurrent writes. Three boundary findings were fixed in a separate commit. A later hardening discussion added pragmatic observability and container/runtime safeguards without introducing authentication or authorization.

### Gate 3

CSV behavior was exercised for quoted fields, creates, updates, unchanged rows, malformed headers, and partial failures. Review findings around resource limits, error truncation, and failure classification were fixed before the gate was approved. This gate also addressed the earlier testing discussion by adding focused unit tests alongside the MariaDB integration suite.

### Gate 4

Frontend delivery was split further into read-only inventory/detail, create/edit/history, and file upload slices. Each slice received a review before commit. Particular attention was paid to viewport resizing and horizontal table containment, stale-edit recovery, request error handling, and readable partial-import results.

### Gate 5

Final verification covered backend tests against MariaDB, frontend tests/lint/build, Compose startup, and manual smoke paths for search, create, edit, stale update, history, and CSV import. ADRs record the principal persistence, import, and concurrency decisions.

## Testing strategy

Integration tests were emphasized initially because the highest-risk behavior crosses Spring MVC validation, transactions, JPA mappings, Flyway schema, MariaDB semantics, optimistic locking, and HTTP `ProblemDetail` responses. Testcontainers keeps those tests representative of the production database.

Focused unit tests were then added where they provided faster and clearer feedback, particularly for CSV import safeguards and frontend form/API behavior. Browser automation remained out of scope for the one-day exercise; frontend workflows were covered by component tests, build/lint checks, and manual responsive verification.

## Entire outcome

Entire installed repository settings, Codex lifecycle-hook declarations, and local Git hooks. However, the Codex hooks were not trusted in the client used during implementation, so no session became active and no checkpoint was linked to a commit. Consequently:

- `entire checkpoint list` reports zero checkpoints on `main`.
- Existing commits contain no `Entire-Checkpoint` trailers.
- The remote exposes no `entire/checkpoints/v1` branch.
- The earlier sessions cannot be reconstructed as a genuine Entire transcript.

Future capture requires starting the interactive Codex CLI from this repository, approving the project hooks there, confirming `entire doctor` is clean, and accepting checkpoint linkage when committing. Because the repository is public, any pushed checkpoint transcript should be reviewed for sensitive information first.

## Result

The initial functional plan was completed: the Compose stack provides frame search, detail, create, edit, optimistic concurrency, field-level history, and transactional CSV upsert through a responsive React interface. Deferred capabilities and production-hardening gaps remain documented in the README and ADRs.
