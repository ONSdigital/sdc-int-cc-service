package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ma.glasnost.orika.MapperFacade;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.SurveyRepositoryClient;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SurveyDTO;

@Service
public class SurveyService {
  
  @Autowired private SurveyRepositoryClient surveyRepoClient;

  @Autowired private MapperFacade mapper;

  
  public SurveyDTO getSurvey(UUID surveyId) throws CTPException {
    
    Survey survey = surveyRepoClient.getSurveyById(surveyId);
    
    SurveyDTO surveyDTO = mapper.map(survey, SurveyDTO.class);
    
    return surveyDTO;
  }
}
