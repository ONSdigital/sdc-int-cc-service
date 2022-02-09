CREATE TABLE survey_usage(
  id UUID PRIMARY KEY,
  survey_type TEXT UNIQUE NOT NULL
);

CREATE TABLE ccuser_survey_usage (
  survey_usage_id UUID REFERENCES survey_usage NOT NULL,
  ccuser_id UUID REFERENCES ccuser NOT NULL
);

ALTER TABLE role ADD COLUMN description TEXT;

INSERT INTO survey_usage (id, survey_type)
VALUES ('f1fa2959-ccf5-447e-99c3-f8c6fc77a90a', 'SOCIAL');

DELETE FROM permission;
DELETE FROM ccuser_role;
DELETE FROM admin_role;
DELETE FROM role; 
DELETE FROM ccuser;
DELETE FROM case_interaction;

-- create users
INSERT INTO ccuser (id, name, active)
VALUES ('382a8474-479c-11ec-a052-4c3275913db5', 'robert.catling@ext.ons.gov.uk', true),
       ('46e62d6a-479c-11ec-aab4-4c3275913db5', 'philip.whiles@ext.ons.gov.uk', true),
       ('dc4eb75d-1478-408a-96f0-0309fc8e03fb', 'simon.diaz@ext.ons.gov.uk', true),
       ('3ed3362a-65e6-4f1b-ab8a-71c2595af25c', 'kieran.wardle@eons.gov.uk', true),
       ('6774fade-479c-11ec-811e-4c3275913db5', 'peter.bochel@ext.ons.gov.uk', true);

-- create roles
INSERT INTO role (id, name)
VALUES ('ecf19b84-4799-11ec-9858-4c3275913db5', 'superuser'),
       ('60e54a1c-9ca7-491f-90d9-eef9c777a34c', 'manager'),
       ('11822218-0b59-41bb-a01f-b651303786b3', 'usermanager'),
       ('94be28c6-5b84-4946-81da-134b511d6ffc', 'enquiriesoperator'),
       ('632b71e4-479a-11ec-a941-4c3275913db5', 'outboundcalloperator');

-- add permissions to superuser role
INSERT INTO permission (id, role_id, permission_type)
VALUES ('8f50c0b2-479a-11ec-94c1-4c3275913db5', 'ecf19b84-4799-11ec-9858-4c3275913db5', 'SUPER_USER');

-- add permissions to enquiriesoperator role
INSERT INTO permission (id, role_id, permission_type)
VALUES ('bd95eabe-479b-11ec-8bb2-4c3275913db5', '94be28c6-5b84-4946-81da-134b511d6ffc', 'SEARCH_CASES'),
       ('594978b4-6806-4fe7-baa3-807e69d6c2af', '94be28c6-5b84-4946-81da-134b511d6ffc', 'VIEW_CASE'),
       ('a82cef6b-16d7-4b05-943b-fcd68414dfb5', '94be28c6-5b84-4946-81da-134b511d6ffc', 'REFUSE_CASE'),
       ('12ed889a-b126-414d-81c0-9017a0001cc8', '94be28c6-5b84-4946-81da-134b511d6ffc', 'REMOVE_CASE'),
       ('2e8a439f-1bcd-4f2a-b529-e6819c863497', '94be28c6-5b84-4946-81da-134b511d6ffc', 'INVALIDATE_CASE'),
       ('6d6aa00b-4dea-4238-8b49-049775bdb0d8', '94be28c6-5b84-4946-81da-134b511d6ffc', 'MODIFY_CASE'),
       ('1474c102-01a0-4730-9c4a-d54557e3934e', '94be28c6-5b84-4946-81da-134b511d6ffc', 'REQUEST_SMS_FULFILMENT'),
       ('af9efd5c-484f-46ca-bf42-4a7b8d189ad3', '94be28c6-5b84-4946-81da-134b511d6ffc', 'REQUEST_POSTAL_FULFILMENT'),
       ('ddaca1bb-b150-4bb3-a0fb-21e9f7d67b68', '94be28c6-5b84-4946-81da-134b511d6ffc', 'REQUEST_EMAIL_FULFILMENT'),
       ('d8deb245-13cc-479e-9542-fe1d17119832', '94be28c6-5b84-4946-81da-134b511d6ffc', 'ADD_CASE_INTERACTION');
       
 -- add permissions to manager role
INSERT INTO permission (id, role_id, permission_type)
VALUES ('80d90533-f07e-4d28-8765-842fc5f8bb81', '60e54a1c-9ca7-491f-90d9-eef9c777a34c', 'SEARCH_CASES'),
       ('ccb40d69-848e-42c4-ab08-35edd2c42a4d', '60e54a1c-9ca7-491f-90d9-eef9c777a34c', 'VIEW_CASE'),
       ('d4a152c7-66fd-40d7-96fd-b44e32c9a9c8', '60e54a1c-9ca7-491f-90d9-eef9c777a34c', 'REFUSE_CASE'),
       ('1eec0f25-63cd-4fe0-837a-9ade79eab20d', '60e54a1c-9ca7-491f-90d9-eef9c777a34c', 'REMOVE_CASE'),
       ('8a113a21-0b2d-44fa-a5b6-0a5bd2056464', '60e54a1c-9ca7-491f-90d9-eef9c777a34c', 'INVALIDATE_CASE'),
       ('8dd5d86c-cee0-4b58-8387-4ed68621f187', '60e54a1c-9ca7-491f-90d9-eef9c777a34c', 'MODIFY_CASE'),
       ('4cb097ee-d1c5-4dcb-b854-e56f0c441f3b', '60e54a1c-9ca7-491f-90d9-eef9c777a34c', 'LAUNCH_EQ'),
       ('9d95575d-c723-43b3-9b6a-0bc3deca8b3f', '60e54a1c-9ca7-491f-90d9-eef9c777a34c', 'CALL_RESPONDENT_ADHOC'),
       ('11108209-4caf-40c9-8392-156282919c11', '60e54a1c-9ca7-491f-90d9-eef9c777a34c', 'CALL_RESPONDENT_PRIORITISED'),
       ('cae56206-4981-41e1-8839-d6111e9fc446', '60e54a1c-9ca7-491f-90d9-eef9c777a34c', 'CALL_RESPONDENT_SCHEDULED'),
       ('45e1894d-b4f0-4a54-99a7-452daec75068', '60e54a1c-9ca7-491f-90d9-eef9c777a34c', 'REQUEST_SMS_FULFILMENT'),
       ('a8164c28-ded3-4e6a-9a5b-d04091c3c109', '60e54a1c-9ca7-491f-90d9-eef9c777a34c', 'REQUEST_POSTAL_FULFILMENT'),
       ('c79367ce-e025-4646-9eae-20d9c02cc2f4', '60e54a1c-9ca7-491f-90d9-eef9c777a34c', 'REQUEST_EMAIL_FULFILMENT'),
       ('ae6cf7c1-0538-458c-9921-75afbdeab3c2', '60e54a1c-9ca7-491f-90d9-eef9c777a34c', 'ADD_CASE_INTERACTION');      
       
 -- add permissions to outboundoperator role
