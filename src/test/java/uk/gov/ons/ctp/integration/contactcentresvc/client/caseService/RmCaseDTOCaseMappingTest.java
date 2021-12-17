package uk.gov.ons.ctp.integration.contactcentresvc.client.caseService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import org.junit.jupiter.api.Test;
import uk.gov.ons.ctp.common.domain.Region;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.RmCaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;

public class RmCaseDTOCaseMappingTest {
  private CCSvcBeanMapper mapper = new CCSvcBeanMapper();
  private RmCaseDTO rmCaseDTO = new RmCaseDTO();
  private CaseDTO caseDTO;

  private void map() {
    caseDTO = mapper.map(rmCaseDTO, CaseDTO.class);
  }

  // TODO Fix Region mapping
  @Test
  public void regionTest() {
    rmCaseDTO.setSample(new HashMap<>());
    map();
    assertNull(caseDTO.getSample().get(CaseUpdate.ATTRIBUTE_REGION));

    rmCaseDTO.getSample().put(CaseUpdate.ATTRIBUTE_REGION, "E12345678");
    map();
    assertEquals(Region.E.name(), caseDTO.getSample().get(CaseUpdate.ATTRIBUTE_REGION));

    rmCaseDTO.getSample().put(CaseUpdate.ATTRIBUTE_REGION, "E");
    map();
    assertEquals(Region.E.name(), caseDTO.getSample().get(CaseUpdate.ATTRIBUTE_REGION));
  }
}
