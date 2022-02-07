package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.SurveyRepositoryClient;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SurveyDTO;

@Slf4j
@Service
public class SurveyService {

  @Autowired private SurveyRepositoryClient surveyRepoClient;

  public SurveyDTO getSurvey(UUID surveyId) throws CTPException {
    
    Survey survey = surveyRepoClient.getSurveyById(surveyId);
    
    return null;
  }
}
