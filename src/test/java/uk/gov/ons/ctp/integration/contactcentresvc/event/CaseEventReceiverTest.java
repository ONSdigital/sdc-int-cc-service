package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.Region;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CaseUpdateSample;
import uk.gov.ons.ctp.common.event.model.CaseUpdateSampleSensitive;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseAddress;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseContact;
import uk.gov.ons.ctp.integration.contactcentresvc.model.RefusalType;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;

@ExtendWith(MockitoExtension.class)
public class CaseEventReceiverTest {
  private static final String CASE_ID = "ad24e36c-2a61-11ec-aa00-4c3275913db5";
  private static final String COLLECTION_EX_ID = "bdfc0ada-2a61-11ec-8c02-4c3275913db5";

  @Mock private CaseRepository repo;
  @Spy private MapperFacade mapper = new CCSvcBeanMapper();

  @InjectMocks private CaseEventReceiver target;

  @Captor private ArgumentCaptor<Case> caseCaptor;

  @Test
  public void shouldReceiveEvent() {
    CaseEvent caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);

    target.acceptCaseEvent(caseEvent);

    verify(repo).save(caseCaptor.capture());

    CaseUpdate ccase = caseEvent.getPayload().getCaseUpdate();
    Case caze = caseCaptor.getValue();
    verifyMappedCase(caze, ccase);
  }

  private void verifyMappedCase(Case caze, CaseUpdate ccase) {
    assertEquals(UUID.fromString(CASE_ID), caze.getId());
    assertEquals(UUID.fromString(COLLECTION_EX_ID), caze.getCollectionExercise().getId());
    assertTrue(caze.isInvalid());
    assertEquals(RefusalType.HARD_REFUSAL, caze.getRefusalReceived());

    CaseContact contact = caze.getContact();
    CaseAddress address = caze.getAddress();
    assertEquals(expectedContact(ccase.getSampleSensitive()), contact);
    assertEquals(expectedAddress(ccase.getSample()), address);
  }

  private CaseContact expectedContact(CaseUpdateSampleSensitive sensitive) {
    return CaseContact.builder().phoneNumber(sensitive.getPhoneNumber()).build();
  }

  private CaseAddress expectedAddress(CaseUpdateSample addr) {
    return CaseAddress.builder()
        .uprn(addr.getUprn())
        .addressLine1(addr.getAddressLine1())
        .addressLine2(addr.getAddressLine2())
        .addressLine3(addr.getAddressLine3())
        .townName(addr.getTownName())
        .postcode(addr.getPostcode())
        .region(Region.valueOf(addr.getRegion()))
        .build();
  }
}
