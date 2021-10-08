package uk.gov.ons.ctp.integration.contactcentresvc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;

@ExtendWith(MockitoExtension.class)
public class CaseDataClientTest {
  private static final UUID ID = UUID.fromString("b7565b5e-1396-4965-91a2-918c0d3642ed");
  private static final Long UPRN = 334999999999L;
  private static final Long CASE_REF = 1000000000000001L;

  private Case caze;
  private CaseContainerDTO result;
  private List<CaseContainerDTO> resultList;
  private CTPException exception;

  @Mock private CaseRepository caseRepo;

  @InjectMocks private CaseDataClient target;

  @BeforeEach
  public void setup() {
    caze = FixtureHelper.loadPackageFixtures(Case[].class).get(0);
  }

  @Test
  public void shouldGetCaseById() throws Exception {
    when(caseRepo.findById(any())).thenReturn(Optional.of(caze));
    result = target.getCaseById(ID, false);
    assertEquals(ID, result.getId());
  }

  @Test
  public void shouldGetCaseByUprn() throws Exception {
    List<Case> cases = new ArrayList<>();
    cases.add(caze);
    when(caseRepo.findByAddressUprn(any())).thenReturn(cases);
    resultList = target.getCaseByUprn(UPRN, false);
    assertEquals(1, resultList.size());
    assertEquals(UPRN.toString(), resultList.get(0).getUprn());
  }

  @Test
  public void shouldGetCaseByRef() throws Exception {
    when(caseRepo.findByCaseRef(CASE_REF)).thenReturn(Optional.of(caze));
    result = target.getCaseByCaseRef(CASE_REF, false);
    assertEquals(CASE_REF.toString(), result.getCaseRef());
  }

  @Test
  public void shouldHandleCaseIdNotFound() throws Exception {
    when(caseRepo.findById(any())).thenReturn(Optional.empty());
    exception = assertThrows(CTPException.class, () -> target.getCaseById(ID, false));
    assertEquals(Fault.RESOURCE_NOT_FOUND, exception.getFault());
  }

  @Test
  public void shouldHandleEmptyResultsForGetCaseByUprn() throws Exception {
    when(caseRepo.findByAddressUprn(any())).thenReturn(new ArrayList<>());
    resultList = target.getCaseByUprn(UPRN, false);
    assertEquals(0, resultList.size());
  }

  @Test
  public void shouldHandleCaseRefNotFound() throws Exception {
    when(caseRepo.findByCaseRef(CASE_REF)).thenReturn(Optional.empty());
    exception = assertThrows(CTPException.class, () -> target.getCaseByCaseRef(CASE_REF, false));
    assertEquals(Fault.RESOURCE_NOT_FOUND, exception.getFault());
  }
}