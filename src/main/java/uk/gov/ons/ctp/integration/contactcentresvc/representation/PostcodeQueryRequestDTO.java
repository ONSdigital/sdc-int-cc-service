package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.Data;
import lombok.ToString;
import uk.gov.ons.ctp.integration.contactcentresvc.Constants;

@Data
@ToString
public class PostcodeQueryRequestDTO {
  @NotBlank
  @Pattern(regexp = Constants.POSTCODE_RE)
  private String postcode;

  @Min(0)
  @Max(5000)
  private Integer offset = 0;

  @Min(0)
  @Max(5000)
  private Integer limit = 100;

  /**
   * Create method, as default values don't work if using an all args constructor or a builder. So
   * this is the least bad workaround.
   *
   * @param postcode to search for.
   * @param offset index of first result to return.
   * @param limit maximum number of addresses to return.
   * @return a newly constructed dto.
   */
  public static PostcodeQueryRequestDTO create(String postcode, int offset, int limit) {
    PostcodeQueryRequestDTO dto = new PostcodeQueryRequestDTO();

    dto.setPostcode(postcode);
    dto.setOffset(0);
    dto.setLimit(limit);

    return dto;
  }
}
