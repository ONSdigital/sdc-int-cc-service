package uk.gov.ons.ctp.integration.contactcentresvc.event;

import java.io.IOException;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.ons.ctp.common.event.model.SurveyUpdate;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Product;

// PMB delete


public class MapperTest {

  @Test
  public void test1() throws IOException {
    ObjectMapper mapper = createMapper();
    
    String productStr = "{\n"
        + "          \"packCode\": \"replace-uac-en\",\n"
        + "          \"description\": \"Replacement UAC - English\",\n"
        + "          \"metadata\": {\n"
        + "            \"suitableRegions\": [\n"
        + "              \"E\",\n"
        + "              \"N\"\n"
        + "            ]\n"
        + "          }\n"
        + "        }";
    
    JsonNode p = mapper.readTree(productStr);
    
    Product p2 = mapper.readValue(productStr.getBytes(), Product.class);
    
    System.out.println("p:"  + p);
    System.out.println("p2: " + p2);
//    System.out.println(p.getPackCode());
    
    String p2Str = mapper.writeValueAsString(p2);
    System.out.println("After: " + p2Str);
  }
  
  @Test
  public void test1b() throws IOException {
    ObjectMapper mapper = createMapper();
    
    String productStr = "{\n"
        + "            \"suitableRegions\": [\n"
        + "              \"E\",\n"
        + "              \"N\"\n"
        + "            ],"
        + "            \"name\": \"value\""
        + "}\n";
    
    JsonNode p = mapper.readTree(productStr);
    
    HashMap<String, Object> p2 = mapper.readValue(productStr.getBytes(), HashMap.class);
    
    System.out.println("p:"  + p);
    System.out.println("p2: " + p2);
//    System.out.println(p.getPackCode());
    
    String p2Str = mapper.writeValueAsString(p2);
    System.out.println("After: " + p2Str);
    System.out.println("Map content for 'name': " + p2.get("name"));
  }
  
