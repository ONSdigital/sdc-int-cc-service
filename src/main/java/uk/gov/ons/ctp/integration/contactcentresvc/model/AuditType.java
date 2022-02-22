package uk.gov.ons.ctp.integration.contactcentresvc.model;

import static uk.gov.ons.ctp.integration.contactcentresvc.model.AuditSubType.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

public enum AuditType {
  USER(CREATED, MODIFIED),
  USER_SURVEY_USAGE(ADDED, REMOVED),
  USER_ROLE(ADDED, REMOVED),
  ADMIN_ROLE(ADDED, REMOVED),
  ROLE(CREATED),
  PERMISSION(ADDED, REMOVED);

  @Getter private Set<AuditSubType> validSubTypes;

  @Getter boolean explicit;

  AuditType(AuditSubType... subTypes) {
    this.validSubTypes = new HashSet<>(Arrays.asList(subTypes));
  }
}
