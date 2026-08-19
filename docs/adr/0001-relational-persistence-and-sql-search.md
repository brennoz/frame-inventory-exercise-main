# ADR 0001: Relational persistence and SQL search

- Status: Accepted
- Date: 2026-08-19

## Context

Frames have structured, validated fields and require atomic updates with field-level audit history. The supplied inventory is approximately 1,000 rows. Search needs case-insensitive substrings across a small set of fields plus exact enum filters and pagination.

## Decision

Use MariaDB as the single system of record, Spring Data JPA for persistence, and Flyway for schema evolution. Store frames, revisions, and revision changes in normalized relational tables.

Implement search with a JPA specification. Text search escapes SQL wildcard characters and matches frame ID, site number, station, address, town, postcode, and format. Status, environment, and media type are exact predicates. Results use stable pagination ordered by update time and frame ID.

## Consequences

- Frame writes and their audit revisions share one relational transaction.
- The stack has one datastore to operate, back up, and make ready.
- SQL substring matching is easy to reason about and sufficient at the current scale.
- Leading-wildcard searches cannot fully use ordinary B-tree indexes and may scan as data grows.
- If scale, fuzzy matching, or geospatial discovery becomes material, a search projection can be added without replacing MariaDB as the source of truth. That projection should be populated through a transactional outbox rather than dual writes.
