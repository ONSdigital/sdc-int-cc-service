The Contact Centre soltion (CC) is comprised of both a Python frontend (CCUI) and Java backend service (CCSvc).

## Azure Authentication
The CCUI protects each entry point/page with SAML - if there is not a valid and current SAML session, the client is redirected to the SAML IDP for authentication challenge.

In each of the current dev,test and integration environments, that IDP is a cluster local simple idp (kristophjunge/test-saml-idp), with local configuration for dev users and some simple example users.

The intention was that that IDP in integration onwards be Azure AD, and the SAML integration was proven with a test/POC Azure tenant provided by Neil Carless (CIA) and Dave Kelly (CIA). That test tenant is transient and cannot be used as a permanent IDP for integration onwards, and the ONS team responsible for the production Azure tenant cannot support non prod environments. At this time there is no team or tenant for non prod Azure AD.



The CCUI will gain from the IDP the username/email, as well as their forename and surname. The username is set in a header in all RESTful requests to the CCSvc, 'x-user-id'.

DB Schema
The CCSvc has RBAC related tables which were modelled on the RM RBAC tables, but adapted to better suit the CC needs:


In particular, whereas RMOps allows for each individual permission to be associated with a DB instance of a survey, CC associates each user with a type of survey (and not the db instance). This was done such that users having roles having permissions, could be added/removed to a survey type en masse, and without needing the survey to be sent by RM in advance.

It is hoped that the SurveyUpdated event might be extended to identify Survey 'topology' ie a Survey 'family' such as 'SOCIAL' as well as a SurveyType such as 'DISABILITY_SURVEY', in which case the RBAC model could be extended also to allow users to be granted access to SOCIAL:DISABILITY_SURVEY or SOCIAL:HEATING_SURVEY. 

Session
There is no session within the CCSvc - each request made by CCUI must have the 'x-user-id' header to identify the principal, and each endpoint will assert using a number of methods within the RBACService bean that the principal is allowed to perform the operation in question. Each request is intercepted by the UserIdentityInterceptor and the principals identity is stored in a thread local - see UserIdentityContext. The principal identity is also put into the SLF4J MDC, allowing the logging framework to include the identity automatically (with logback.xml tweak) in all logging.

As a result of the thread local storage of the principals identity, none of the endpoints are individually required to find it in the request, or to have to pass it in method arguments. In any layer of the application code it is readily accessible.



## RBACService
The RBACService bean is the central class used by endpoints to assert that the principal making an enpoint request has the relevant permission for the operation
being requested. Certain operations require the survey related to the case to be considered - the survey_usage association dictates which surveys the user may access.
Some endpoints simply need to assert that the principal exists in the cc_user table and that they are an active user.
So the exact assert method called by endpoint is use case specific.



| RBACService assert method | Description |
| ----------------- | ---------------- |
| assertNotSelfModification(String userIdentity) | asserts that the thread local principal is not the user in question |
| assertUserPermission(PermissionType permissionType) | asserts that the thread local principle has the given permission |
| assertUserPermission(UUID surveyId, PermissionType permissionType) | asserts that the thread local principle has the given permission AND access to the given survey - not used by the RBAC endpoints but rather Case related endpoints |
| assertUserActiveAndValid() | asserts that the thread local principle exists and is active |
| assertAdminPermission(String roleName, PermissionType permissionType) | asserts that the thread local principle has the requested permission RELATED to the given role. OR has the reserved admin permission required to do so |


## Permissions
At the time of writing, the PermissionsType enum declares the following fine grained permissions:

- CAN_RECEIVE_INBOUND_CALLS
- CAN_MAKE_OUTBOUND_CALLS
- CAN_MANAGE_USERS
- CAN_MANAGE_SYSTEM
- READ_USER_AUDIT
- READ_USER_INTERACTIONS
- SEARCH_CASES
- VIEW_CASE
- REFUSE_CASE
- REMOVE_CASE
- INVALIDATE_CASE
- MODIFY_CASE
- ENROL_CASE
- LAUNCH_EQ
- CALL_RESPONDENT_ADHOC
- CALL_RESPONDENT_PRIORITISED
- CALL_RESPONDENT_SCHEDULED
- REQUEST_SMS_FULFILMENT
- REQUEST_POSTAL_FULFILMENT
- REQUEST_EMAIL_FULFILMENT
- ADD_CASE_INTERACTION
- READ_USER_INTERACTIONS
- CREATE_USER
- READ_USER
- MODIFY_USER
- DELETE_USER
- USER_SURVEY_MAINTENANCE
- USER_ROLE_MAINTENANCE
- CREATE_ROLE
- READ_ROLE
- MAINTAIN_PERMISSIONS
- RESERVED_USER_ROLE_ADMIN
- RESERVED_ADMIN_ROLE_MAINTENANCE

