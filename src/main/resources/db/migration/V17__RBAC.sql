
UPDATE role SET description='permitted to assist inbound callers' WHERE id='94be28c6-5b84-4946-81da-134b511d6ffc';
UPDATE role SET description='permitted to make outbound calls' WHERE id='632b71e4-479a-11ec-a941-4c3275913db5';

UPDATE permission SET permission_type='RESERVED_ADMIN_ROLE_MAINTENANCE' WHERE permission_type='ADMIN_ROLE_MAINTENANCE';
UPDATE permission SET permission_type='RESERVED_USER_ROLE_ADMIN' WHERE permission_type='USER_ROLE_ADMIN';

UPDATE role SET name='Super User' WHERE id='ecf19b84-4799-11ec-9858-4c3275913db5';
UPDATE role SET name='Manager' WHERE id='60e54a1c-9ca7-491f-90d9-eef9c777a34c';
UPDATE role SET name='User Manager' WHERE id='11822218-0b59-41bb-a01f-b651303786b3';
UPDATE role SET name='Enquiries Operator' WHERE id='94be28c6-5b84-4946-81da-134b511d6ffc';
UPDATE role SET name='Outbound Call Operator' WHERE id='632b71e4-479a-11ec-a941-4c3275913db5';

ALTER TABLE role ADD UNIQUE (name);