INSERT INTO permission (id, role_id, permission_type)
VALUES ('a419a655-a3eb-473a-8390-a74fe79eafda', '632b71e4-479a-11ec-a941-4c3275913db5', 'SEARCH_CASES'),
       ('2240ceb8-10bd-4e5b-964e-824851099e60', '632b71e4-479a-11ec-a941-4c3275913db5', 'VIEW_CASE'),
       ('c5a0d6d0-e236-41e1-a8bd-0b0d5b12128c', '632b71e4-479a-11ec-a941-4c3275913db5', 'REFUSE_CASE'),
       ('ab2ca63f-b9b1-48a5-a39b-a1c597cc1432', '632b71e4-479a-11ec-a941-4c3275913db5', 'REMOVE_CASE'),
       ('e460bde4-cfcd-4531-ada4-730c27222a54', '632b71e4-479a-11ec-a941-4c3275913db5', 'INVALIDATE_CASE'),
       ('f56e30e2-7b70-4596-9277-523b345c4fce', '632b71e4-479a-11ec-a941-4c3275913db5', 'LAUNCH_EQ'),
       ('f8e5084c-c4b1-4fb7-ab95-b9fbbb502862', '632b71e4-479a-11ec-a941-4c3275913db5', 'CALL_RESPONDENT_PRIORITISED'),
       ('e263fc43-309a-4a54-b598-adcd915e39e9', '632b71e4-479a-11ec-a941-4c3275913db5', 'CALL_RESPONDENT_SCHEDULED'),
       ('b6ab7e2f-f5a1-4bf1-8bfc-6a20794e54f2', '632b71e4-479a-11ec-a941-4c3275913db5', 'REQUEST_SMS_FULFILMENT'),
       ('98e5c192-7e2b-424a-b62c-8d7bd1780eb5', '632b71e4-479a-11ec-a941-4c3275913db5', 'REQUEST_POSTAL_FULFILMENT'),
       ('29a09481-a41d-464f-9d4d-511ed26a99f6', '632b71e4-479a-11ec-a941-4c3275913db5', 'REQUEST_EMAIL_FULFILMENT'),
       ('a52b0289-e17d-40f7-a11c-105986d1d449', '632b71e4-479a-11ec-a941-4c3275913db5', 'ADD_CASE_INTERACTION');      
       
 -- add permissions to usermanager role
INSERT INTO permission (id, role_id, permission_type)
VALUES ('a2ec2409-0de5-4d0f-9d55-281a374755ed', '11822218-0b59-41bb-a01f-b651303786b3', 'CREATE_USER'),
       ('d58e5707-f32c-4d77-ab83-aa467ba90633', '11822218-0b59-41bb-a01f-b651303786b3', 'READ_USER'),
       ('77ffa322-4452-45ff-916b-e1a3b3d83d2a', '11822218-0b59-41bb-a01f-b651303786b3', 'MODIFY_USER'),
       ('fe0d2a13-ec9c-4cd1-b6a7-b0231d034805', '11822218-0b59-41bb-a01f-b651303786b3', 'USER_SURVEY_MAINTENANCE'),
       ('725777c7-4dc4-4439-a4f1-040dfd2a913b', '11822218-0b59-41bb-a01f-b651303786b3', 'USER_ROLE_MAINTENANCE'),
       ('944c9cd0-1420-4624-9fe3-f6e0ec70ce4d', '11822218-0b59-41bb-a01f-b651303786b3', 'READ_ROLE');
       
-- put users into superuser role
INSERT INTO ccuser_role (role_id, ccuser_id)
VALUES ('ecf19b84-4799-11ec-9858-4c3275913db5', '382a8474-479c-11ec-a052-4c3275913db5'), -- Rob Catling
       ('ecf19b84-4799-11ec-9858-4c3275913db5', '46e62d6a-479c-11ec-aab4-4c3275913db5'), -- Phil Whiles
       ('ecf19b84-4799-11ec-9858-4c3275913db5', 'dc4eb75d-1478-408a-96f0-0309fc8e03fb'), -- Simon Diaz
       ('ecf19b84-4799-11ec-9858-4c3275913db5', '3ed3362a-65e6-4f1b-ab8a-71c2595af25c'), -- Kieran Wardle
       ('ecf19b84-4799-11ec-9858-4c3275913db5', '6774fade-479c-11ec-811e-4c3275913db5'); -- Peter Bochel