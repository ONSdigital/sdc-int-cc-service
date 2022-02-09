package uk.gov.ons.ctp.integration.contactcentresvc.model;

import java.util.UUID;
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
import org.hibernate.annotations.DynamicUpdate;

/**
 * Representation of Permission entity from database table. This represents a permission for a given
 * role.
 *
 * <ul>
 *   <li>A permission has a permission type
 *   <li>A role has many permissions
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
@DynamicUpdate
@Table(name = "permission")
public class Permission {
  @ToString.Include @Id private UUID id;

  @ToString.Include
  @Enumerated(EnumType.STRING)
  private PermissionType permissionType;

  @ManyToOne(optional = false)
  private Role role;
}
