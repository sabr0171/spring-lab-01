-- New migration added after V1-V3 were already applied (Task E)
CREATE INDEX idx_accounts_owner_name ON accounts (owner_name);
