package uk.gov.ons.ctp.integration.contactcentresvc;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.ObjectFactory;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.impl.ConfigurableMapper;
import ma.glasnost.orika.metadata.Type;
import ma.glasnost.orika.metadata.TypeBuilder;
import org.eclipse.jdt.internal.compiler.SourceElementNotifier;
import org.springframework.stereotype.Component;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdate;
import uk.gov.ons.ctp.common.event.model.NewCasePayloadContent;
import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.event.model.UacUpdate;
import uk.gov.ons.ctp.common.util.StringToUPRNConverter;
import uk.gov.ons.ctp.common.util.StringToUUIDConverter;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.RmCaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.client.addressindex.model.AddressIndexAddressCompositeDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseInteraction;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Permission;
import uk.gov.ons.ctp.integration.contactcentresvc.model.PermissionType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Uac;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.CaseInteractionRequestDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.SurveyDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UserDTO;
import uk.gov.ons.ctp.integration.contactcentresvc.representation.UsersCaseInteractionDTO;

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
    converterFactory.registerConverter(new StringToUUIDConverter());
    converterFactory.registerConverter(new StringToUPRNConverter());
    converterFactory.registerConverter(new UtcOffsetDateTimeConverter());
    converterFactory.registerConverter(new UtcLocalDateTimeConverter());
    converterFactory.registerConverter(new ArrayListConverter());
    factory.registerObjectFactory(
        new PermissionTypeFactory(), new TypeBuilder<PermissionType>() {}.build());

    factory
        .classMap(CaseUpdate.class, Case.class)
        .field("collectionExerciseId", "collectionExercise.id")
        .field("caseId", "id")
        .byDefault()
        .register();

    factory
        .classMap(SurveyUpdate.class, Survey.class)
        .field("surveyId", "id")
        .byDefault()
        .register();

    factory
        .classMap(CollectionExerciseUpdate.class, CollectionExercise.class)
        .field("collectionExerciseId", "id")
        .field("surveyId", "survey.id")
        .field("metadata.numberOfWaves", "numberOfWaves")
        .field("metadata.waveLength", "waveLength")
        .field("metadata.cohorts", "cohorts")
        .field("metadata.cohortSchedule", "cohortSchedule")
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

    factory
        .classMap(UacUpdate.class, Uac.class)
        .field("metadata.wave", "waveNum")
        .field("qid", "questionnaire")
        .byDefault()
        .register();

    factory.classMap(CaseInteractionRequestDTO.class, CaseInteraction.class).byDefault().register();
    factory.classMap(RmCaseDTO.class, Case.class).byDefault().register();

    factory
        .classMap(CaseDTO.class, Case.class)
        .field("collectionExerciseId", "collectionExercise.id")
        .field("surveyId", "collectionExercise.survey.id")
        .customize(
            new CustomMapper<CaseDTO, Case>() {
              @Override
              public void mapBtoA(Case a, CaseDTO b, MappingContext mappingContext) {
                // Work out survey type
                Survey survey = a.getCollectionExercise().getSurvey();
                if (survey != null) {
                  String sampleDefinitionUrl = survey.getSampleDefinitionUrl();
                  b.setSurveyType(SurveyType.fromSampleDefinitionUrl(sampleDefinitionUrl));
                }
              }
            })
        .byDefault()
        .register();

    factory
        .classMap(UserDTO.class, User.class)
        .customize(
            new CustomMapper<UserDTO, User>() {
              @Override
              public void mapBtoA(User a, UserDTO b, MappingContext mappingContext) {
                b.setUserRoles(
                    a.getUserRoles() == null
                        ? null
                        : a.getUserRoles().stream()
                            .map(r -> r.getName())
                            .collect(Collectors.toList()));
                b.setAdminRoles(
                    a.getAdminRoles() == null
                        ? null
                        : a.getAdminRoles().stream()
                            .map(r -> r.getName())
                            .collect(Collectors.toList()));
              }
            })
        .byDefault()
        .register();

    factory
        .classMap(NewCasePayloadContent.class, CaseDTO.class)
        .field("caseId", "id")
        .byDefault()
        .register();

    factory
        .classMap(Survey.class, SurveyDTO.class)
        .field("name", "surveyName")
        .customize(
            new CustomMapper<Survey, SurveyDTO>() {
              @Override
              public void mapAtoB(
                  Survey survey, SurveyDTO surveyDTO, MappingContext mappingContext) {
                String sampleDefinitionUrl = survey.getSampleDefinitionUrl();
                surveyDTO.setSurveyType(SurveyType.fromSampleDefinitionUrl(sampleDefinitionUrl));
              }
            })
        .byDefault()
        .register();

    factory
        .classMap(CaseInteraction.class, UsersCaseInteractionDTO.class)
        .field("caze.id", "caseId")
        .field("caze.caseRef", "caseRef")
        .field("type", "interaction")
        .field("subtype", "subInteraction")
        .customize(
            new CustomMapper<CaseInteraction, UsersCaseInteractionDTO>() {
              @Override
              public void mapAtoB(
                  CaseInteraction interaction,
                  UsersCaseInteractionDTO interactionDTO,
                  MappingContext mappingContext) {
                interactionDTO.setSurveyType(
                    interaction.getCaze().getCollectionExercise().getSurvey().surveyType());
              }
            })
        .byDefault()
        .register();
  }

  static class PermissionTypeFactory implements ObjectFactory<PermissionType> {
    @Override
    public PermissionType create(Object source, MappingContext mappingContext) {
      return ((Permission) source).getPermissionType();
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

  // This converter was added to support the conversion of the metadata map used
  // by
  // the Product class
  static class ArrayListConverter extends BidirectionalConverter<ArrayList<Object>, Object> {
    @Override
    public Object convertTo(
        ArrayList<Object> source, Type<Object> destinationType, MappingContext mappingContext) {

      List<String> destination = new ArrayList<>();
      for (Object sourceElement : source) {
        if (sourceElement instanceof String sourceElementStr) {
          destination.add(sourceElementStr);
        } else {
          throw new UnsupportedOperationException(
              "Unsupported type found when mapping an an ArrayList: "
                  + SourceElementNotifier.class.getCanonicalName());
        }
      }

      return destination;
    }

    @Override
    public ArrayList<Object> convertFrom(
        Object source, Type<ArrayList<Object>> destinationType, MappingContext mappingContext) {
      throw new UnsupportedOperationException("Conversion not implemented");
    }
  }
}
