package uk.gov.ons.ctp.integration.contactcentresvc.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Representation of Operator (user) entity from database table.
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
@Table(name = "operator")
public class Operator {
  @ToString.Include @Id private UUID id;
  @ToString.Include private String name;
  @ToString.Include private boolean active;

  @JsonIgnore
  @OneToMany(mappedBy = "operator")
  private List<OperatorRole> memberRoles;

  @JsonIgnore
  @OneToMany(mappedBy = "operator")
  private List<AdminRole> adminRoles;
}
