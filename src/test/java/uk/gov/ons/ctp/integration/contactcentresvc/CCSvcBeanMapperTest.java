package uk.gov.ons.ctp.integration.contactcentresvc;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.Test;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CaseUpdateSample;
import uk.gov.ons.ctp.common.event.model.CaseUpdateSampleSensitive;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.EventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressCompositeDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseAddress;
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
    assertAll(
        () -> assertEquals(source.getId(), destination.getId()),
        () -> assertEquals(source.getCaseRef(), destination.getCaseRef()),
        () -> assertEquals(source.getAddressLine1(), destination.getAddressLine1()),
        () -> assertEquals(source.getAddressLine2(), destination.getAddressLine2()),
        () -> assertEquals(source.getAddressLine3(), destination.getAddressLine3()),
        () -> assertEquals(source.getTownName(), destination.getTownName()),
        () -> assertEquals(source.getPostcode(), destination.getPostcode()),
        () -> assertEquals(source.getOrganisationName(), destination.getCeOrgName()),
        () -> assertEquals(source.getRegion().substring(0, 1), destination.getRegion()),
        () -> assertEquals(source.getUprn(), String.valueOf(destination.getUprn().getValue())),
        () ->
            assertEquals(
                source.getEstabUprn(), String.valueOf(destination.getEstabUprn().getValue())));

    verifyMapping(source.getCaseEvents(), destination.getCaseEvents());
  }

  /*
   * FIXME remove when sure this has no value after refactor.
   *

  @Test
  public void shouldMapCase_to_CaseContainerDTO() {
    Case source = FixtureHelper.loadClassFixtures(Case[].class).get(0);
    CaseAddress addr = source.getAddress();
    CaseContainerDTO destination = mapperFacade.map(source, CaseContainerDTO.class);
    Date expectedCreation = new Date(source.getCreatedDateTime().toInstant().toEpochMilli());
    assertAll(
        () -> assertEquals(source.getId(), destination.getId()),
        () -> assertEquals(source.getCaseRef().toString(), destination.getCaseRef()),
        () -> assertEquals(source.getCaseType().name(), destination.getCaseType()),
        () -> assertEquals(source.getSurvey(), destination.getSurveyType()),
        () -> assertEquals(source.getCollectionExerciseId(), destination.getCollectionExerciseId()),
        () -> assertEquals(source.isHandDelivery(), destination.isHandDelivery()),
        () -> assertEquals(addr.getUprn(), destination.getUprn()),
        () -> assertEquals(addr.getAddressLine1(), destination.getAddressLine1()),
        () -> assertEquals(addr.getAddressLine2(), destination.getAddressLine2()),
        () -> assertEquals(addr.getAddressLine3(), destination.getAddressLine3()),
        () -> assertEquals(addr.getTownName(), destination.getTownName()),
        () -> assertEquals(addr.getPostcode(), destination.getPostcode()),
        () -> assertEquals(addr.getRegion(), destination.getRegion()),
        () -> assertEquals(addr.getEstabType(), destination.getEstabType()),
        () -> assertEquals(addr.getOrganisationName(), destination.getOrganisationName()),
        () -> assertEquals(addr.getLatitude(), destination.getLatitude()),
        () -> assertEquals(addr.getLongitude(), destination.getLongitude()),
        () -> assertEquals(addr.getEstabUprn(), destination.getEstabUprn()),
        () -> assertEquals(addr.getAddressType(), destination.getAddressType()),
        () -> assertEquals(addr.getAddressLevel(), destination.getAddressLevel()),
        () -> assertEquals(expectedCreation, destination.getCreatedDateTime()));
  }

  */

  @Test
  public void shouldMapCaseUpdate_to_Case() {
    CaseUpdate source = FixtureHelper.loadClassFixtures(CaseUpdate[].class).get(0);
    Case destination = mapperFacade.map(source, Case.class);
    CaseUpdateSample sample = source.getSample();
    CaseUpdateSampleSensitive sensitive = source.getSampleSensitive();
    CaseAddress addr = destination.getAddress();
    assertAll(
        () -> assertEquals(source.getCaseId(), destination.getId().toString()),
        () -> assertEquals(source.getSurveyId(), destination.getSurveyId().toString()),
        () ->
            assertEquals(
                source.getCollectionExerciseId(), destination.getCollectionExerciseId().toString()),
        () -> assertEquals(source.isInvalid(), destination.isInvalid()),
        () -> assertEquals(source.getRefusalReceived(), destination.getRefusalReceived().name()),
        () -> assertEquals(sample.getAddressLine1(), addr.getAddressLine1()),
        () -> assertEquals(sample.getAddressLine2(), addr.getAddressLine2()),
        () -> assertEquals(sample.getAddressLine3(), addr.getAddressLine3()),
        () -> assertEquals(sample.getTownName(), addr.getTownName()),
        () -> assertEquals(sample.getPostcode(), addr.getPostcode()),
        () -> assertEquals(sample.getRegion(), addr.getRegion().name()),
        () -> assertEquals(sample.getUprn(), addr.getUprn()),
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
  public void shouldMapCaseContainerDTO_AddressCompact() {
    CaseContainerDTO source = FixtureHelper.loadClassFixtures(CaseContainerDTO[].class).get(0);
    AddressCompact destination = mapperFacade.map(source, AddressCompact.class);
    verifyMapping(destination, source);
  }
}
