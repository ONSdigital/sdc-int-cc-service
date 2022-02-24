package uk.gov.ons.ctp.integration.contactcentresvc.model;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.*;
import lombok.*;

@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Data
@Table(name = "user_audit")
public class UserAudit {

  @ToString.Include @Id private UUID id;

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
