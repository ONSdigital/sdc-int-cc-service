package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.InvalidCase;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.InvalidateCaseRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;

@ExtendWith(MockitoExtension.class)
public class CaseServiceInvalidateCaseTest extends CaseServiceImplTestBase {

  private static final String A_NOTE = "Respondent is a boat";

  @Test
  public void invalidateCase() throws Exception {
    UUID caseId = UUID.randomUUID();
    String expectedResponseCaseId = caseId.toString();
    InvalidateCaseRequestDTO invalidateCaseRequestDTO =
        InvalidateCaseRequestDTO.builder().note(A_NOTE).build();

    ResponseDTO responseDTO = target.invalidateCase(caseId, invalidateCaseRequestDTO);

    InvalidCase invalidCasePayload = new InvalidCase();
    invalidCasePayload.setCaseId(caseId);
    invalidCasePayload.setReason(A_NOTE);

    verify(eventTransfer).send(TopicType.INVALID_CASE, invalidCasePayload);
    assertEquals(expectedResponseCaseId, responseDTO.getId());
  }
}
