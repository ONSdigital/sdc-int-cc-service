package uk.gov.ons.ctp.integration.contactcentresvc.model;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "caze")
public class Case {

  @Id private UUID caseId;

  private Long caseRef;
  private String uprn;
  private String caseType;
}
