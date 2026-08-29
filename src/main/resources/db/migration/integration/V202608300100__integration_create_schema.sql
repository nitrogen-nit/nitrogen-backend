-- Module integration — Async infrastructure: outbox, job, dedup
-- Một module sở hữu đúng một schema (§8). Không module nào ghi vào schema của module khác.
CREATE SCHEMA IF NOT EXISTS integration;
