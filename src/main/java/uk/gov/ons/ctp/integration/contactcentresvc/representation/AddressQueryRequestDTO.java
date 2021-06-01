package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class AddressQueryRequestDTO {

  @Size(max = 250)
  @NotBlank
  private String input;

  @Min(0)
  @Max(250)
  private Integer offset = 0;

  @Min(0)
  @Max(100)
  private Integer limit = 100;

  /**
   * Create method, as default values don't work if using an all args constructor or a builder. So
   * this is the least bad workaround.
   *
   * @param input address, or part off, to search for.
   * @param offset index of first result to return.
   * @param limit maximum number of addresses to return.
   * @return a newly constructed dto.
   */
  public static AddressQueryRequestDTO create(String input, int offset, int limit) {
    AddressQueryRequestDTO dto = new AddressQueryRequestDTO();

    dto.setInput(input);
    dto.setOffset(0);
    dto.setLimit(limit);

    return dto;
  }
}