Some of these are speculative, based on features planned for the future, and it is fully anticipated that these will morph over time.

The permissions within the CC RBAC model are more fine grained than in RMOps currently - there is no single SuperUser permission, rather individual permissions for each type of operation a super user may need to perform, and those permissions are associated with a Super User role.

The CCSvc also makes use of the associative table 'admin_role' which maps which users are allowed to add/remove other users from roles.

Two permissions in particular are the 'power' permissions, and their use through a role needs to be carefully maintained - to make those 
two permissions stand out from others they are prefixed 'RESERVED_'.



##Endpoints
The UserEndpoint and RoleEndpoint classes handle requests related to the RBAC management, and the majority of endpoints themselves
assert that the principal has the relevant permission to perform the requested operation:

| HTTP Method | Path | User Permission Required | Admin Permission Required | Additional Assertion | Note |
| ----------------- | ---------------- | ---------------- | ---------------- | ---------------- | ---------------- |
| GET | /users | READ_USER | | | |
| GET | /users/audit | READ_USER_AUDIT | | | |
| GET | /users/{userName} | READ_USER | |  | |
| GET | /users/{userName}/permissions |  | | assertUserValidAndActive  | CCUI calls after /login |
| DELETE | /users/{userName} | DELETE_USER | | assertNotSelfModification  | Only of users never logged in |
| POST | /users | CREATE_USER | |  | Creates user with identity only |
| PUT	| /users  | MODIFY_USER | | assertNotSelfModification | Modifies users SurveyUsage only |
| PUT	| /users/login |  | | assertUserValidAndActive  | Triggers UserAudit and provides forename/surname |
| PUT	| /users/logout |  | | assertUserValidAndActive  | Triggers UserAudit |
| PATCH | /users/{userName}/addUserRole/{roleName} |  | USER_ROLE_MAINTENANCE (AND has admin_role association) | assertNotSelfModification  | Or 'Super User' who has RESERVED_USER_ROLE_ADMIN |
| PATCH | /users/{userName}/removeUserRole/{roleName} |  | USER_ROLE_MAINTENANCE (AND has admin_role association) | assertNotSelfModification  | Or 'Super User' who has RESERVED_USER_ROLE_ADMIN |
| PATCH | /users/{userName}/addAdminRole/{roleName} | RESERVED_ADMIN_ROLE_MAINTENCE | | assertNotSelfModification  | Only 'Super User' will have permission |
| PATCH | /users/{userName}/removeAdminRole/{roleName} | RESERVED_ADMIN_ROLE_MAINTENCE | | assertNotSelfModification  | Only 'Super User' will have permission |
| PATCH | /users/{userName}/addSurvey/{surveyName} | USER_SURVEY_MAINTENCE | | assertNotSelfModification | |
| PATCH | /users/{userName}/removeSurvey/{surveyName} | USER_SURVEY_MAINTENCE | | assertNotSelfModification  | |
| GET | /roles | READ_ROLE | |  | |
| GET | /roles/{roleName} | READ_ROLE | |  | |
| GET | /roles/{roleName}/users | READ_ROLE | |  | |
| GET | /roles/{roleName}/admins | READ_ROLE | |  | |
| POST | /roles | CREATE_ROLE | |  | Not currently used by CCUI |
| PATCH | /roles/{roleName}/addPermission/{permissionName} | MAINTAIN_PERMISSIONS | |  | Not currently used by CCUI * |
| PATCH | /roles/{roleName}/removePermission/{permissionName} | MAINTAIN_PERMISSIONS | |  | Not currently used by CCUI * |

* The ability to create roles, add or remove permissions from those new roles, or the standing roles, is not currently supported by CCUI.
While it may be a feature of the UI in time, the initial standing roles and their permissions are maintained through Flyway migrations, and
were it necessary to modify those or create new roles in any environment, the use of curl to the appropriate endpoints, perhaps scripted, was 
going to be the teams fallback, should it ever be needed. Due to the growing number of fine grained permissions, and the need to correctly align
those permissions with any new role, the team was unsure if this was a feature that would be best exposed through the UI, even to a Super User,
as havoc could be wrought. Certianly we do not envisage giving this ability to a non Super User ie a developer, as it would require full understanding 
of how many and which permissions an ordinary user would need and an inadvertant addition of a poorly understood permission could similarly wreak havoc.

