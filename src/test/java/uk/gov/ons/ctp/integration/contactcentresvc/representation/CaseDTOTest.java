package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.Test;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;

public class CaseDTOTest {
  private static final UniquePropertyReferenceNumber A_UPRN =
      new UniquePropertyReferenceNumber("3341111111111");
  private static final UniquePropertyReferenceNumber ANOTHER_UPRN =
      new UniquePropertyReferenceNumber("1347459999");
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
    aCase.setUprn(A_UPRN);
    aCase.setEstabUprn(ANOTHER_UPRN);

    String json = prettySerialise(aCase);

    CaseDTO deser = deserialise(json, CaseDTO.class);

    assertEquals(A_UUID, deser.getId());
    assertEquals(A_UPRN, deser.getUprn());
    assertEquals(ANOTHER_UPRN, deser.getEstabUprn());
  }
}
