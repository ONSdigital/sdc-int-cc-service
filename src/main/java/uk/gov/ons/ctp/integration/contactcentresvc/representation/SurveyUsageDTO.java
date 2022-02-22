package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.SurveyType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyUsageDTO {
  private SurveyType surveyType;
}
