package uk.gov.ons.ctp.integration.contactcentresvc;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.impl.ConfigurableMapper;
import ma.glasnost.orika.metadata.Type;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.ons.ctp.common.domain.Region;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.common.event.model.CollectionExercise;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.util.StringToUPRNConverter;
import uk.gov.ons.ctp.common.util.StringToUUIDConverter;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.EventDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressCompositeDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseEventDTO;

/** The bean mapper that maps to/from DTOs and JPA entity types. */
@Component
public class CCSvcBeanMapper extends ConfigurableMapper {

  /**
   * Setup the mapper for all of our beans. Only fields having non identical names need mapping if
   * we also use byDefault() following.
   *
   * @param factory the factory to which we add our mappings
   */
  protected final void configure(final MapperFactory factory) {
    ConverterFactory converterFactory = factory.getConverterFactory();
    converterFactory.registerConverter("regionConverter", new RegionConverter());
    converterFactory.registerConverter(new StringToUUIDConverter());
    converterFactory.registerConverter(new StringToUPRNConverter());
    converterFactory.registerConverter(new UtcOffsetDateTimeConverter());
    converterFactory.registerConverter(new UtcLocalDateTimeConverter());

    factory
        .classMap(CaseContainerDTO.class, CaseDTO.class)
        .field("uprn", "address.uprn")
        .field("addressLine1", "address.addressLine1")
        .field("addressLine2", "address.addressLine2")
        .field("addressLine3", "address.addressLine3")
        .field("townName", "address.townName")
        .field("postcode", "address.postcode")
        .byDefault()
        .fieldMap("region", "address.region")
        .converter("regionConverter")
        .add()
        .register();

    factory
        .classMap(CaseContainerDTO.class, Case.class)
        .field("collectionExerciseId", "collectionExercise.id")
        .field("uprn", "address.uprn")
        .field("addressLine1", "address.addressLine1")
        .field("addressLine2", "address.addressLine2")
        .field("addressLine3", "address.addressLine3")
        .field("townName", "address.townName")
        .field("postcode", "address.postcode")
        .field("region", "address.region")
        .byDefault()
        .register();

    factory
        .classMap(CaseUpdate.class, Case.class)
        .field("collectionExerciseId", "collectionExercise.id")
        .field("caseId", "id")
        .field("sample.addressLine1", "address.addressLine1")
        .field("sample.addressLine2", "address.addressLine2")
        .field("sample.addressLine3", "address.addressLine3")
        .field("sample.townName", "address.townName")
        .field("sample.postcode", "address.postcode")
        .field("sample.region", "address.region")
        .field("sample.uprn", "address.uprn")
        .field("sampleSensitive.phoneNumber", "contact.phoneNumber")
        .byDefault()
        .register();

    factory
        .classMap(SurveyUpdate.class, Survey.class)
        .field("surveyId", "id")
        .byDefault()
        .register();

    factory
        .classMap(
            CollectionExercise.class,
            uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise.class)
        .field("collectionExerciseId", "id")
        .field("surveyId", "survey.id")
        .field("metadata.numberOfWaves", "numberOfWaves")
        .field("metadata.waveLength", "waveLength")
        .field("metadata.cohorts", "cohorts")
        .field("metadata.cohortSchedule", "cohortSchedule")
        .byDefault()
        .register();

    factory
        .classMap(EventDTO.class, CaseEventDTO.class)
        .field("eventType", "category")
        .byDefault()
        .register();

    factory
        .classMap(AddressIndexAddressCompositeDTO.class, CollectionCaseNewAddress.class)
        .field("uprn", "address.uprn")
        .field("addressLine1", "address.addressLine1")
        .field("addressLine2", "address.addressLine2")
        .field("addressLine3", "address.addressLine3")
        .field("townName", "address.townName")
        .field("postcode", "address.postcode")
        .field("censusAddressType", "address.addressType")
        .field("censusEstabType", "address.estabType")
        .field("countryCode", "address.region")
        .field("organisationName", "organisationName")
        .register();

    factory.classMap(CaseContainerDTO.class, Address.class).byDefault().register();
    factory.classMap(CaseContainerDTO.class, AddressCompact.class).byDefault().register();
    factory.classMap(CaseDTO.class, Case.class).byDefault().register();
  }

  static class RegionConverter extends BidirectionalConverter<String, Region> {
    public Region convertTo(String src, Type<Region> dstType, MappingContext context) {
      return Region.valueOf(convert(src));
    }

    public String convertFrom(Region src, Type<String> dstType, MappingContext context) {
      return src.name();
    }

    private String convert(String src) {
      return StringUtils.isEmpty(src) ? src : src.substring(0, 1).toUpperCase();
    }
  }

  static class UtcOffsetDateTimeConverter extends BidirectionalConverter<Date, OffsetDateTime> {
    @Override
    public OffsetDateTime convertTo(
        Date source, Type<OffsetDateTime> destinationType, MappingContext mappingContext) {
      return source.toInstant().atOffset(ZoneOffset.UTC);
    }

    @Override
    public Date convertFrom(
        OffsetDateTime source, Type<Date> destinationType, MappingContext mappingContext) {
      return new Date(source.toInstant().toEpochMilli());
    }
  }

  static class UtcLocalDateTimeConverter extends BidirectionalConverter<Date, LocalDateTime> {
    @Override
    public LocalDateTime convertTo(
        Date source, Type<LocalDateTime> destinationType, MappingContext mappingContext) {
      return source.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
    }

    @Override
    public Date convertFrom(
        LocalDateTime source, Type<Date> destinationType, MappingContext mappingContext) {
      return new Date(source.toInstant(ZoneOffset.UTC).toEpochMilli());
    }
  }
}
