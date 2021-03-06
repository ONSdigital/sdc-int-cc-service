package uk.gov.ons.ctp.integration.contactcentresvc.model;

public enum CaseSubInteractionType {
  CALL_NUMBER_ENGAGED,
  CALL_NUMBER_UNOBTAINABLE,
  CALL_ANSWERED,
  CALL_UNANSWERED,
  CALL_VOICEMAIL,

  FULFILMENT_PRINT,
  FULFILMENT_SMS,
  FULFILMENT_EMAIL,

  HARD_REFUSAL,
  EXTRAORDINARY_REFUSAL,
  SOFT_REFUSAL,
  WITHDRAWAL_REFUSAL
}
