package uk.gov.ons.ctp.integration.contactcentresvc.client.caseService;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;

public class CaseContainerDTOCaseMappingTest {
  private CCSvcBeanMapper mapper = new CCSvcBeanMapper();
  private CaseContainerDTO caseContainerDTO = new CaseContainerDTO();
  private CaseDTO caseDTO;

  private void map() {
    caseDTO = mapper.map(caseContainerDTO, CaseDTO.class);
  }

  @Test
  public void regionTest() {
    map();
    assertEquals(null, caseDTO.getRegion());

    caseContainerDTO.setRegion("E12345678");
    map();
    assertEquals("E", caseDTO.getRegion());

    caseContainerDTO.setRegion("E");
    map();
    assertEquals("E", caseDTO.getRegion());

    caseContainerDTO.setRegion("");
    map();
    assertEquals("", caseDTO.getRegion());
  }
}
