package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import uk.gov.ons.ctp.integration.contactcentresvc.model.Permission;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
  Permission findByPermissionTypeAndRole(PermissionType type, Role role);
  Permission findByPermissionTypeAndRoleAndSurvey(PermissionType type, Role role, Survey survey);
}
