-- ============================================================================
-- Migration V8: No-op (superseded by V5 and V6)
--
-- V5 creates all financial columns, settlements, and financial_transactions.
-- V6 re-applies NOT NULL + DEFAULT constraints.
-- The PL/pgSQL DO block that was here previously is no longer needed since
-- V5 is now clean and V6 handles constraint enforcement.
-- This migration is intentionally empty.
-- ============================================================================

-- (intentionally empty)

-- ============================================================================
-- END OF MIGRATION V8
-- ============================================================================

