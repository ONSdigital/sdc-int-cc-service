package uk.gov.ons.ctp.integration.contactcentresvc.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "caze")
public class Case {

  @Id private UUID caseId;

  private Long caseRef;
  private String caseType;
  private String survey;
  private UUID collectionExerciseId;
  private String actionableFrom;
  private boolean handDelivery;
  private boolean addressInvalid;
  private Integer ceExpectedCapacity;

  @Embedded private CaseContact contact;

  @Embedded private CaseAddress address;

  private OffsetDateTime createdDateTime;
}
