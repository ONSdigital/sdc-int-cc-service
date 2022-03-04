package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.integration.contactcentresvc.model.AuditSubType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.AuditType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuditDTO {

    String principalUserName;
    
    String targetUserName;
    
    String targetRoleName;

    private Date createdDateTime;
    
    AuditType auditType;
    
    AuditSubType auditSubType;
    
    String auditValue;
}
