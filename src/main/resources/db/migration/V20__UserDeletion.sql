INSERT INTO permission (id, role_id, permission_type)
        --SuperUser
VALUES ('9a2ae4d8-1513-4be9-a8d5-3053648a68eb', 'ecf19b84-4799-11ec-9858-4c3275913db5', 'DELETE_USER'),
        --UserManager
       ('f22f76d6-a5be-41f6-93a2-19989924d68e', '11822218-0b59-41bb-a01f-b651303786b3', 'DELETE_USER');

ALTER TABLE ccuser ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT false;
