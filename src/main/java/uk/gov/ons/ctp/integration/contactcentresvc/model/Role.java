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
 * Representation of Role entity from database table.
 *
 * <ul>
 *   <li>A role has a name
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
  @OneToMany(mappedBy = "role")
  private List<OperatorRole> operatorRoles;

  @JsonIgnore
  @OneToMany(mappedBy = "role")
  private List<AdminRole> adminRoles;

  @JsonIgnore
  @OneToMany(mappedBy = "role")
  private List<Permission> permissions;
}
