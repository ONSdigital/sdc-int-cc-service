package uk.gov.ons.ctp.integration.contactcentresvc.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
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
 * <ul>
 *   <li>An operator has a unique name
 *   <li>An operator has many operator (non-admin) roles
 *   <li>An operator has many admin roles
 * </ul>
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

  @Builder.Default @ToString.Include private boolean active = true;

  @JsonIgnore
  @ManyToMany
  @JoinTable(
      name = "operator_role",
      joinColumns = @JoinColumn(name = "operator_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private List<Role> operatorRoles;

  @JsonIgnore
  @ManyToMany
  @JoinTable(
      name = "admin_role",
      joinColumns = @JoinColumn(name = "operator_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private List<Role> adminRoles;
}
