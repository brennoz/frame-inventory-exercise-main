# ADR 0002: Synchronous transactional CSV import

- Status: Accepted
- Date: 2026-08-19

## Context

Operators need to upsert the supplied inventory and understand partial failures. Imports are bounded to 5,000 rows and 2 MB. At this scale, background infrastructure would add operational and consistency costs without improving the user workflow.

## Decision

Accept RFC 4180 CSV as a synchronous multipart request. Validate the filename, headers, shape, and row count before making writes. Map supported columns by header name and process rows sequentially.

Each valid row calls the command service in a `REQUIRES_NEW` transaction and is classified as created, updated, or unchanged. A validation or optimistic-lock failure affects only that row. The response reports aggregate counts and at most 100 detailed row errors.

Do not parallelize the import with virtual threads. One thousand small rows do not justify nondeterministic pressure on the database pool, more complicated failure ordering, or concurrent updates to duplicate frame IDs within one file.

## Consequences

- The API provides an immediate, deterministic partial-success report.
- Successful rows survive invalid rows, while structural errors reject the file before writes.
- Request duration grows with row count and the client must remain connected.
- A future large-volume design should persist an import job, publish it through a transactional outbox, process bounded batches asynchronously, and expose progress and retry state. A broker alone would not make the database write and message publish atomic.
