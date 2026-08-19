# ADR 0003: Optimistic concurrency and transactional audit history

- Status: Accepted
- Date: 2026-08-19

## Context

Two operators may edit the same frame concurrently. The system must avoid lost updates and retain who changed each field, from which source, and when. No authentication provider is present, so actor identities are currently fixed application values.

## Decision

Use JPA `@Version` on frames and require clients to send the version they read. Reject stale writes with `409 Conflict`. Perform the frame mutation and its field-level revision in the same database transaction. Do not create a revision for a no-op update or a rejected stale write.

Record revisions in `frame_revisions` and individual old/new values in `frame_revision_changes`. Manual changes use actor `demo-user`; imports use `csv-import`.

Enable Java 21 virtual threads for Spring request handling and use them in the concurrency integration test. Virtual threads allow more blocking requests to wait efficiently; they do not serialize access or prevent lost updates. Correctness comes from the version check, the database transaction, and the JPA optimistic lock.

## Consequences

- Exactly one of two writes based on the same version succeeds.
- Audit history cannot commit without its frame mutation, and failed writes leave no revision.
- Clients must handle conflicts by reloading and deliberately reapplying changes.
- Virtual threads remain bounded in practice by database connections and downstream capacity.
- When authentication is introduced, actor values should come from verified identity claims. If revisions must feed other systems, publish them after commit through a transactional outbox.
