# ADR 004 — JPA and clean backend boundaries

Date: 2026-09-29
Status: accepted for new and substantially changed backend persistence; existing JDBC migration pending.

## Problem

The first identity and organization slices use `JdbcTemplate` and PostgreSQL functions for all persistence. That made ordinary database access harder to follow and left the project without a firm ORM standard. A repository rename also left two Spring beans implementing the same port, which broke CI startup.

## Decision

Use Spring Data JPA for ordinary business persistence. Keep `*JpaEntity`, `*JpaRepository`, domain model and HTTP `*Request`/`*Response` DTOs separate, with named mappers at boundaries where conversion is needed. Application services coordinate the use case; repositories persist state; domain code owns business rules. Use module-owned enums for fixed business values. Require descriptive names, cohesive classes, constructor injection, no unused code and one active implementation per port.

Permit focused native SQL or JDBC only when an identified PostgreSQL operation cannot be represented safely and clearly with JPA. Record why it is needed and test its tenant scope, transaction, audit and mapping behavior against PostgreSQL. Do not replace atomic database operations solely to claim ORM coverage. Utilities and design patterns must solve a real repeated operation or variation; they are not mandatory extra layers.

Apply this standard to new features and persistence code being substantially changed. Migrate the existing JDBC implementations in a separate reviewed change that preserves row-level security, idempotency and audit behavior. Do not claim the current modules already use JPA repositories.

## Verification

Backend review checks the entity/repository/DTO/mapper boundaries and requires an explanation plus PostgreSQL tests for each native-SQL exception. Existing integration and architecture checks remain required. This decision changes guidance only; no runtime code or database schema changed here.
