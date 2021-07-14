package uk.gov.ons.ctp.integration.contactcentresvc.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.time.DateTimeUtil;

/**
 * Representation of Case entity from database table.
 *
 * <p>Implementation note: avoid Lombok Data annotation, since generated toString, equals and
 * hashcode are dangerous in combination with Entity annotation.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "caze")
public class Case {

  @Id private UUID id;

  private Long caseRef;

  @Enumerated(EnumType.STRING)
  private CaseType caseType;

  private String survey;
  private UUID collectionExerciseId;
  private String actionableFrom;
  private boolean handDelivery;
  private boolean addressInvalid;
  private Integer ceExpectedCapacity;

  @Embedded private CaseContact contact;

  @Embedded private CaseAddress address;

  @JsonFormat(pattern = DateTimeUtil.DATE_FORMAT_IN_JSON, timezone = "UTC")
  private OffsetDateTime createdDateTime;
}
