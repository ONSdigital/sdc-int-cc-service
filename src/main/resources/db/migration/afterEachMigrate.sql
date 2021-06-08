-- run automatically after each migration step

--
-- Since the CCSvc user does not own the schema (for security), we grant CRUD privileges
-- after each migration.
--
GRANT USAGE ON SCHEMA cc_schema TO ccsvc;
GRANT SELECT, UPDATE, INSERT, DELETE ON ALL TABLES IN SCHEMA cc_schema TO ccsvc;
GRANT SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA cc_schema TO ccsvc;

-- EOF
