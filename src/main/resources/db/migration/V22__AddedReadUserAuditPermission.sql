 -- add READ_USER_AUDIT permission to usermanager role
INSERT INTO permission (id, role_id, permission_type)
VALUES ('7a73d3a8-b281-4e9c-b772-ac13b964c059', '11822218-0b59-41bb-a01f-b651303786b3', 'READ_USER_AUDIT');

 -- add READ_USER_AUDIT permission to superuser role
INSERT INTO permission (id, role_id, permission_type)
VALUES ('9b643791-34c0-4649-9fef-2b85bfa55121', 'ecf19b84-4799-11ec-9858-4c3275913db5', 'READ_USER_AUDIT');

-- add a user into the usermanager role
INSERT INTO ccuser_role (role_id, ccuser_id)
VALUES ('11822218-0b59-41bb-a01f-b651303786b3', '6774fade-479c-11ec-811e-4c3275913db5'); -- Peter Bochel
