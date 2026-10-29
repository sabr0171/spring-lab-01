-- Test-only migration: plays the role of "a new migration added later" in SchemaVersioningTest
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_balance_non_negative CHECK (balance >= 0);
