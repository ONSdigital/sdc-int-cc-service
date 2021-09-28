package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseAddress;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseContact;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;

@ExtendWith(MockitoExtension.class)
public class CaseEventReceiverTest {

  @Mock private CaseRepository repo;

  @InjectMocks private CaseEventReceiver target;

  @Captor private ArgumentCaptor<Case> caseCaptor;

  @Test
  public void shouldReceiveEvent() {
    Header header = new Header();
    header.setMessageId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    CaseEvent caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
    caseEvent.setHeader(header);
    target.acceptCaseEvent(caseEvent);

    verify(repo).save(caseCaptor.capture());

    CollectionCase ccase = caseEvent.getPayload().getCollectionCase();

    Case caze = caseCaptor.getValue();
    verifyMappedCase(caze, ccase);
  }

  private void verifyMappedCase(Case caze, CollectionCase ccase) {
    CaseContact contact = caze.getContact();
    CaseAddress address = caze.getAddress();
    assertEquals(expectedContact(ccase.getContact()), contact);
    assertEquals(expectedAddress(ccase.getAddress()), address);

    assertEquals(OffsetDateTime.parse("2020-06-08T07:28:45.113Z"), caze.getCreatedDateTime());

    assertEquals(UUID.fromString("0000089e-c6ef-46cb-9f09-b4def5a6d2d1"), caze.getId());
    assertEquals(Long.valueOf(ccase.getCaseRef()), caze.getCaseRef());
    assertEquals(CaseType.valueOf(ccase.getCaseType()), caze.getCaseType());
    assertEquals(ccase.getSurvey(), caze.getSurvey());
    assertEquals(UUID.fromString(ccase.getCollectionExerciseId()), caze.getCollectionExerciseId());
    assertEquals(ccase.getActionableFrom(), caze.getActionableFrom());
    assertTrue(caze.isHandDelivery());
    assertFalse(caze.isAddressInvalid());
    assertEquals(ccase.getCeExpectedCapacity(), caze.getCeExpectedCapacity());
  }

  private CaseContact expectedContact(Contact contact) {
    return CaseContact.builder()
        .forename(contact.getForename())
        .surname(contact.getSurname())
        .title(contact.getTitle())
        .telNo(contact.getTelNo())
        .build();
  }

  private CaseAddress expectedAddress(Address addr) {
    return CaseAddress.builder()
        .uprn(addr.getUprn())
        .addressLine1(addr.getAddressLine1())
        .addressLine2(addr.getAddressLine2())
        .addressLine3(addr.getAddressLine3())
        .townName(addr.getTownName())
        .postcode(addr.getPostcode())
        .region(addr.getRegion())
        .estabType(addr.getEstabType())
        .organisationName(addr.getOrganisationName())
        .latitude(addr.getLatitude())
        .longitude(addr.getLongitude())
        .estabUprn(addr.getEstabUprn())
        .addressType(addr.getAddressType())
        .addressLevel(addr.getAddressLevel())
        .build();
  }
}
