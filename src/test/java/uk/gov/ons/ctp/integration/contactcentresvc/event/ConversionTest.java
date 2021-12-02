package uk.gov.ons.ctp.integration.contactcentresvc.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import ma.glasnost.orika.MappingException;
import ma.glasnost.orika.metadata.Property;
import ma.glasnost.orika.property.IntrospectorPropertyResolver;
import org.junit.jupiter.api.Test;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Product;
import uk.gov.ons.ctp.integration.contactcentresvc.model.ProductType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Survey;

// PMB delete

public class ConversionTest {

  @Test
  public void test2() throws IOException {
    //    ObjectMapper mapper = createMapper();
    //
    //    String surveyStr =
    //        "{\n"
    //            + "      \"surveyId\": \"3883af91-0052-4497-9805-3238544fcf8a\",\n"
    //            + "      \"name\": \"LMS-PMB3\",\n"
    //            + "      \"sampleDefinitionUrl\":
    // \"https://raw.githubusercontent.com/ONSdigital/ssdc-shared-events/main/sample/social/0.1.0-DRAFT/social.json\",\n"
    //            + "      \"sampleDefinition\": [\n"
    //            + "        {\n"
    //            + "          \"columnName\": \"addressLine1\",\n"
    //            + "          \"rules\": [\n"
    //            + "            {\n"
    //            + "              \"className\":
    // \"uk.gov.ons.ssdc.common.validation.MandatoryRule\"\n"
    //            + "            },\n"
    //            + "            {\n"
    //            + "              \"className\":
    // \"uk.gov.ons.ssdc.common.validation.LengthRule\",\n"
    //            + "              \"maxLength\": 60\n"
    //            + "            }\n"
    //            + "          ]\n"
    //            + "        },\n"
    //            + "        {\n"
    //            + "          \"columnName\": \"addressLine2\",\n"
    //            + "          \"rules\": [\n"
    //            + "            {\n"
    //            + "              \"className\":
    // \"uk.gov.ons.ssdc.common.validation.LengthRule\",\n"
    //            + "              \"maxLength\": 60\n"
    //            + "            }\n"
    //            + "          ]\n"
    //            + "        },\n"
    //            + "        {\n"
    //            + "          \"columnName\": \"addressLine3\",\n"
    //            + "          \"rules\": [\n"
    //            + "            {\n"
    //            + "              \"className\":
    // \"uk.gov.ons.ssdc.common.validation.LengthRule\",\n"
    //            + "              \"maxLength\": 60\n"
    //            + "            }\n"
    //            + "          ]\n"
    //            + "        },\n"
    //            + "        {\n"
    //            + "          \"columnName\": \"townName\",\n"
    //            + "          \"rules\": [\n"
    //            + "            {\n"
    //            + "              \"className\":
    // \"uk.gov.ons.ssdc.common.validation.MandatoryRule\"\n"
    //            + "            }\n"
    //            + "          ]\n"
    //            + "        },\n"
    //            + "        {\n"
    //            + "          \"columnName\": \"postcode\",\n"
    //            + "          \"rules\": [\n"
    //            + "            {\n"
    //            + "              \"className\":
    // \"uk.gov.ons.ssdc.common.validation.MandatoryRule\"\n"
    //            + "            }\n"
    //            + "          ]\n"
    //            + "        },\n"
    //            + "        {\n"
    //            + "          \"columnName\": \"region\",\n"
    //            + "          \"rules\": [\n"
    //            + "            {\n"
    //            + "              \"className\":
    // \"uk.gov.ons.ssdc.common.validation.InSetRule\",\n"
    //            + "              \"set\": [\n"
    //            + "                \"E\",\n"
    //            + "                \"W\",\n"
    //            + "                \"N\"\n"
    //            + "              ]\n"
    //            + "            }\n"
    //            + "          ]\n"
    //            + "        },\n"
    //            + "        {\n"
    //            + "          \"columnName\": \"uprn\",\n"
    //            + "          \"rules\": [\n"
    //            + "            {\n"
    //            + "              \"className\":
    // \"uk.gov.ons.ssdc.common.validation.MandatoryRule\"\n"
    //            + "            }\n"
    //            + "          ]\n"
    //            + "        },\n"
    //            + "        {\n"
    //            + "          \"columnName\": \"phoneNumber\",\n"
    //            + "          \"sensitive\": true,\n"
    //            + "          \"rules\": [\n"
    //            + "            {\n"
    //            + "              \"className\":
    // \"uk.gov.ons.ssdc.common.validation.RegexRule\",\n"
    //            + "              \"expression\": \"^07[0-9]{9}$\"\n"
    //            + "            }\n"
    //            + "          ]\n"
    //            + "        }\n"
    //            + "      ],\n"
    //            + "      \"allowedPrintFulfilments\": [\n"
    //            + "        {\n"
    //            + "          \"packCode\": \"replace-uac-en\",\n"
    //            + "          \"description\": \"Replacement UAC - English\",\n"
    //            + "          \"metadata\": {\n"
    //            + "            \"suitableRegions\": [\n"
    //            + "              \"E\",\n"
    //            + "              \"N\"\n"
    //            + "            ]\n"
    //            + "          }\n"
    //            + "        },\n"
    //            + "        {\n"
    //            + "          \"packCode\": \"replace-uac-cy\",\n"
    //            + "          \"description\": \"Replacement UAC - English & Welsh\",\n"
    //            + "          \"metadata\": {\n"
    //            + "            \"suitableRegions\": [\n"
    //            + "              \"W\"\n"
    //            + "            ]\n"
    //            + "          }\n"
    //            + "        }\n"
    //            + "      ],\n"
    //            + "      \"allowedSmsFulfilments\": [\n"
    //            + "        {\n"
    //            + "          \"packCode\": \"replace-uac-en\",\n"
    //            + "          \"description\": \"Replacement UAC - English3\",\n"
    //            + "          \"metadata\": {\n"
    //            + "            \"suitableRegions\": [\n"
    //            + "              \"E\",\n"
    //            + "              \"N\"\n"
    //            + "            ]\n"
    //            + "          }\n"
    //            + "        },\n"
    //            + "        {\n"
    //            + "          \"packCode\": \"replace-uac-cy\",\n"
    //            + "          \"description\": \"Replacement UAC - English & Welsh\",\n"
    //            + "          \"metadata\": {\n"
    //            + "            \"suitableRegions\": [\n"
    //            + "              \"W\"\n"
    //            + "            ],\n"
    //            + "            \"Another\": \"PMB\"\n"
    //            + "          }\n"
    //            + "        }\n"
    //            + "      ],\n"
    //            + "      \"allowedEmailFulfilments\": [],\n"
    //            + "      \"metadata\": {\n"
    //            + "        \"ex_e4\": true\n"
    //            + "      }\n"
    //            + "    }\n"
    //            + "";
    //
    //    SurveyUpdate p2 = mapper.readValue(surveyStr.getBytes(), SurveyUpdate.class);
    //
    //    MapperFactory factory =
    //        new DefaultMapperFactory.Builder()
    //            .propertyResolverStrategy(new ElementPropertyResolver())
    //            .build();
    //
    //    CCSvcBeanMapper ccMapper = new CCSvcBeanMapper();
    //
    //    Map m1 = (Map) p2.getAllowedPrintFulfilments().get(0).getMetadata();
    //    Map m2 = ccMapper.map(m1, Map.class);
    //
    //    ArrayList al =
    //        (ArrayList)
    // p2.getAllowedPrintFulfilments().get(0).getMetadata().get("suitableRegions");
    //    ArrayList al2 = ccMapper.map(al, ArrayList.class);
    //
    //    Survey survey = ccMapper.map(p2, Survey.class);
    //    Map<String, ?> m = filterProducts(survey, ProductType.POSTAL).get(0).getMetadata();
    //    ArrayList<String> foo = (ArrayList<String>) m.get("suitableRegions");
    //    System.out.println(foo);
  }

