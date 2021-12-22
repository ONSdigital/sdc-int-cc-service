package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.event.model.CaseUpdate;

public class CaseDTOTest {
  private static final UniquePropertyReferenceNumber A_UPRN =
      new UniquePropertyReferenceNumber("3341111111111");
  private static final UUID A_UUID = UUID.randomUUID();

  private CaseDTO aCase;

  private ObjectMapper getObjectMapper() {
    return new ObjectMapper();
  }

  @SneakyThrows
  private <T> T deserialise(String json, Class<T> clazz) {
    return getObjectMapper().readValue(json, clazz);
  }

  @SneakyThrows
  private String prettySerialise(Object o) {
    return getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(o);
  }

  @Test
  public void shouldSerialiseAndDeserialise() {
    aCase = new CaseDTO();
    aCase.setId(A_UUID);
    aCase.setSample(Map.of(CaseUpdate.ATTRIBUTE_UPRN, A_UPRN.toString()));

    String json = prettySerialise(aCase);

    CaseDTO deser = deserialise(json, CaseDTO.class);

    assertEquals(A_UUID, deser.getId());
    assertEquals(A_UPRN.toString(), deser.getSample().get(CaseUpdate.ATTRIBUTE_UPRN));
  }
}
