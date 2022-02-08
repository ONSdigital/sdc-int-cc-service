package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import uk.gov.ons.ctp.common.domain.SurveyType;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyDTO {
  private UUID surveyId;
  private List<ProductDTO> allowedFulfilments;
  private SurveyType surveyType;
  private String surveyName;
}
