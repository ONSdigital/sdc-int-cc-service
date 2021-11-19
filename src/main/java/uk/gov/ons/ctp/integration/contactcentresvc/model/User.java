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
 * Representation of User (operator) entity from database table.
 *
 * <ul>
 *   <li>A user has a unique name
 *   <li>A user has many non-admin) user-roles
 *   <li>A user has many admin-roles
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
public class User {
  @ToString.Include @Id private UUID id;
  @ToString.Include private String name;
  @ToString.Include private boolean active;

  @JsonIgnore
  @ManyToMany
  @JoinTable(
      name = "operator_role",
      joinColumns = @JoinColumn(name = "operator_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private List<Role> userRoles;

  @JsonIgnore
  @ManyToMany
  @JoinTable(
      name = "admin_role",
      joinColumns = @JoinColumn(name = "operator_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private List<Role> adminRoles;
}