The application endpoints within the remaining CCSvc endpoint classes, make similar assertions:



| HTTP Method | Path | User Permission Required | Note |
| ----------------- | ---------------- | ---------------- | ---------------- |
| GET | /surveys | | asserts user is valid |
| GET | /surveys/usages | | asserts user is valid |
| GET | /addresses | SEARCH_CASES | |
| GET | /addresses/postcode | SEARCH_CASES | |
| GET | /cases | VIEW_CASE | |
| GET | /cases/attribute | SEARCH_CASES | |
| GET | /cases/ref | VIEW_CASE | |
| GET | /cases/.../launch | LAUNCH_EQ | |
| POST | /cases/.../fulfilment/post | REQUEST_POSTAL_FULFILMENT | |
| POST | /cases/.../refusal | REFUSE_CASE | |
| PUT | /cases | MODIFY_CASE | |
| POST | /cases/.../interaction | ADD_CASE_INTERACTION | |
| POST | /cases/.../enrol | ENROL_CASE | |
| GET | /interactions/user | READ_USER_INTERACTIONS | |



## DB Initialisation
Standing roles, with assigned permission, are currently setup by Flyway migrations, as well as standing user entries for the dev team members.
Due to the fine grained permissions each role requires, and the ability of the CCUI to show/hide features based on permissions, and therefor modify the
UI behaviour, the team felt that wide scale creation of roles with permissions would be open to abuse or misadventure by an uninformed user given that ability.
The ability to create roles and assign them permissions is not currently implemented - the team felt it prudent to delay that implementation until such a need
became apparent, instead relying on the ability of a script using curl to invoke the endpoints required. For the time being this would be a carefully orchestrated, developer lead initiative, should it arise.

The role names started as 'superuser', but later changed to 'Super User' as it was felt that the role names should be humanly readable, and the endpoints happily handle URL escaping to make this possible.

### Super User
- CAN_MANAGE_USERS
- CAN_MANAGE_SYSTEM
- CREATE_USER
- DELETE_USER
- READ_USER
- MODIFY_USER
- USER_SURVEY_MAINTENANCE
- USER_ROLE_ADMIN
- USER_ROLE_MAINTENANCE
- ADMIN_ROLE_MAINTENANCE
- CREATE_ROLE
- READ_ROLE
- MAINTAIN_PERMISSIONS
- READ_USER_INTERACTIONS
- READ_USER_AUDIT

### Enquiries Operator
- CAN_RECEIVE_INBOUND_CALLS
- SEARCH_CASES
- VIEW_CASE
- REFUSE_CASE
- REMOVE_CASE
- ENROL_CASE
- INVALIDATE_CASE
- MODIFY_CASE
- REQUEST_SMS_FULFILMENT
- REQUEST_POSTAL_FULFILMENT
- REQUEST_EMAIL_FULFILMENT
- ADD_CASE_INTERACTION

### Manager
- CAN_MANAGE_SYSTEM
- SEARCH_CASES
- VIEW_CASE
- REFUSE_CASE
- REMOVE_CASE
- ENROL_CASE
- INVALIDATE_CASE
- MODIFY_CASE
- LAUNCH_EQ
- CALL_RESPONDENT_ADHOC
- CALL_RESPONDENT_PRIORITISED
- CALL_RESPONDENT_SCHEDULED
- REQUEST_SMS_FULFILMENT
- REQUEST_POSTAL_FULFILMENT
- REQUEST_EMAIL_FULFILMENT
- ADD_CASE_INTERACTION

### Outbound Call Operator
- CAN_MAKE_OUTBOUND_CALLS
- SEARCH_CASES
- VIEW_CASE
- REFUSE_CASE
- REMOVE_CASE
- INVALIDATE_CASE
- LAUNCH_EQ
- CALL_RESPONDENT_PRIORITISED
- CALL_RESPONDENT_SCHEDULED
- REQUEST_SMS_FULFILMENT
- REQUEST_POSTAL_FULFILMENT
- REQUEST_EMAIL_FULFILMENT
- ADD_CASE_INTERACTION

### User Manager
- CAN_MANAGE_USERS
- CREATE_USER
- READ_USER
- DELETE_USER
- MODIFY_USER
- USER_SURVEY_MAINTENANCE
- USER_ROLE_MAINTENANCE
- READ_ROLE
- READ_USER_INTERACTIONS
- READ_USER_AUDIT
       




























