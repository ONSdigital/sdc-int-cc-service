package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.SurveyRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SurveyDTO;

@Service
public class SurveyService {

  @Autowired private SurveyRepository surveyRepo;

  @Autowired private MapperFacade mapper;

  @Transactional
  public SurveyDTO getSurvey(UUID surveyId) throws CTPException {

    Survey survey =
        surveyRepo
            .findById(surveyId)
            .orElseThrow(
                () ->
                    new CTPException(
                        Fault.RESOURCE_NOT_FOUND, "Could not find survey for ID: " + surveyId));

    SurveyDTO surveyDTO = mapper.map(survey, SurveyDTO.class);

    return surveyDTO;
  }
}
