ALTER TABLE permission RENAME COLUMN authorised_activity TO permission_type;

-- dummy data population to assist testing --

-- create users
INSERT INTO operator (id, name, active)
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
INSERT INTO admin_role (id, role_id, operator_id)
VALUES ('7d82f89e-479c-11ec-bb8f-4c3275913db5',
        'ecf19b84-4799-11ec-9858-4c3275913db5',   -- superuser
        '46e62d6a-479c-11ec-aab4-4c3275913db5');  -- jane

-- give a non-admin role to a user
INSERT INTO operator_role (id, role_id, operator_id)
VALUES ('c1985d26-479c-11ec-b947-4c3275913db5',
        '632b71e4-479a-11ec-a941-4c3275913db5',   -- teloperator
        '382a8474-479c-11ec-a052-4c3275913db5');  -- fred

