-- run automatically after each migration step

--
-- Since the CCSvc user may not own the schema (for security), we grant CRUD privileges
-- after each migration.
--
GRANT USAGE ON SCHEMA cc_schema TO ${username};
GRANT SELECT, UPDATE, INSERT, DELETE ON ALL TABLES IN SCHEMA cc_schema TO ${username};
GRANT SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA cc_schema TO ${username};

-- EOF
