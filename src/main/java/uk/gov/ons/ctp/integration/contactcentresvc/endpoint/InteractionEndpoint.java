package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import io.micrometer.core.annotation.Timed;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UsersCaseInteractionDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.InteractionService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.RBACService;

/** The REST controller for ContactCentreSvc find interactions end points */
@Slf4j
@Timed
@RestController
@RequestMapping(value = "/interactions", produces = "application/json")
public class InteractionEndpoint {

  private InteractionService interactionService;
  private RBACService rbacService;

  /**
   * Constructor for ContactCentreDataEndpoint
   *
   * @param rbacService performs permission checking
   * @param interactionService is a service layer object that we be doing the processing on behalf
   *     of this endpoint.
   */
  @Autowired
  public InteractionEndpoint(
      final RBACService rbacService, final InteractionService interactionService) {
    this.rbacService = rbacService;
    this.interactionService = interactionService;
  }

  @RequestMapping(value = "/user/{userIdentity}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<List<UsersCaseInteractionDTO>> getCaseInteractionsForUser(
      @PathVariable(value = "userIdentity") @Valid @Email final String userIdentity)
      throws CTPException {

    log.info("Entering GET getCaseInteractionsForUser", kv("pathParam", userIdentity));

    rbacService.assertUserPermission(PermissionType.READ_USER_INTERACTIONS);

    List<UsersCaseInteractionDTO> response =
        interactionService.getAllCaseInteractionsForUser(userIdentity);
    return ResponseEntity.ok(response);
  }
}
