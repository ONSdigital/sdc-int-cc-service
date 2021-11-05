package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.RefusalDetails;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.Reason;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;

/** Unit Test {@link CaseService#reportRefusal(UUID, RefusalRequestDTO) reportRefusal}. */
@ExtendWith(MockitoExtension.class)
public class CaseServiceImplReportRefusalTest extends CaseServiceImplTestBase {

  @Test
  public void testRespondentRefusal_withHardReason() throws Exception {
    Date dateTime = new Date();
    doRespondentRefusalTest(dateTime, Reason.HARD_REFUSAL);
  }

  @Test
  public void testRespondentRefusal_withExtraordinaryReason() throws Exception {
    Date dateTime = new Date();
    doRespondentRefusalTest(dateTime, Reason.EXTRAORDINARY_REFUSAL);
  }

  @Test
  public void testRespondentRefusal_withSoftReason() throws Exception {
    Date dateTime = new Date();
    doRespondentRefusalTest(dateTime, Reason.SOFT_REFUSAL);
  }

  @Test
  public void shouldRequestRefusalWithMinimumFields() throws Exception {
    UUID caseId = UUID.randomUUID();
    String expectedResponseCaseId = caseId.toString();
    RefusalRequestDTO refusalPayload =
        RefusalRequestDTO.builder()
            .caseId(caseId)
            .reason(Reason.HARD_REFUSAL)
            .dateTime(new Date())
            .build();

    ResponseDTO refusalResponse = target.reportRefusal(caseId, refusalPayload);

    assertEquals(expectedResponseCaseId, refusalResponse.getId());

    RefusalDetails refusal = verifyEventSent(TopicType.REFUSAL, RefusalDetails.class);
    assertEquals(caseId, refusal.getCaseId());
    assertEquals("HARD_REFUSAL", refusal.getType());
  }

  private RefusalRequestDTO createRefusalDto(UUID caseId, Date dateTime, Reason reason) {
    return RefusalRequestDTO.builder()
        .caseId(caseId)
        .reason(reason)
        .dateTime(dateTime)
        .build();
  }

  private void doRespondentRefusalTest(Date dateTime, Reason reason) throws Exception {
    UUID caseId = UUID.randomUUID();
    UUID expectedEventCaseId = caseId;
    String expectedResponseCaseId = caseId.toString();
    RefusalRequestDTO refusalPayload = createRefusalDto(expectedEventCaseId, dateTime, reason);

    // report the refusal
    long timeBeforeInvocation = System.currentTimeMillis();
    ResponseDTO refusalResponse = target.reportRefusal(caseId, refusalPayload);
    long timeAfterInvocation = System.currentTimeMillis();

    // Validate the response to the refusal
    assertEquals(expectedResponseCaseId, refusalResponse.getId());
    verifyTimeInExpectedRange(
        timeBeforeInvocation, timeAfterInvocation, refusalResponse.getDateTime());

    // Validate payload of published event
    RefusalDetails refusal = verifyEventSent(TopicType.REFUSAL, RefusalDetails.class);
    assertEquals(expectedEventCaseId, refusal.getCaseId());
    assertEquals(reason.name(), refusal.getType());
  }
}
