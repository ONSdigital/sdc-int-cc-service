package uk.gov.ons.ctp.integration.contactcentresvc.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Column;
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
import org.hibernate.annotations.Type;

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

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private Map<String, String> sample;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private Map<String, String> sampleSensitive;

  private LocalDateTime createdAt;
  private LocalDateTime lastUpdatedAt;

  @Enumerated(EnumType.STRING)
  private CCStatus ccStatus;
}
