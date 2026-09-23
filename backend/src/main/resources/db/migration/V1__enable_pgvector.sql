-- Run with a migration role authorized to install extensions.
-- RDS production provisioning/migration must enable vector before runtime startup.
CREATE EXTENSION IF NOT EXISTS vector;
