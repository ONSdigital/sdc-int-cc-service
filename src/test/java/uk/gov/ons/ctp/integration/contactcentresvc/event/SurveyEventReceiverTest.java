package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import javax.persistence.PersistenceException;
import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.SurveyFulfilment;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.SurveyUpdateEvent;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Product;
import uk.gov.ons.ctp.integration.contactcentresvc.model.ProductType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.SurveyRepository;

@ExtendWith(MockitoExtension.class)
public class SurveyEventReceiverTest {

  @Mock private SurveyRepository repo;
  @Spy private MapperFacade mapper = new CCSvcBeanMapper();

  @InjectMocks private SurveyEventReceiver target;

  @Captor private ArgumentCaptor<Survey> surveyCaptor;

  private SurveyUpdateEvent event;

  @BeforeEach
  public void setup() {
    event = FixtureHelper.loadPackageFixtures(SurveyUpdateEvent[].class).get(0);
  }

  @Test
  public void shouldReceiveSurveyUpdateEvent() throws Exception {

    System.out.println(event.getPayload().getSurveyUpdate().getSampleDefinition());

    target.acceptEvent(event);

    verify(repo).saveAndFlush(surveyCaptor.capture());

    SurveyUpdate payload = event.getPayload().getSurveyUpdate();
    Survey survey = surveyCaptor.getValue();
    verifyMappedSurvey(survey, payload);
    verifyFulfilments(
        payload.getAllowedPrintFulfilments(),
        survey.getAllowedPrintFulfilments(),
        survey.getId(),
        ProductType.POSTAL);
    verifyFulfilments(
        payload.getAllowedSmsFulfilments(),
        survey.getAllowedSmsFulfilments(),
        survey.getId(),
        ProductType.SMS);
    verifyFulfilments(
        payload.getAllowedEmailFulfilments(),
        survey.getAllowedEmailFulfilments(),
        survey.getId(),
        ProductType.EMAIL);
  }

  @Test
  public void shouldRejectFailingSave() {
    when(repo.saveAndFlush(any())).thenThrow(PersistenceException.class);
    assertThrows(PersistenceException.class, () -> target.acceptEvent(event));
  }

  private void verifyMappedSurvey(Survey survey, SurveyUpdate surveyUpdate) {
    assertEquals(UUID.fromString(surveyUpdate.getSurveyId()), survey.getId());
    assertEquals(surveyUpdate.getName(), survey.getName());
    assertEquals(surveyUpdate.getSampleDefinitionUrl(), survey.getSampleDefinitionUrl());
    assertEquals(surveyUpdate.getSampleDefinition(), survey.getSampleDefinition());
  }

  private void verifyFulfilments(
      List<SurveyFulfilment> expectedFulfilments,
      List<Product> actualProducts,
      UUID expectedSurveyId,
      ProductType expectedProductType) {

    if (actualProducts == null) {
      assertNull(expectedFulfilments);
    }

    assertEquals(actualProducts.size(), expectedFulfilments.size());

    for (int i = 0; i < expectedFulfilments.size(); i++) {
      SurveyFulfilment expected = expectedFulfilments.get(i);
      Product actual = actualProducts.get(i);

      assertEquals(expected.getPackCode(), actual.getPackCode());
      assertEquals(expected.getDescription(), actual.getDescription());
      assertEquals(expected.getMetadata(), actual.getMetadata());

      assertEquals(expectedSurveyId, actual.getSurvey().getId());
      assertEquals(expectedProductType, actual.getType());
    }
  }
}
