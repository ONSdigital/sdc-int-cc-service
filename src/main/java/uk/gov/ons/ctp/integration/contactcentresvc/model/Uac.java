package uk.gov.ons.ctp.integration.contactcentresvc.model;

import java.util.UUID;
import javax.persistence.Entity;
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
 * Representation of Uac entity from database table.
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
@Table(name = "uac")
public class Uac {

  @GeneratedValue @ToString.Include @Id private UUID id;

  @ToString.Include private UUID caseId;
  @ToString.Include private UUID collectionExerciseId;
  @ToString.Include private UUID surveyId;

  private int waveNum;

  private boolean receiptReceived;
  private boolean eqLaunched;
  private boolean active;

  private String questionnaire;
  private String uacHash;
  private String collectionInstrumentUrl;
}
