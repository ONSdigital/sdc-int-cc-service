package uk.gov.ons.ctp.integration.contactcentresvc.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import javax.persistence.AttributeConverter;
import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;

@Slf4j
public class HashMapConverter implements AttributeConverter<Map<String, Object>, String> {

  private ObjectMapper mapper = new ObjectMapper();
  CCSvcBeanMapper ccMapper = new CCSvcBeanMapper();

  @Override
  public String convertToDatabaseColumn(Map<String, Object> customerInfo) {

    String customerInfoJson = null;
    try {
      customerInfoJson = mapper.writeValueAsString(customerInfo);
    } catch (final JsonProcessingException e) {
      log.error("JSON writing error", e);
    }

    return customerInfoJson;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, Object> convertToEntityAttribute(String customerInfoJSON) {

    Map<String, Object> customerInfo = null;
    try {
      customerInfo = mapper.readValue(customerInfoJSON, Map.class);
    } catch (final IOException e) {
      log.error("JSON reading error", e);
    }

    return customerInfo;
  }
}
