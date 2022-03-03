package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.UserIdentityContext;
import uk.gov.ons.ctp.integration.contactcentresvc.model.*;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.RoleRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserAuditRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserRepository;

@ExtendWith(MockitoExtension.class)
public class UserAuditServiceTest {

  @Mock UserRepository userRepository;

  @Mock RoleRepository roleRepository;

  @Mock UserAuditRepository auditRepository;

  @InjectMocks private UserAuditService userAuditService = new UserAuditService();

  @Captor private ArgumentCaptor<UserAudit> auditCaptor;

  private final UUID PRINCIPAL_ID = UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");
  private final UUID TARGET_USER = UUID.fromString("c7565b5e-1396-4965-91a2-918c0d3642ed");
  private final UUID TARGET_ROLE = UUID.fromString("d7565b5e-1396-4965-91a2-918c0d3642ed");

  @Test
  public void correctUserIds() throws CTPException {
    when(userRepository.findByIdentity(UserIdentityContext.get()))
        .thenReturn(Optional.of(User.builder().id(PRINCIPAL_ID).build()));
    when(userRepository.findByIdentity("testUser"))
        .thenReturn(Optional.of(User.builder().id(TARGET_USER).build()));

    userAuditService.saveUserAudit(
        "testUser", null, AuditType.USER, AuditSubType.MODIFIED, "ACTIVE");

    verify(auditRepository).saveAndFlush(auditCaptor.capture());

    UserAudit capturedUserAudit = auditCaptor.getValue();

    UserAudit userAudit =
        UserAudit.builder()
            .ccuserId(PRINCIPAL_ID)
            .targetUserId(TARGET_USER)
            .targetRoleId(null)
            .auditType(AuditType.USER)
            .auditSubType(AuditSubType.MODIFIED)
            .auditValue("ACTIVE")
            .createdDateTime(capturedUserAudit.getCreatedDateTime())
            .build();

    assertEquals(userAudit, capturedUserAudit);
  }

  @Test
  public void correctRoleIds() throws CTPException {
    when(userRepository.findByIdentity(UserIdentityContext.get()))
        .thenReturn(Optional.of(User.builder().id(PRINCIPAL_ID).build()));
    when(roleRepository.findByName("testRole"))
        .thenReturn(Optional.of(Role.builder().id(TARGET_ROLE).build()));

    userAuditService.saveUserAudit(
        null,
        "testRole",
        AuditType.PERMISSION,
        AuditSubType.ADDED,
        PermissionType.READ_USER.name());

    verify(auditRepository).saveAndFlush(auditCaptor.capture());

    UserAudit capturedUserAudit = auditCaptor.getValue();

    UserAudit userAudit =
        UserAudit.builder()
            .ccuserId(PRINCIPAL_ID)
            .targetUserId(null)
            .targetRoleId(TARGET_ROLE)
            .auditType(AuditType.PERMISSION)
            .auditSubType(AuditSubType.ADDED)
            .auditValue(PermissionType.READ_USER.name())
            .createdDateTime(capturedUserAudit.getCreatedDateTime())
            .build();

    assertEquals(userAudit, capturedUserAudit);
  }

  @Test
  public void correctUserAndRoleIds() throws CTPException {
    when(userRepository.findByIdentity(UserIdentityContext.get()))
        .thenReturn(Optional.of(User.builder().id(PRINCIPAL_ID).build()));
    when(userRepository.findByIdentity("testUser"))
        .thenReturn(Optional.of(User.builder().id(TARGET_USER).build()));
    when(roleRepository.findByName("testRole"))
        .thenReturn(Optional.of(Role.builder().id(TARGET_ROLE).build()));

    userAuditService.saveUserAudit(
        "testUser", "testRole", AuditType.USER_ROLE, AuditSubType.ADDED, null);

    verify(auditRepository).saveAndFlush(auditCaptor.capture());

    UserAudit capturedUserAudit = auditCaptor.getValue();

    UserAudit userAudit =
        UserAudit.builder()
            .ccuserId(PRINCIPAL_ID)
            .targetUserId(TARGET_USER)
            .targetRoleId(TARGET_ROLE)
            .auditType(AuditType.USER_ROLE)
            .auditSubType(AuditSubType.ADDED)
            .auditValue(null)
            .createdDateTime(capturedUserAudit.getCreatedDateTime())
            .build();

    assertEquals(userAudit, capturedUserAudit);
  }

