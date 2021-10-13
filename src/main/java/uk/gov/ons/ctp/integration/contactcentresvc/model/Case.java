package uk.gov.ons.ctp.integration.contactcentresvc.model;

import java.time.OffsetDateTime;
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
@Table(name = "collection_case")
public class Case {

  @Id private UUID id;

  @ManyToOne(optional = false)
  private CollectionExercise collectionExercise;

  private String caseRef;

  private boolean invalid;

  @Enumerated(EnumType.STRING)
  private RefusalType refusalReceived;

  @Embedded private CaseAddress address;
  @Embedded private CaseContact contact;

  private OffsetDateTime createdAt;
  private OffsetDateTime lastUpdatedAt;
}
