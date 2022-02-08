package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ma.glasnost.orika.MapperFacade;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.model.SurveyUsage;

public class UserDTOTest {
  private MapperFacade mapperFacade = new CCSvcBeanMapper();
  
  @Test
  public void foo() {
    
    User user = new User();
    List<Role> userRoles = new ArrayList<>();
    Role role = new Role();
    role.setId(UUID.randomUUID());
    userRoles.add(role);
    user.setUserRoles(userRoles);
    List<SurveyUsage> surveyUsageList = new ArrayList<>();
    SurveyUsage usu = new SurveyUsage();
    usu.setSurveyType(SurveyType.SOCIAL);
    surveyUsageList.add(usu);
    user.setSurveyUsages(surveyUsageList);
    user.setId(UUID.randomUUID());
    
    UserDTO userDTO = mapperFacade.map(user,  UserDTO.class);
    assert(userDTO.getSurveyUsages().stream().anyMatch(su -> su.getSurveyType().equals(SurveyType.SOCIAL)));
  }

}