  @Test
  public void incorrectUserIds_NoUserId() throws CTPException {
    when(userRepository.findByIdentity(UserIdentityContext.get()))
        .thenReturn(Optional.of(User.builder().id(PRINCIPAL_ID).build()));

    CTPException exception =
        assertThrows(
            CTPException.class,
            () -> {
              userAuditService.saveUserAudit(
                  null, null, AuditType.USER, AuditSubType.MODIFIED, "ACTIVE");
            });

    assertEquals(CTPException.Fault.SYSTEM_ERROR, exception.getFault());
    assertEquals("Target userId not supplied", exception.getMessage());
    verify(auditRepository, times(0)).saveAndFlush(any());
  }

  @Test
  public void incorrectUserIds_UnwantedRoleId() {
    when(userRepository.findByIdentity(UserIdentityContext.get()))
        .thenReturn(Optional.of(User.builder().id(PRINCIPAL_ID).build()));
    when(userRepository.findByIdentity("testUser"))
        .thenReturn(Optional.of(User.builder().id(TARGET_USER).build()));
    when(roleRepository.findByName("testRole"))
        .thenReturn(Optional.of(Role.builder().id(TARGET_ROLE).build()));

    CTPException exception =
        assertThrows(
            CTPException.class,
            () -> {
              userAuditService.saveUserAudit(
                  "testUser", "testRole", AuditType.USER, AuditSubType.MODIFIED, "ACTIVE");
            });

    assertEquals(CTPException.Fault.SYSTEM_ERROR, exception.getFault());
    assertEquals("Target roleId supplied but not expected", exception.getMessage());
    verify(auditRepository, times(0)).saveAndFlush(any());
  }

  @Test
  public void incorrectRoleIds_NoRoleId() {
    when(userRepository.findByIdentity(UserIdentityContext.get()))
        .thenReturn(Optional.of(User.builder().id(PRINCIPAL_ID).build()));

    CTPException exception =
        assertThrows(
            CTPException.class,
            () -> {
              userAuditService.saveUserAudit(
                  null,
                  null,
                  AuditType.PERMISSION,
                  AuditSubType.ADDED,
                  PermissionType.READ_USER.name());
            });

    assertEquals(CTPException.Fault.SYSTEM_ERROR, exception.getFault());
    assertEquals("Target roleId not supplied", exception.getMessage());
    verify(auditRepository, times(0)).saveAndFlush(any());
  }

  @Test
  public void incorrectRoleIds_UnwantedUserId() {
    when(userRepository.findByIdentity(UserIdentityContext.get()))
        .thenReturn(Optional.of(User.builder().id(PRINCIPAL_ID).build()));
    when(userRepository.findByIdentity("testUser"))
        .thenReturn(Optional.of(User.builder().id(TARGET_USER).build()));
    when(roleRepository.findByName("testRole"))
        .thenReturn(Optional.of(Role.builder().id(TARGET_ROLE).build()));

    CTPException exception =
        assertThrows(
            CTPException.class,
            () -> {
              userAuditService.saveUserAudit(
                  "testUser",
                  "testRole",
                  AuditType.PERMISSION,
                  AuditSubType.ADDED,
                  PermissionType.READ_USER.name());
            });

    assertEquals(CTPException.Fault.SYSTEM_ERROR, exception.getFault());
    assertEquals("Target userId supplied but not expected", exception.getMessage());
    verify(auditRepository, times(0)).saveAndFlush(any());
  }

  @Test
  public void incorrectUserAndRoleIds_NoRoleId() {
    when(userRepository.findByIdentity(UserIdentityContext.get()))
        .thenReturn(Optional.of(User.builder().id(PRINCIPAL_ID).build()));
    when(userRepository.findByIdentity("testUser"))
        .thenReturn(Optional.of(User.builder().id(TARGET_USER).build()));

    CTPException exception =
        assertThrows(
            CTPException.class,
            () -> {
              userAuditService.saveUserAudit(
                  "testUser", null, AuditType.USER_ROLE, AuditSubType.ADDED, null);
            });

    assertEquals(CTPException.Fault.SYSTEM_ERROR, exception.getFault());
    assertEquals("Target roleId not supplied", exception.getMessage());
    verify(auditRepository, times(0)).saveAndFlush(any());
  }

