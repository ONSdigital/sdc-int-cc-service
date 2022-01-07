package uk.gov.ons.ctp.integration.contactcentresvc.model;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Representation of Case Interaction entity from database table.
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
@Table(name = "case_interaction")
public class CaseInteraction {

  @GeneratedValue @ToString.Include @Id private UUID id;

  @ToString.Include private UUID caseId;

  @ToString.Include private UUID ccuserId;

  private LocalDateTime createdDateTime;

  @Enumerated(EnumType.STRING)
  private CaseInteractionType type;

  @Enumerated(EnumType.STRING)
  private CaseSubInteractionType subtype;

  private String note;
}
