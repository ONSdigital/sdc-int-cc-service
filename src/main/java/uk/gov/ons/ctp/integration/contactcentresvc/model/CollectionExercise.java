package uk.gov.ons.ctp.integration.contactcentresvc.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "collection_exercise")
public class CollectionExercise {
  @Id private UUID id;

  @ManyToOne(optional = false)
  private Survey survey;

  private String name;
  private String reference;
  private OffsetDateTime startDate;
  private OffsetDateTime endDate;
  private int cohortSchedule;
  private int cohorts;
  private int numberOfWaves;
  private int waveLength;
}
