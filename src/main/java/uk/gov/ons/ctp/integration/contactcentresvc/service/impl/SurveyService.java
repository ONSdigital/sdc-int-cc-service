package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.SurveyRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserSurveyUsageRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SurveyDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SurveyUsageDTO;

@Service
@Slf4j
public class SurveyService {

  @Autowired private SurveyRepository surveyRepo;

  @Autowired private MapperFacade mapper;

  @Autowired private UserSurveyUsageRepository userSurveyUsageRepository;

  @Transactional
  public SurveyDTO getSurvey(UUID surveyId) throws CTPException {
    log.debug("Entering getSurvey");

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

  @Transactional
  public List<SurveyUsageDTO> getSurveyUsages() throws CTPException {
    log.debug("Entering getSurveyUsages");
    return mapper.mapAsList(userSurveyUsageRepository.findAll(), SurveyUsageDTO.class);
  }
}
