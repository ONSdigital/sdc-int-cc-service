package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.ons.ctp.integration.contactcentresvc.endpoint.support.TransactionalOps;
import uk.gov.ons.ctp.integration.contactcentresvc.model.AuditSubType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.AuditType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserAuditDTO;

/** Integration test for user login. */
public class AuditSearchIT extends FullStackIntegrationTestBase {

  private static final String USER_SU = "SuperUser@ext.ons.gov.uk";
  private static String USER_TL = "TeamLeader@ext.ons.gov.uk";

  @Autowired private TransactionalOps txOps;

  @BeforeEach
  public void setup() throws MalformedURLException {
    super.init();

    txOps.deleteAll();

    // Bootstrap an initial user
    List<PermissionType> permissions =
        List.of(
            PermissionType.CREATE_USER,
            PermissionType.CREATE_ROLE,
            PermissionType.MAINTAIN_PERMISSIONS,
            PermissionType.RESERVED_ADMIN_ROLE_MAINTENANCE,
            PermissionType.RESERVED_USER_ROLE_ADMIN);
    Role role = txOps.createRole("creator", UUID.randomUUID(), permissions);
    txOps.createUser(USER_SU, UUID.randomUUID(), List.of(role), null);
  }

  @Test
  public void auditSearch() throws Exception {

    // @formatter:off
    // Build some audit history:
    //
    //  Principle    Target    Role     Audit   AuditSub    Audit
    //    user        user     Name     type      type      value
    // -----------+----------+-------+--------+----------+---------
    //  SuperUser      --     Admin   ROLE       CREATED        --
    //  SuperUser      --     Admin   PERMISSION ADDED    READ_USER_AUDIT
    //  SuperUser  TeamLeader   --    USER       CREATED        --
    //  SuperUser  TeamLeader Admin   USER_ROLE  ADDED          --
    // TeamLeader  TeamLeader   --    LOGIN       --            --
    // TeamLeader  TeamLeader   --    LOGOUT      --            --
    //
    // @formatter:on

    // Create group that allows invocation of audit endpoint
    roleEndpoint().invokeCreateRole(USER_SU, "Admin", "For administrators only");
    roleEndpoint().invokeAddPermission(USER_SU, "Admin", PermissionType.READ_USER_AUDIT);

    // Create a team leader and allow them to use the audit endpoint
    userEndpoint().invokeCreateUser(USER_SU, USER_TL);
    userEndpoint().addUserRole(USER_SU, USER_TL, "Admin");

    // Create some user activity
    userEndpoint().invokeLogin(USER_TL);
    userEndpoint().invokeLogout(USER_TL);

    // Now that there is audit history, check audit on what TeamLeader has done
    List<UserAuditDTO> audit = userEndpoint().invokeAudit(USER_TL, USER_TL, null).getBody();
    verifyAudit(audit, 0, USER_TL, USER_TL, null, AuditType.LOGOUT, null, null);
    verifyAudit(audit, 1, USER_TL, USER_TL, null, AuditType.LOGIN, null, null);
    assertEquals(2, audit.size());

    // Check audit on what TeamLeader has had done to them
    audit = userEndpoint().invokeAudit(USER_TL, null, USER_TL).getBody();
    verifyAudit(audit, 0, USER_TL, USER_TL, null, AuditType.LOGOUT, null, null);
    verifyAudit(audit, 1, USER_TL, USER_TL, null, AuditType.LOGIN, null, null);
    verifyAudit(audit, 2, USER_SU, USER_TL, "Admin", AuditType.USER_ROLE, AuditSubType.ADDED, null);
    verifyAudit(audit, 3, USER_SU, USER_TL, null, AuditType.USER, AuditSubType.CREATED, null);
    assertEquals(4, audit.size());

    // Check audit on what SuperUser has done
    audit = userEndpoint().invokeAudit(USER_TL, USER_SU, null).getBody();
    verifyAudit(audit, 0, USER_SU, USER_TL, "Admin", AuditType.USER_ROLE, AuditSubType.ADDED, null);
    verifyAudit(audit, 1, USER_SU, USER_TL, null, AuditType.USER, AuditSubType.CREATED, null);
    verifyAudit(
        audit,
        2,
        USER_SU,
        null,
        "Admin",
        AuditType.PERMISSION,
        AuditSubType.ADDED,
        "READ_USER_AUDIT");
    verifyAudit(audit, 3, USER_SU, null, "Admin", AuditType.ROLE, AuditSubType.CREATED, null);
    assertEquals(4, audit.size());

    // Confirm that there is on audit history on what the SuperUser has done
    audit = userEndpoint().invokeAudit(USER_TL, null, USER_SU).getBody();
    assertEquals(0, audit.size());
  }

  private void verifyAudit(
      List<UserAuditDTO> auditList,
      int index,
      String expectedPrincipalUserName,
      String expectedTargetUserName,
      String expectedTargetRoleName,
      AuditType expectedAuditType,
      AuditSubType expectedAuditSubType,
      String expectedAuditValue) {

    UserAuditDTO audit = auditList.get(index);

    assertEquals(expectedPrincipalUserName, audit.getPrincipalUserName());
    assertEquals(expectedTargetUserName, audit.getTargetUserName());
    assertEquals(expectedTargetRoleName, audit.getTargetRoleName());
    assertEquals(expectedAuditType, audit.getAuditType());
    assertEquals(expectedAuditSubType, audit.getAuditSubType());
    assertEquals(expectedAuditValue, audit.getAuditValue());
  }

  @Test
  public void noPermissionToAudit() throws Exception {

    // Create a team leader
    userEndpoint().invokeCreateUser(USER_SU, USER_TL);

    // Team leader attempts to use audit endpoint, but they don't have permission
    ResponseEntity<String> audit =
        userEndpoint().invokeAudit(HttpStatus.UNAUTHORIZED, USER_TL, USER_TL, null);
    assertTrue(
        audit.getBody().contains("User not authorised for activity READ_USER_AUDIT"),
        audit.getBody());
  }
}