  @Test
  public void incorrectUserAndRoleIds_NoUserId() {
    when(userRepository.findByIdentity(UserIdentityContext.get()))
        .thenReturn(Optional.of(User.builder().id(PRINCIPAL_ID).build()));
    when(roleRepository.findByName("testRole"))
        .thenReturn(Optional.of(Role.builder().id(TARGET_ROLE).build()));

    CTPException exception =
        assertThrows(
            CTPException.class,
            () -> {
              userAuditService.saveUserAudit(
                  null, "testRole", AuditType.USER_ROLE, AuditSubType.ADDED, null);
            });

    assertEquals(CTPException.Fault.SYSTEM_ERROR, exception.getFault());
    assertEquals("Target userId not supplied", exception.getMessage());
    verify(auditRepository, times(0)).saveAndFlush(any());
  }

  @Test
  public void incorrectUserAndRoleIds_NoId() {
    when(userRepository.findByIdentity(UserIdentityContext.get()))
        .thenReturn(Optional.of(User.builder().id(PRINCIPAL_ID).build()));
    CTPException exception =
        assertThrows(
            CTPException.class,
            () -> {
              userAuditService.saveUserAudit(
                  null, null, AuditType.USER_ROLE, AuditSubType.ADDED, null);
            });

    assertEquals(CTPException.Fault.SYSTEM_ERROR, exception.getFault());
    assertEquals("Target userId not supplied", exception.getMessage());
    verify(auditRepository, times(0)).saveAndFlush(any());
  }

  @Test
  public void auditNotSavedIfUserIdNotFound() {
    when(userRepository.findByIdentity(UserIdentityContext.get()))
        .thenReturn(Optional.of(User.builder().id(PRINCIPAL_ID).build()));
    when(userRepository.findByIdentity("testUser")).thenReturn(Optional.empty());
    CTPException exception =
        assertThrows(
            CTPException.class,
            () -> {
              userAuditService.saveUserAudit(
                  "testUser", null, AuditType.USER, AuditSubType.MODIFIED, "ACTIVE");
            });

    assertEquals(CTPException.Fault.BAD_REQUEST, exception.getFault());
    assertEquals("User not found", exception.getMessage());
    verify(auditRepository, times(0)).saveAndFlush(any());
  }

  @Test
  public void auditNotSavedIfRoleIdNotFound() {
    when(userRepository.findByIdentity(UserIdentityContext.get()))
        .thenReturn(Optional.of(User.builder().id(PRINCIPAL_ID).build()));
    when(roleRepository.findByName("testRole")).thenReturn(Optional.empty());
    CTPException exception =
        assertThrows(
            CTPException.class,
            () -> {
              userAuditService.saveUserAudit(
                  null, "testRole", AuditType.ROLE, AuditSubType.ADDED, null);
            });

    assertEquals(CTPException.Fault.BAD_REQUEST, exception.getFault());
    assertEquals("Role not found", exception.getMessage());
    verify(auditRepository, times(0)).saveAndFlush(any());
  }

  @Test
  public void auditSubTypeDoesNotMatchAuditType() {
    when(userRepository.findByIdentity(UserIdentityContext.get()))
        .thenReturn(Optional.of(User.builder().id(PRINCIPAL_ID).build()));
    when(userRepository.findByIdentity("targetUser"))
        .thenReturn(Optional.of(User.builder().id(TARGET_USER).build()));
    CTPException exception =
        assertThrows(
            CTPException.class,
            () -> {
              userAuditService.saveUserAudit(
                  "targetUser", null, AuditType.USER, AuditSubType.ADDED, null);
            });

    assertEquals(CTPException.Fault.SYSTEM_ERROR, exception.getFault());
    assertEquals("Unexpected AuditSubType for given AuditType", exception.getMessage());
    verify(auditRepository, times(0)).saveAndFlush(any());
  }
}
