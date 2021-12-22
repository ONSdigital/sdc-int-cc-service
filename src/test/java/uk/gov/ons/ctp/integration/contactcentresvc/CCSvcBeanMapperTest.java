package uk.gov.ons.ctp.integration.contactcentresvc;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.Test;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.SurveyFulfilment;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.RmCaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressCompositeDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Product;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Uac;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;

public class CCSvcBeanMapperTest {

  private MapperFacade mapperFacade = new CCSvcBeanMapper();

  @Test
  public void shouldMapCase_to_RmCaseDTO() {
    Case source = FixtureHelper.loadClassFixtures(Case[].class).get(0);
    RmCaseDTO destination = mapperFacade.map(source, RmCaseDTO.class);
    assertAll(
        () -> assertEquals(source.getId(), destination.getId()),
        () -> assertEquals(source.getCaseRef(), destination.getCaseRef()),
        () -> assertEquals(source.getSample(), destination.getSample()));
  }

  @Test
  public void shouldMapCase_to_CaseDTO() {
    Case source = FixtureHelper.loadClassFixtures(Case[].class).get(0);
    CaseDTO destination = mapperFacade.map(source, CaseDTO.class);
    assertAll(
        () -> assertEquals(source.getId(), destination.getId()),
        () -> assertEquals(source.getCaseRef(), destination.getCaseRef()),
        () -> assertEquals(source.getSample(), destination.getSample()),
        () -> assertEquals(source.getSampleSensitive(), destination.getSampleSensitive()));
  }

  @Test
  public void shouldMapCaseUpdate_to_Case() {
    CaseUpdate source = FixtureHelper.loadClassFixtures(CaseUpdate[].class).get(0);
    Case destination = mapperFacade.map(source, Case.class);
    assertAll(
        () -> assertEquals(source.getCaseId(), destination.getId().toString()),
        () ->
            assertEquals(
                source.getCollectionExerciseId(),
                destination.getCollectionExercise().getId().toString()),
        () -> assertEquals(source.getCaseRef(), destination.getCaseRef()),
        () -> assertEquals(source.isInvalid(), destination.isInvalid()),
        () -> assertEquals(source.getRefusalReceived(), destination.getRefusalReceived().name()),
        () -> assertEquals(source.getSample(), destination.getSample()),
        () -> assertEquals(toLocalDateTime(source.getCreatedAt()), destination.getCreatedAt()),
        () ->
            assertEquals(
                toLocalDateTime(source.getLastUpdatedAt()), destination.getLastUpdatedAt()),
        () -> assertEquals(source.getSampleSensitive(), destination.getSampleSensitive()));
  }

  @Test
  public void shouldMapAddressIndexAddressCompositeDTO_CollectionCaseNewAddress() {
    AddressIndexAddressCompositeDTO source =
        FixtureHelper.loadClassFixtures(AddressIndexAddressCompositeDTO[].class).get(0);
    CollectionCaseNewAddress destination = mapperFacade.map(source, CollectionCaseNewAddress.class);
    Address destAddr = destination.getAddress();
    assertAll(
        () -> assertEquals(source.getUprn(), destAddr.getUprn()),
        () -> assertEquals(source.getAddressLine1(), destAddr.getAddressLine1()),
        () -> assertEquals(source.getAddressLine2(), destAddr.getAddressLine2()),
        () -> assertEquals(source.getAddressLine3(), destAddr.getAddressLine3()),
        () -> assertEquals(source.getTownName(), destAddr.getTownName()),
        () -> assertEquals(source.getPostcode(), destAddr.getPostcode()),
        () -> assertEquals(source.getCensusAddressType(), destAddr.getAddressType()),
        () -> assertEquals(source.getCensusEstabType(), destAddr.getEstabType()),
        () -> assertEquals(source.getCountryCode(), destAddr.getRegion()),
        () -> assertEquals(source.getOrganisationName(), destination.getOrganisationName()));
  }

