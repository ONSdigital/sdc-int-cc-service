package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.lenient;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.event.model.RefusalDetails;
import uk.gov.ons.ctp.integration.contactcentresvc.model.RefusalType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.RefusalRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.ResponseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.CaseService;
import uk.gov.ons.ctp.integration.contactcentresvc.util.PgpDecrypt;
import uk.gov.ons.ctp.integration.contactcentresvc.util.PgpEncryptTest;

/** Unit Test {@link CaseService#reportRefusal(UUID, RefusalRequestDTO) reportRefusal}. */
@ExtendWith(MockitoExtension.class)
public class CaseServiceImplReportRefusalTest extends CaseServiceImplTestBase {

  private static final String PUBLIC_KEY_1 = "pgp/key1.asc";
  private static final String PUBLIC_KEY_2 = "pgp/key2.asc";
  private static final String PRIVATE_KEY_1 = "pgp/priv-key1.asc";
  private static final String PRIVATE_KEY_2 = "pgp/priv-key2.asc";

  @BeforeEach
  public void setup() {
    Resource pubKey1 = new ClassPathResource(PUBLIC_KEY_1);
    Resource pubKey2 = new ClassPathResource(PUBLIC_KEY_2);
    lenient().when(appConfig.getPublicPgpKey1()).thenReturn(pubKey1);
    lenient().when(appConfig.getPublicPgpKey2()).thenReturn(pubKey2);
  }

  @Test
  public void testRespondentRefusal_withHardReason() throws Exception {
    Date dateTime = new Date();
    doRespondentRefusalTest(dateTime, RefusalType.HARD_REFUSAL);
  }

  @Test
  public void testRespondentRefusal_withExtraordinaryReason() throws Exception {
    Date dateTime = new Date();
    doRespondentRefusalTest(dateTime, RefusalType.EXTRAORDINARY_REFUSAL);
  }

  @Test
  public void testRespondentRefusal_withSoftReason() throws Exception {
    Date dateTime = new Date();
    doRespondentRefusalTest(dateTime, RefusalType.SOFT_REFUSAL);
  }

  @Test
  public void shouldRequestRefusalWithMinimumFields() throws Exception {
    UUID caseId = UUID.randomUUID();
    String expectedResponseCaseId = caseId.toString();
    RefusalRequestDTO refusalPayload =
        RefusalRequestDTO.builder()
            .caseId(caseId)
            .reason(RefusalType.HARD_REFUSAL)
            .dateTime(new Date())
            .build();

    ResponseDTO refusalResponse = target.reportRefusal(caseId, refusalPayload);

    assertEquals(expectedResponseCaseId, refusalResponse.getId());

    RefusalDetails refusal = verifyEventSent(TopicType.REFUSAL, RefusalDetails.class);
    assertEquals(caseId, refusal.getCaseId());
    assertEquals("HARD_REFUSAL", refusal.getType());
  }

  private RefusalRequestDTO createRefusalDto(UUID caseId, Date dateTime, RefusalType reason) {
    return RefusalRequestDTO.builder().caseId(caseId).reason(reason).dateTime(dateTime).build();
  }

  private void doRespondentRefusalTest(Date dateTime, RefusalType reason) throws Exception {
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

    // This code is intentionally commented out. Reinstate if ccsvc uses encryption in outgoing events.
    // verifyEncryptedField("Jimmy McTavish", refusal.getName());

    // The following code exists to prevent complaints about unused code. It's never called.
    // This can be deleted once a final decision is reached about ccsvc encryption
    if (System.currentTimeMillis() == 1) {
      verifyEncryptedField("Jimmy McTavish", /*reason.getName()*/ "never-executed");
    }
  }

  private void verifyEncryptedField(String clear, String sendField) throws Exception {
    if (clear == null) {
      assertNull(sendField);
      return;
    }
    String privKey = PgpEncryptTest.readFileIntoString(PRIVATE_KEY_1);
    String pgpField = new String(Base64.getDecoder().decode(sendField), StandardCharsets.UTF_8);
    try (ByteArrayInputStream secretKeyFile = new ByteArrayInputStream(privKey.getBytes())) {
      String decrypted =
          PgpDecrypt.decrypt(secretKeyFile, pgpField, PgpEncryptTest.PASS_PHRASE.toCharArray());
      assertEquals(clear, decrypted);
    }

    String privKey2 = PgpEncryptTest.readFileIntoString(PRIVATE_KEY_2);
    String pgpField2 = new String(Base64.getDecoder().decode(sendField), StandardCharsets.UTF_8);
    try (ByteArrayInputStream secretKeyFile = new ByteArrayInputStream(privKey2.getBytes())) {
      String decrypted =
          PgpDecrypt.decrypt(secretKeyFile, pgpField2, PgpEncryptTest.PASS_PHRASE2.toCharArray());
      assertEquals(clear, decrypted);
    }
  }
}
