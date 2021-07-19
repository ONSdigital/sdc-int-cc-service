package uk.gov.ons.ctp.integration.contactcentresvc;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.Test;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.EventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressCompositeDTO;
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
        () -> assertEquals(source.getAddressType(), destination.getAddressType()),
        () -> assertNull(destination.getEstabType()),
        () -> assertEquals(source.getEstabType(), destination.getEstabDescription()),
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
                source.getEstabUprn(), String.valueOf(destination.getEstabUprn().getValue())),
        () -> assertEquals(source.getCreatedDateTime(), destination.getCreatedDateTime()),
        () -> assertEquals(source.getLastUpdated(), destination.getLastUpdated()));

    verifyMapping(source.getCaseEvents(), destination.getCaseEvents());
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
