package uk.gov.ons.ctp.integration.contactcentresvc.model;

import java.util.UUID;
import javax.persistence.Entity;
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
 * Representation of Operator-Role database link table. This supports many-to-many non-admin role
 * assignment for operators (users).
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
@Table(name = "operator_role")
public class OperatorRole {
  @ToString.Include @Id private UUID id;

  @ManyToOne(optional = false)
  private Operator operator;

  @ManyToOne(optional = false)
  private Role role;
}
