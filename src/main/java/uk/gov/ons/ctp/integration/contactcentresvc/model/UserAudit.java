package uk.gov.ons.ctp.integration.contactcentresvc.model;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Data
@Table(name = "user_audit")
public class UserAudit {

  @GeneratedValue @ToString.Include @Id private UUID id;

  @ToString.Include private UUID ccuserId;

  @ToString.Include private UUID targetUserId;

  @ToString.Include private UUID targetRoleId;

  private LocalDateTime createdDateTime;

  @ToString.Include
  @Enumerated(EnumType.STRING)
  private AuditType auditType;

  @ToString.Include
  @Enumerated(EnumType.STRING)
  private AuditSubType auditSubType;

  @ToString.Include private String auditValue;
}
