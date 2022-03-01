package uk.gov.ons.ctp.integration.contactcentresvc.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteractionType.CASE_NOTE_ADDED;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteractionType.FULFILMENT_REQUESTED;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteractionType.HARD_APPOINTMENT_MADE;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteractionType.MANUAL_CASE_VIEW;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteractionType.OUTBOUND_APPOINTMENT_CALL;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteractionType.REFUSAL_REQUESTED;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseSubInteractionType.CALL_NUMBER_ENGAGED;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseSubInteractionType.FULFILMENT_EMAIL;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseSubInteractionType.HARD_REFUSAL;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.CaseSubInteractionType.WITHDRAWAL_REFUSAL;

import org.junit.jupiter.api.Test;

public class CaseInteractionTypeTest {

  @Test
  public void shouldAcceptValidSubtype() {
    assertTrue(OUTBOUND_APPOINTMENT_CALL.isValidSubType(CALL_NUMBER_ENGAGED));
    assertTrue(REFUSAL_REQUESTED.isValidSubType(WITHDRAWAL_REFUSAL));
    assertTrue(FULFILMENT_REQUESTED.isValidSubType(FULFILMENT_EMAIL));
  }

  @Test
  public void shouldAcceptNullSubtypeWhenTypeHasNone() {
    assertTrue(HARD_APPOINTMENT_MADE.isValidSubType(null));
  }

  @Test
  public void shouldRejectInvalidSubtype() {
    assertFalse(OUTBOUND_APPOINTMENT_CALL.isValidSubType(HARD_REFUSAL));
    assertFalse(REFUSAL_REQUESTED.isValidSubType(CALL_NUMBER_ENGAGED));
    assertFalse(REFUSAL_REQUESTED.isValidSubType(null));
  }

  @Test
  public void shouldRejectAnySubtype() {
    assertFalse(MANUAL_CASE_VIEW.isValidSubType(HARD_REFUSAL));
    assertFalse(CASE_NOTE_ADDED.isValidSubType(HARD_REFUSAL));
  }
}
