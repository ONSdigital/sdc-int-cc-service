package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserSurveyUsageRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SurveyUsageDTO;

@Slf4j
@RestController
@RequestMapping(value = "/surveyusages", produces = "application/json")
public class SurveyUsageEndpoint {

  private MapperFacade mapper;

  private UserSurveyUsageRepository userSurveyUsageRepository;


  @Autowired
  public SurveyUsageEndpoint(
      final UserSurveyUsageRepository userSurveyUsageRepository,
      final MapperFacade mapper) {
    this.userSurveyUsageRepository = userSurveyUsageRepository;
    this.mapper = mapper;
  }

  @GetMapping
  @Transactional
  public ResponseEntity<List<SurveyUsageDTO>> getSurveyUsages() throws CTPException {

    // All users need to access this so no permission assertion

    List<SurveyUsageDTO> dtoList =
        mapper.mapAsList(userSurveyUsageRepository.findAll(), SurveyUsageDTO.class);
    return ResponseEntity.ok(dtoList);
  }
}
