-- ═══════════════════════════════════════════════════════════════════════════
-- V11: Add Invoice Template Table and Update Invoice Table
-- ═══════════════════════════════════════════════════════════════════════════
-- Purpose: Store invoice templates in database for dynamic template management
-- Features: Template versioning, caching, and history tracking
-- ═══════════════════════════════════════════════════════════════════════════

-- Create invoice_templates table
CREATE TABLE IF NOT EXISTS invoice_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    version INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

-- Create indexes for invoice_templates
CREATE INDEX IF NOT EXISTS idx_invoice_template_active ON invoice_templates(is_active);
CREATE INDEX IF NOT EXISTS idx_invoice_template_version ON invoice_templates(version);

-- Add template_version column to invoices table
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS template_version INTEGER;

-- Add comment for documentation
COMMENT ON TABLE invoice_templates IS 'Stores HTML invoice templates for dynamic template management without backend redeploy';
COMMENT ON COLUMN invoice_templates.version IS 'Auto-incremented version number for tracking template changes';
COMMENT ON COLUMN invoice_templates.is_active IS 'Only one template should have is_active = true at any time';
COMMENT ON COLUMN invoices.template_version IS 'Tracks which template version was used to generate this invoice';

