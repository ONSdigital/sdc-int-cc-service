package uk.gov.ons.ctp.integration.contactcentresvc;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.Test;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CaseUpdateSample;
import uk.gov.ons.ctp.common.event.model.CaseUpdateSampleSensitive;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.EventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressCompositeDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseAddress;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseAddressDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;

public class CCSvcBeanMapperTest {

  private MapperFacade mapperFacade = new CCSvcBeanMapper();

  private void verifyMapping(List<EventDTO> sourceList, List<CaseEventDTO> destinationList) {
    for (int i = 0; i < sourceList.size(); i++) {
      EventDTO sourceEvent = sourceList.get(i);
      CaseEventDTO destinationEvent = destinationList.get(i);
      assertAll(
          () -> assertEquals(sourceEvent.getDescription(), destinationEvent.getDescription()),
          () -> assertEquals(sourceEvent.getEventType(), destinationEvent.getCategory()),
          () ->
              assertEquals(
                  sourceEvent.getCreatedDateTime(), destinationEvent.getCreatedDateTime()));
    }
  }

  @Test
  public void shouldMapCaseContainerDTO_CaseDTO() {
    CaseContainerDTO source = FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    CaseDTO destination = mapperFacade.map(source, CaseDTO.class);
    CaseAddressDTO addr = destination.getAddress();
    assertAll(
        () -> assertEquals(source.getId(), destination.getId()),
        () -> assertEquals(source.getCaseRef(), destination.getCaseRef()),
        () -> assertEquals(source.getAddressLine1(), addr.getAddressLine1()),
        () -> assertEquals(source.getAddressLine2(), addr.getAddressLine2()),
        () -> assertEquals(source.getAddressLine3(), addr.getAddressLine3()),
        () -> assertEquals(source.getTownName(), addr.getTownName()),
        () -> assertEquals(source.getPostcode(), addr.getPostcode()),
        () -> assertEquals(source.getRegion().substring(0, 1), addr.getRegion().name()),
        () -> assertEquals(source.getUprn(), String.valueOf(addr.getUprn().getValue())));

    verifyMapping(source.getCaseEvents(), destination.getCaseEvents());
  }

  @Test
  public void shouldMapCase_to_CaseContainerDTO() {
    Case source = FixtureHelper.loadClassFixtures(Case[].class).get(0);
    CaseAddress addr = source.getAddress();
    CaseContainerDTO destination = mapperFacade.map(source, CaseContainerDTO.class);
    assertAll(
        () -> assertEquals(source.getId(), destination.getId()),
        () -> assertEquals(source.getCaseRef(), destination.getCaseRef()),
        () ->
            assertEquals(
                source.getCollectionExercise().getId(), destination.getCollectionExerciseId()),
        () -> assertEquals(addr.getUprn(), destination.getUprn()),
        () -> assertEquals(addr.getAddressLine1(), destination.getAddressLine1()),
        () -> assertEquals(addr.getAddressLine2(), destination.getAddressLine2()),
        () -> assertEquals(addr.getAddressLine3(), destination.getAddressLine3()),
        () -> assertEquals(addr.getTownName(), destination.getTownName()),
        () -> assertEquals(addr.getPostcode(), destination.getPostcode()),
        () -> assertEquals(addr.getRegion().name(), destination.getRegion()));
  }

  @Test
  public void shouldMapCase_to_CaseDTO() {
    Case source = FixtureHelper.loadClassFixtures(Case[].class).get(0);
    CaseAddress addr = source.getAddress();
    CaseDTO destination = mapperFacade.map(source, CaseDTO.class);
    CaseAddressDTO addrDto = destination.getAddress();
    assertAll(
        () -> assertEquals(source.getId(), destination.getId()),
        () -> assertEquals(source.getCaseRef(), destination.getCaseRef()),
        () -> assertEquals(Long.valueOf(addr.getUprn()), addrDto.getUprn().getValue()),
        () -> assertEquals(addr.getAddressLine1(), addrDto.getAddressLine1()),
        () -> assertEquals(addr.getAddressLine2(), addrDto.getAddressLine2()),
        () -> assertEquals(addr.getAddressLine3(), addrDto.getAddressLine3()),
        () -> assertEquals(addr.getTownName(), addrDto.getTownName()),
        () -> assertEquals(addr.getPostcode(), addrDto.getPostcode()),
        () -> assertEquals(addr.getRegion(), addrDto.getRegion()));
  }

