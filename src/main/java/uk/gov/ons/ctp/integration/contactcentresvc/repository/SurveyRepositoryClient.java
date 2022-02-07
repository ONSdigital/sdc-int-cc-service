package uk.gov.ons.ctp.integration.contactcentresvc.repository;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.SurveyRepository;

@Slf4j
@Service
public class SurveyRepositoryClient {
  
  private SurveyRepository surveyRepo;

  
  public SurveyRepositoryClient(SurveyRepository surveyRepo) {
    this.surveyRepo = surveyRepo;
  }

  public Survey getSurveyById(UUID surveyId) throws CTPException {
    log.debug("Find survey details by ID", kv("surveyId", surveyId));

    Survey survey =
        surveyRepo
            .findById(surveyId)
            .orElseThrow(
                () ->
                    new CTPException(
                        Fault.RESOURCE_NOT_FOUND, "Could not find survey for ID: " + surveyId));

    log.debug("Found survey details for ID", kv("surveyId", surveyId));
    
    return survey;
  }
}