  @Test
  public void test2() throws IOException {
    ObjectMapper mapper = createMapper();
    
    String surveyStr = "{\n"
        + "      \"surveyId\": \"3883af91-0052-4497-9805-3238544fcf8a\",\n"
        + "      \"name\": \"LMS-PMB3\",\n"
        + "      \"sampleDefinitionUrl\": \"https://raw.githubusercontent.com/ONSdigital/ssdc-shared-events/main/sample/social/0.1.0-DRAFT/social.json\",\n"
        + "      \"sampleDefinition\": [\n"
        + "        {\n"
        + "          \"columnName\": \"addressLine1\",\n"
        + "          \"rules\": [\n"
        + "            {\n"
        + "              \"className\": \"uk.gov.ons.ssdc.common.validation.MandatoryRule\"\n"
        + "            },\n"
        + "            {\n"
        + "              \"className\": \"uk.gov.ons.ssdc.common.validation.LengthRule\",\n"
        + "              \"maxLength\": 60\n"
        + "            }\n"
        + "          ]\n"
        + "        },\n"
        + "        {\n"
        + "          \"columnName\": \"addressLine2\",\n"
        + "          \"rules\": [\n"
        + "            {\n"
        + "              \"className\": \"uk.gov.ons.ssdc.common.validation.LengthRule\",\n"
        + "              \"maxLength\": 60\n"
        + "            }\n"
        + "          ]\n"
        + "        },\n"
        + "        {\n"
        + "          \"columnName\": \"addressLine3\",\n"
        + "          \"rules\": [\n"
        + "            {\n"
        + "              \"className\": \"uk.gov.ons.ssdc.common.validation.LengthRule\",\n"
        + "              \"maxLength\": 60\n"
        + "            }\n"
        + "          ]\n"
        + "        },\n"
        + "        {\n"
        + "          \"columnName\": \"townName\",\n"
        + "          \"rules\": [\n"
        + "            {\n"
        + "              \"className\": \"uk.gov.ons.ssdc.common.validation.MandatoryRule\"\n"
        + "            }\n"
        + "          ]\n"
        + "        },\n"
        + "        {\n"
        + "          \"columnName\": \"postcode\",\n"
        + "          \"rules\": [\n"
        + "            {\n"
        + "              \"className\": \"uk.gov.ons.ssdc.common.validation.MandatoryRule\"\n"
        + "            }\n"
        + "          ]\n"
        + "        },\n"
        + "        {\n"
        + "          \"columnName\": \"region\",\n"
        + "          \"rules\": [\n"
        + "            {\n"
        + "              \"className\": \"uk.gov.ons.ssdc.common.validation.InSetRule\",\n"
        + "              \"set\": [\n"
        + "                \"E\",\n"
        + "                \"W\",\n"
        + "                \"N\"\n"
        + "              ]\n"
        + "            }\n"
        + "          ]\n"
        + "        },\n"
        + "        {\n"
        + "          \"columnName\": \"uprn\",\n"
        + "          \"rules\": [\n"
        + "            {\n"
        + "              \"className\": \"uk.gov.ons.ssdc.common.validation.MandatoryRule\"\n"
        + "            }\n"
        + "          ]\n"
        + "        },\n"
        + "        {\n"
        + "          \"columnName\": \"phoneNumber\",\n"
        + "          \"sensitive\": true,\n"
        + "          \"rules\": [\n"
        + "            {\n"
        + "              \"className\": \"uk.gov.ons.ssdc.common.validation.RegexRule\",\n"
        + "              \"expression\": \"^07[0-9]{9}$\"\n"
        + "            }\n"
        + "          ]\n"
        + "        }\n"
        + "      ],\n"
        + "      \"allowedPrintFulfilments\": [\n"
        + "        {\n"
        + "          \"packCode\": \"replace-uac-en\",\n"
        + "          \"description\": \"Replacement UAC - English\",\n"
        + "          \"metadata\": {\n"
        + "            \"suitableRegions\": [\n"
        + "              \"E\",\n"
        + "              \"N\"\n"
        + "            ]\n"
        + "          }\n"
        + "        },\n"
        + "        {\n"
        + "          \"packCode\": \"replace-uac-cy\",\n"
        + "          \"description\": \"Replacement UAC - English & Welsh\",\n"
        + "          \"metadata\": {\n"
        + "            \"suitableRegions\": [\n"
        + "              \"W\"\n"
        + "            ]\n"
        + "          }\n"
        + "        }\n"
        + "      ],\n"
        + "      \"allowedSmsFulfilments\": [\n"
        + "        {\n"
        + "          \"packCode\": \"replace-uac-en\",\n"
        + "          \"description\": \"Replacement UAC - English3\",\n"
        + "          \"metadata\": {\n"
        + "            \"suitableRegions\": [\n"
        + "              \"E\",\n"
        + "              \"N\"\n"
        + "            ]\n"
        + "          }\n"
        + "        },\n"
        + "        {\n"
        + "          \"packCode\": \"replace-uac-cy\",\n"
        + "          \"description\": \"Replacement UAC - English & Welsh\",\n"
        + "          \"metadata\": {\n"
        + "            \"suitableRegions\": [\n"
        + "              \"W\"\n"
        + "            ],\n"
        + "            \"Another\": \"PMB\"\n"
        + "          }\n"
        + "        }\n"
        + "      ],\n"
        + "      \"allowedEmailFulfilments\": [],\n"
        + "      \"metadata\": {\n"
        + "        \"ex_e4\": true\n"
        + "      }\n"
        + "    }\n"
        + "";
    
    JsonNode p = mapper.readTree(surveyStr);
    
    SurveyUpdate p2 = mapper.readValue(surveyStr.getBytes(), SurveyUpdate.class);
    
    System.out.println("p:"  + p);
    System.out.println("p2: " + p2);
//    System.out.println(p.getPackCode());
    
    String p2Str = mapper.writeValueAsString(p2);
    System.out.println("After: " + p2Str);
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

}
