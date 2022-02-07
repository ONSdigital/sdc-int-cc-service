package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SurveyDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.service.impl.SurveyService;

/** The REST controller to deal with Surveys */
@Slf4j
@Timed
@RestController
@RequestMapping(value = "/surveys", produces = "application/json")
public class SurveyEndpoint {
  private SurveyService surveyService;

  public SurveyEndpoint(SurveyService surveyService) {
    this.surveyService = surveyService;
  }

  @GetMapping("/{surveyId}")
  public ResponseEntity<SurveyDTO> survey(@PathVariable final UUID surveyId) throws CTPException {
    log.info("Entering GET survey by ID {}", kv("surveyId", surveyId));
    SurveyDTO result = surveyService.getSurvey(surveyId);
    return ResponseEntity.ok(result);
  }
}
