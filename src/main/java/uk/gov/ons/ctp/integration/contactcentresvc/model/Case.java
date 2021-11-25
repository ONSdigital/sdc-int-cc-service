package uk.gov.ons.ctp.integration.contactcentresvc.model;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Representation of Case entity from database table.
 *
 * <p>Implementation note: avoid Lombok Data annotation, since generated toString, equals and
 * hashcode are considered dangerous in combination with Entity annotation.
 */
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "collection_case")
public class Case {

  @ToString.Include @Id private UUID id;

  @ManyToOne(optional = false)
  private CollectionExercise collectionExercise;

  @ToString.Include private String caseRef;

  private boolean invalid;

  @Enumerated(EnumType.STRING)
  private RefusalType refusalReceived;

  private String questionnaire;
  private String sampleUnitRef;
  private String cohort;

  @Embedded private CaseAddress address;
  @Embedded private CaseContact contact;

  private LocalDateTime createdAt;
  private LocalDateTime lastUpdatedAt;
}