  @Test
  public void shouldMapSurveyUpdateToSurvey() {
    SurveyUpdate source = FixtureHelper.loadClassFixtures(SurveyUpdate[].class).get(0);
    Survey destination = mapperFacade.map(source, Survey.class);

    assertAll(
        () -> assertEquals(UUID.fromString(source.getSurveyId()), destination.getId()),
        () -> assertEquals(source.getName(), destination.getName()),
        () -> assertEquals(source.getSampleDefinitionUrl(), destination.getSampleDefinitionUrl()),
        () -> assertEquals(source.getSampleDefinition(), destination.getSampleDefinition()));
  }

  @Test
  public void shouldMapSurveyFulfilmentToProduct() {
    // This test will fail without the mappers ArrayListConverter
    SurveyFulfilment source = FixtureHelper.loadClassFixtures(SurveyFulfilment[].class).get(0);
    Product destination = mapperFacade.map(source, Product.class);

    assertAll(
        () -> assertEquals(source.getPackCode(), destination.getPackCode()),
        () -> assertEquals(source.getDescription(), destination.getDescription()),
        () -> assertEquals(source.getMetadata(), destination.getMetadata()));
  }

  @Test
  public void shouldMapArrayListOfStrings() {
    // This test will fail without the mappers ArrayListConverter
    List<Object> source = new ArrayList<>();
    source.add("A");
    source.add("B");

    Object destination = mapperFacade.map(source, Object.class);

    assertEquals(source, destination);
  }

  @Test
  public void shouldFailToMapArrayListOfNonStrings() {
    // This test will fail without the mappers ArrayListConverter
    List<Object> source = new ArrayList<>();
    source.add(Integer.valueOf("94"));

    try {
      mapperFacade.map(source, Object.class);
      fail("Conversion should have thrown exception");
    } catch (UnsupportedOperationException e) {
      assertTrue(e.getMessage().contains("Unsupported type found when mapping"), e.getMessage());
    }
  }

  @Test
  public void shouldMapCollectionExerciseUpdateToCollectionExercise() {
    CollectionExercise source = FixtureHelper.loadClassFixtures(CollectionExercise[].class).get(0);
    var meta = source.getMetadata();
    var destination =
        mapperFacade.map(
            source, uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise.class);

    assertAll(
        () -> assertEquals(UUID.fromString(source.getCollectionExerciseId()), destination.getId()),
        () -> assertEquals(UUID.fromString(source.getSurveyId()), destination.getSurvey().getId()),
        () -> assertEquals(source.getName(), destination.getName()),
        () -> assertEquals(source.getReference(), destination.getReference()),
        () ->
            assertEquals(
                source.getStartDate().toInstant(),
                destination.getStartDate().toInstant(ZoneOffset.UTC)),
        () ->
            assertEquals(
                source.getEndDate().toInstant(),
                destination.getEndDate().toInstant(ZoneOffset.UTC)),
        () -> assertEquals(meta.getNumberOfWaves(), destination.getNumberOfWaves()),
        () -> assertEquals(meta.getWaveLength(), destination.getWaveLength()),
        () -> assertEquals(meta.getCohorts(), destination.getCohorts()),
        () -> assertEquals(meta.getCohortSchedule(), destination.getCohortSchedule()));
  }

  @Test
  public void shouldMapUacUpdate_to_Uac() {
    UacUpdate source = FixtureHelper.loadClassFixtures(UacUpdate[].class).get(0);
    Uac destination = mapperFacade.map(source, Uac.class);
    assertAll(
        () -> assertEquals(source.getCaseId(), destination.getCaseId().toString()),
        () ->
            assertEquals(
                source.getCollectionExerciseId(), destination.getCollectionExerciseId().toString()),
        () -> assertEquals(source.getQid(), destination.getQuestionnaire()),
        () -> assertEquals(source.getMetadata().getWave(), destination.getWaveNum()),
        () -> assertEquals(source.getUacHash(), destination.getUacHash()),
        () ->
            assertEquals(
                source.getCollectionInstrumentUrl(), destination.getCollectionInstrumentUrl()),
        () -> assertEquals(source.getSurveyId(), destination.getSurveyId().toString()));
  }

  private LocalDateTime toLocalDateTime(Date date) {
    return date.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
  }
}
