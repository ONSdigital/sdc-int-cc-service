package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.UserSurveyUsageRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SurveyUsageDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.security.UserIdentityHelper;

@Slf4j
// @Timed
@RestController
@RequestMapping(value = "/surveyusages", produces = "application/json")
public class SurveyUsageEndpoint {

  private MapperFacade mapper;

  private UserSurveyUsageRepository userSurveyUsageRepository;

  private UserIdentityHelper identityHelper;

  @Autowired
  public SurveyUsageEndpoint(
      final UserSurveyUsageRepository userSurveyUsageRepository,
      final MapperFacade mapper,
      final UserIdentityHelper identityHelper) {
    this.userSurveyUsageRepository = userSurveyUsageRepository;
    this.mapper = mapper;
    this.identityHelper = identityHelper;
  }

  @GetMapping
  @Transactional
  public ResponseEntity<List<SurveyUsageDTO>> getSurveyUsages(
      @Value("#{request.getAttribute('principal')}") String principal) throws CTPException {

    // All users need to access this so no permission assertion

    List<SurveyUsageDTO> dtoList =
        mapper.mapAsList(userSurveyUsageRepository.findAll(), SurveyUsageDTO.class);
    return ResponseEntity.ok(dtoList);
  }
}
