package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseContact;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;

@ExtendWith(MockitoExtension.class)
public class CaseEventReceiverTest {

  @Mock private CaseRepository repo;

  @InjectMocks private CaseEventReceiver target;

  @Captor private ArgumentCaptor<Case> caseCaptor;

  @Test
  public void dummy() {}

  @Test
  public void shouldReceiveEvent() throws Exception {
    CaseEvent caseEvent = FixtureHelper.loadPackageFixtures(CaseEvent[].class).get(0);
    target.acceptCaseEvent(caseEvent);

    verify(repo).save(caseCaptor.capture());

    CollectionCase ccase = caseEvent.getPayload().getCollectionCase();

    Case caze = caseCaptor.getValue();
    CaseContact contact = caze.getContact();
    assertEquals(expectedContact(ccase.getContact()), contact);
  }

  private CaseContact expectedContact(Contact contact) {
    return CaseContact.builder()
        .forename(contact.getForename())
        .surname(contact.getSurname())
        .title(contact.getTitle())
        .telNo(contact.getTelNo())
        .build();
  }
}
