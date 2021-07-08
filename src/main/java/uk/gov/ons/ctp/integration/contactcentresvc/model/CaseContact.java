package uk.gov.ons.ctp.integration.contactcentresvc.model;

import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.event.model.Contact;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class CaseContact {
  private String title;
  private String forename;
  private String surname;
  private String telNo;

  public CaseContact(Contact eventContact) {
    title = eventContact.getTitle();
    forename = eventContact.getForename();
    surname = eventContact.getSurname();
    telNo = eventContact.getTelNo();
  }
}
