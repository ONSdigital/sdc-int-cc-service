package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseInteractionDetailsDTO {

  public String interactionSource;

  public String interaction;

  public String subInteraction;

  public String note;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
  LocalDateTime createdDateTime;

  public String userName;
}