  private static ObjectMapper createMapper() {
    ObjectMapper mapper = new CustomObjectMapper();
    //    mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

    //    // make strict
    //    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    //    mapper.configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, false);
    //    mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
    //    mapper.configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true);
    return mapper;
  }

  public static class ElementPropertyResolver extends IntrospectorPropertyResolver {
    protected Property getProperty(
        java.lang.reflect.Type type, String expr, boolean isNestedLookup, Property owner)
        throws MappingException {
      Property property = null;
      try {
        property = super.getProperty(type, expr, isNestedLookup, null);
      } catch (MappingException e) {
        //        try {
        //          property = super.resolveInlineProperty(type, expr +
        //              ":{getAttribute(\""+ expr+"\")|setAttribute(\""+ expr+"\",%s)|type=" +
        //              (isNestedLookup ? Element.class.getName() : "Object") + "}");
        //        } catch (MappingException e2) {
        throw e; // throw the original exception
        //        }
      }
      return property;
    }
  }

  private List<Product> filterProducts(Survey survey, ProductType targetProductType) {
    List<Product> allowedFulfilments = survey.getAllowedFulfilments();
    if (allowedFulfilments == null) {
      return null;
    }
    return allowedFulfilments.stream()
        .filter(f -> f.getType() == targetProductType)
        .collect(Collectors.toList());
  }
}
