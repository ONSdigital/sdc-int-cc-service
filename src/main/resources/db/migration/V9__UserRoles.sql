-- refine and rename some existing tables
-- to keep automatic index names in line, it is easier to drop and recreate
-- some tables, rather than individual alter SQL.

DROP TABLE IF EXISTS operator CASCADE;
DROP TABLE IF EXISTS case_interaction CASCADE;
DROP TABLE IF EXISTS operator_role CASCADE;
DROP TABLE IF EXISTS admin_role CASCADE;

-- Named 'ccuser' since 'user' is a reserved word.
CREATE TABLE ccuser (
  id UUID PRIMARY KEY,
  name TEXT UNIQUE NOT NULL,
  active BOOLEAN DEFAULT TRUE NOT NULL
);

CREATE TABLE case_interaction (
  id UUID PRIMARY KEY,
  case_id UUID REFERENCES collection_case NOT NULL,
  ccuser_id UUID REFERENCES ccuser NOT NULL,
  created_date_time TIMESTAMP NOT NULL,
  type TEXT NOT NULL,
  outcome TEXT NOT NULL,
  note TEXT
);

CREATE TABLE ccuser_role (
  role_id UUID REFERENCES role NOT NULL,
  ccuser_id UUID REFERENCES ccuser NOT NULL
);

CREATE TABLE admin_role (
  role_id UUID REFERENCES role NOT NULL,
  ccuser_id UUID REFERENCES ccuser NOT NULL
);

ALTER TABLE role ADD UNIQUE (name);
ALTER TABLE permission RENAME COLUMN authorised_activity TO permission_type;

-- dummy data population to assist testing --

-- create users
INSERT INTO ccuser (id, name, active)
VALUES ('382a8474-479c-11ec-a052-4c3275913db5', 'Fred', true),
       ('46e62d6a-479c-11ec-aab4-4c3275913db5', 'Jane', true),
       ('6774fade-479c-11ec-811e-4c3275913db5', 'Marge', false);

-- create roles
INSERT INTO role (id, name)
VALUES ('ecf19b84-4799-11ec-9858-4c3275913db5', 'superuser'),
       ('632b71e4-479a-11ec-a941-4c3275913db5', 'teloperator');

-- add permissions to roles
INSERT INTO permission (id, role_id, permission_type)
VALUES ('8f50c0b2-479a-11ec-94c1-4c3275913db5', 'ecf19b84-4799-11ec-9858-4c3275913db5', 'SUPER_USER'),
       ('bd95eabe-479b-11ec-8bb2-4c3275913db5', '632b71e4-479a-11ec-a941-4c3275913db5', 'SEARCH_CASES'),
       ('f0a8d736-479b-11ec-9e7f-4c3275913db5', '632b71e4-479a-11ec-a941-4c3275913db5', 'VIEW_CASE_DETAILS');

-- give an admin role to a user
INSERT INTO admin_role (role_id, ccuser_id)
VALUES ('632b71e4-479a-11ec-a941-4c3275913db5',   -- teloperator
        '46e62d6a-479c-11ec-aab4-4c3275913db5');  -- jane

-- give a non-admin role to a user
INSERT INTO ccuser_role (role_id, ccuser_id)
VALUES ('632b71e4-479a-11ec-a941-4c3275913db5',   -- teloperator
        '382a8474-479c-11ec-a052-4c3275913db5'),  -- fred
       ('ecf19b84-4799-11ec-9858-4c3275913db5',   -- superuser
        '6774fade-479c-11ec-811e-4c3275913db5');  -- marge