  @Test
  public void shouldMapCaseUpdate_to_Case() {
    CaseUpdate source = FixtureHelper.loadClassFixtures(CaseUpdate[].class).get(0);
    Case destination = mapperFacade.map(source, Case.class);
    CaseUpdateSample sample = source.getSample();
    CaseUpdateSampleSensitive sensitive = source.getSampleSensitive();
    CaseAddress addr = destination.getAddress();
    assertAll(
        () -> assertEquals(source.getCaseId(), destination.getId().toString()),
        () ->
            assertEquals(
                source.getCollectionExerciseId(),
                destination.getCollectionExercise().getId().toString()),
        () -> assertEquals(source.getCaseRef(), destination.getCaseRef()),
        () -> assertEquals(source.isInvalid(), destination.isInvalid()),
        () -> assertEquals(source.getRefusalReceived(), destination.getRefusalReceived().name()),
        () -> assertEquals(sample.getAddressLine1(), addr.getAddressLine1()),
        () -> assertEquals(sample.getAddressLine2(), addr.getAddressLine2()),
        () -> assertEquals(sample.getAddressLine3(), addr.getAddressLine3()),
        () -> assertEquals(sample.getTownName(), addr.getTownName()),
        () -> assertEquals(sample.getPostcode(), addr.getPostcode()),
        () -> assertEquals(sample.getRegion(), addr.getRegion().name()),
        () -> assertEquals(sample.getUprn(), addr.getUprn()),
        () -> assertEquals(toLocalDateTime(source.getCreatedAt()), destination.getCreatedAt()),
        () ->
            assertEquals(
                toLocalDateTime(source.getLastUpdatedAt()), destination.getLastUpdatedAt()),
        () -> assertEquals(sensitive.getPhoneNumber(), destination.getContact().getPhoneNumber()));
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

  private void verifyMapping(AddressCompact destination, CaseContainerDTO source) {
    assertAll(
        () -> assertEquals(source.getAddressLine1(), destination.getAddressLine1()),
        () -> assertEquals(source.getAddressLine2(), destination.getAddressLine2()),
        () -> assertEquals(source.getAddressLine3(), destination.getAddressLine3()),
        () -> assertEquals(source.getTownName(), destination.getTownName()),
        () -> assertEquals(source.getPostcode(), destination.getPostcode()),
        () -> assertEquals(source.getRegion(), destination.getRegion()),
        () -> assertEquals(source.getUprn(), destination.getUprn()),
        () -> assertEquals(source.getEstabType(), destination.getEstabType()),
        () -> assertEquals(source.getOrganisationName(), destination.getOrganisationName()));
  }

  @Test
  public void shouldMapCaseContainerDTO_Address() {
    CaseContainerDTO source = FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    Address destination = mapperFacade.map(source, Address.class);
    verifyMapping(destination, source);

    assertAll(
        () -> assertEquals(source.getLatitude(), destination.getLatitude()),
        () -> assertEquals(source.getLongitude(), destination.getLongitude()),
        () -> assertEquals(source.getEstabUprn(), destination.getEstabUprn()),
        () -> assertEquals(source.getAddressType(), destination.getAddressType()),
        () -> assertEquals(source.getAddressLevel(), destination.getAddressLevel()));
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
  public void shouldMapCaseContainerDTO_AddressCompact() {
    CaseContainerDTO source = FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    AddressCompact destination = mapperFacade.map(source, AddressCompact.class);
    verifyMapping(destination, source);
  }

  private LocalDateTime toLocalDateTime(Date date) {
    return date.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
  }
}
