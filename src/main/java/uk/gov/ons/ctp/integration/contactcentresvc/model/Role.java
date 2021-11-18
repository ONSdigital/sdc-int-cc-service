package uk.gov.ons.ctp.integration.contactcentresvc.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Representation of Role entity from database table.
 *
 * <ul>
 *   <li>A role has a unique name
 *   <li>A role has many permissions
 *   <li>A role has many operator (non-admin) roles
 *   <li>A role has many admin roles
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
@Table(name = "role")
public class Role {
  @ToString.Include @Id private UUID id;
  @ToString.Include private String name;

  @JsonIgnore
  @ManyToMany(mappedBy = "operatorRoles", targetEntity = Operator.class)
  private List<Role> operatorRoles;

  @JsonIgnore
  @ManyToMany(mappedBy = "adminRoles", targetEntity = Operator.class)
  private List<Role> adminRoles;

  @JsonIgnore
  @OneToMany(mappedBy = "role")
  private List<Permission> permissions;
}
