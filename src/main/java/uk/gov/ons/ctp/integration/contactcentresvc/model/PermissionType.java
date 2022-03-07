package uk.gov.ons.ctp.integration.contactcentresvc.model;

public enum PermissionType {
  // All of the permissions starting 'CAN_' are indicators to the UI that a user is allowed to
  // perform a wider
  // set of operations, to enable it to choose to show/hide coarser grained options, without having
  // to second guess based on
  // one of the finer permissions which for all it knows may shift and change
  CAN_RECEIVE_INBOUND_CALLS,
  CAN_MAKE_OUTBOUND_CALLS,
  CAN_MANAGE_USERS,
  CAN_MANAGE_SYSTEM,

  SEARCH_CASES,
  VIEW_CASE,
  REFUSE_CASE,
  REMOVE_CASE,
  INVALIDATE_CASE,
  MODIFY_CASE,
  ENROL_CASE,
  LAUNCH_EQ,
  CALL_RESPONDENT_ADHOC,
  CALL_RESPONDENT_PRIORITISED,
  CALL_RESPONDENT_SCHEDULED,
  REQUEST_SMS_FULFILMENT,
  REQUEST_POSTAL_FULFILMENT,
  REQUEST_EMAIL_FULFILMENT,
  ADD_CASE_INTERACTION,

  CREATE_USER,
  READ_USER,
  MODIFY_USER,
  USER_SURVEY_MAINTENANCE,
  USER_ROLE_MAINTENANCE,
  CREATE_ROLE,
  READ_ROLE,
  MAINTAIN_PERMISSIONS,
  READ_USER_AUDIT,

  // RESERVED_ permisions are those which we only want to assign to superusers
  // naming this way to call them out from other permissions
  RESERVED_USER_ROLE_ADMIN,
  RESERVED_ADMIN_ROLE_MAINTENANCE
}
