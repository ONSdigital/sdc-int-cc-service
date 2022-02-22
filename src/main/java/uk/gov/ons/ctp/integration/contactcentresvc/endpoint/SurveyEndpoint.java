package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SurveyDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SurveyUsageDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.RBACService;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.SurveyService;

/** The REST controller to deal with Surveys */
@Slf4j
@Timed
@RestController
@RequestMapping(value = "/surveys", produces = "application/json")
public class SurveyEndpoint {
  private SurveyService surveyService;
  private RBACService rbacService;

  @Autowired
  public SurveyEndpoint(final SurveyService surveyService, final RBACService rbacService) {
    this.surveyService = surveyService;
    this.rbacService = rbacService;
  }

  @GetMapping("/{surveyId}")
  public ResponseEntity<SurveyDTO> survey(@PathVariable final UUID surveyId) throws CTPException {
    log.info("Entering GET survey by ID {}", kv("surveyId", surveyId));

    rbacService.assertUserValidAndActive();
    SurveyDTO result = surveyService.getSurvey(surveyId);

    return ResponseEntity.ok(result);
  }

  @GetMapping("/usages")
  @Transactional
  public ResponseEntity<List<SurveyUsageDTO>> getSurveyUsages() throws CTPException {

    log.info("Entering getSurveyUsages");
    rbacService.assertUserValidAndActive();

    return ResponseEntity.ok(surveyService.getSurveyUsages());
  }
}
