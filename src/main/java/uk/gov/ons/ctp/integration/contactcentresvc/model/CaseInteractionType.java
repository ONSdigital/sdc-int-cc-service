package uk.gov.ons.ctp.integration.contactcentresvc.model;

import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseSubInteractionType.CALL_ANSWERED;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseSubInteractionType.CALL_NUMBER_ENGAGED;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseSubInteractionType.CALL_NUMBER_UNOBTAINABLE;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseSubInteractionType.CALL_UNANSWERED;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseSubInteractionType.CALL_VOICEMAIL;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseSubInteractionType.FULFILMENT_EMAIL;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseSubInteractionType.FULFILMENT_PRINT;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseSubInteractionType.FULFILMENT_SMS;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

public enum CaseInteractionType {
  // implicit interactions are those resulting from an indirect UI action
  MANUAL_CASE_VIEW(false),
  SCHEDULED_CASE_VIEW(false),
  SOFT_APPOINTMENT_MADE(false),
  HARD_APPOINTMENT_MADE(false),
  REFUSAL_REQUESTED(false),
  CASE_UPDATE_REQUESTED(false),
  CONTACT_UPDATE_REQUESTED(false),
  DATA_REMOVAL_REQUESTED(false),
  TELEPHONE_CAPTURE_STARTED(false),

  FULFILMENT_REQUESTED(false, FULFILMENT_PRINT, FULFILMENT_SMS, FULFILMENT_EMAIL),

  // explicit interactions are those the UI will choose
  CASE_NOTE_ADDED(true),

  OUTBOUND_APPOINTMENT_CALL(
      true,
      CALL_NUMBER_ENGAGED,
      CALL_NUMBER_UNOBTAINABLE,
      CALL_ANSWERED,
      CALL_UNANSWERED,
      CALL_VOICEMAIL),

  OUTBOUND_PRIORITISED_CALL(
      true,
      CALL_NUMBER_ENGAGED,
      CALL_NUMBER_UNOBTAINABLE,
      CALL_ANSWERED,
      CALL_UNANSWERED,
      CALL_VOICEMAIL),

  OUTBOUND_MANUAL_CALL(
      true,
      CALL_NUMBER_ENGAGED,
      CALL_NUMBER_UNOBTAINABLE,
      CALL_ANSWERED,
      CALL_UNANSWERED,
      CALL_VOICEMAIL);

  @Getter private Set<CaseSubInteractionType> validSubInteractions;

  @Getter boolean explicit;

  private CaseInteractionType(boolean explicit, CaseSubInteractionType... subInteractions) {
    this.explicit = explicit;
    this.validSubInteractions = Arrays.asList(subInteractions).stream().collect(Collectors.toSet());
  }
}
