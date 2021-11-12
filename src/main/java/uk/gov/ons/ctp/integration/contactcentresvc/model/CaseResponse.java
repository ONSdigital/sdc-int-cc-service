package uk.gov.ons.ctp.integration.contactcentresvc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

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
@Table(name = "case_response")
public class CaseResponse {

  @ToString.Include @Id private UUID id;

  @ToString.Include private UUID case_id;

  private int waveNum;

  private boolean receipt_received;
  private boolean eq_launched;
  private boolean active;

  private String questionnaire;
  private String uac_hash;

}
