package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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
import uk.gov.ons.ctp.integration.contactcentresvc.model.DeliveryChannel;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Product;
import uk.gov.ons.ctp.integration.contactcentresvc.model.ProductGroup;
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

    target.acceptEvent(event);

    verify(repo).saveAndFlush(surveyCaptor.capture());

    SurveyUpdate payload = event.getPayload().getSurveyUpdate();
    Survey survey = surveyCaptor.getValue();
    verifyMappedSurvey(survey, payload);
    verifyFulfilments(
        payload.getAllowedPrintFulfilments(),
        filterProducts(survey, DeliveryChannel.POST),
        survey.getId(),
        DeliveryChannel.POST);
    verifyFulfilments(
        payload.getAllowedSmsFulfilments(),
        filterProducts(survey, DeliveryChannel.SMS),
        survey.getId(),
        DeliveryChannel.SMS);
    verifyFulfilments(
        payload.getAllowedEmailFulfilments(),
        filterProducts(survey, DeliveryChannel.EMAIL),
        survey.getId(),
        DeliveryChannel.EMAIL);
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
      DeliveryChannel expectedDeliveryChannel) {

    if (actualProducts == null) {
      assertNull(expectedFulfilments);
    }

    assertEquals(actualProducts.size(), expectedFulfilments.size());

    for (int i = 0; i < actualProducts.size(); i++) {
      Product actual = actualProducts.get(i);
      SurveyFulfilment expected = expectedFulfilments.get(i);

      assertEquals(expected.getPackCode(), actual.getPackCode());
      assertEquals(expected.getDescription(), actual.getDescription());
      assertEquals(expected.getMetadata(), actual.getMetadata());

      assertEquals(expectedSurveyId, actual.getSurvey().getId());
      assertEquals(expectedDeliveryChannel, actual.getDeliveryChannel());
      assertEquals(ProductGroup.UAC, actual.getProductGroup());
    }
  }

  private List<Product> filterProducts(Survey survey, DeliveryChannel targetDeliveryChannel) {
    return survey.getAllowedFulfilments().stream()
        .filter(f -> f.getDeliveryChannel() == targetDeliveryChannel)
        .collect(Collectors.toList());
  }
}
