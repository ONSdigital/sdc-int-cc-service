package uk.gov.ons.ctp.integration.contactcentresvc.model;

import static uk.gov.ons.ctp.integration.contactcentresvc.model.AuditSubType.ADDED;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.AuditSubType.CREATED;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.AuditSubType.MODIFIED;
import static uk.gov.ons.ctp.integration.contactcentresvc.model.AuditSubType.REMOVED;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;

public enum AuditType {
  USER(new AuditTarget[] {AuditTarget.USER}, CREATED, MODIFIED, REMOVED),
  USER_SURVEY_USAGE(new AuditTarget[] {AuditTarget.USER}, ADDED, REMOVED),
  USER_ROLE(new AuditTarget[] {AuditTarget.USER, AuditTarget.ROLE}, ADDED, REMOVED),
  ADMIN_ROLE(new AuditTarget[] {AuditTarget.USER, AuditTarget.ROLE}, ADDED, REMOVED),
  ROLE(new AuditTarget[] {AuditTarget.ROLE}, CREATED),
  PERMISSION(new AuditTarget[] {AuditTarget.ROLE}, ADDED, REMOVED),
  LOGIN(new AuditTarget[] {AuditTarget.ROLE}),
  LOGOUT(new AuditTarget[] {AuditTarget.ROLE});

  @Getter private Set<AuditSubType> validSubTypes;

  @Getter private AuditTarget[] targets;

  AuditType(AuditTarget[] targets, AuditSubType... subTypes) {
    this.targets = targets;
    this.validSubTypes = new HashSet<>(Arrays.asList(subTypes));
  }
}
