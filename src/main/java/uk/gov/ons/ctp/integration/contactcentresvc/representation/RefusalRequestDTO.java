package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.Date;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The request object when contact centre registers a refusal
 *
 * @author philwhiles
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefusalRequestDTO {

  /*
  refusal_json = {
      'caseId': case_id,
      'dateTime': datetime.now(utc).isoformat(),
      'agentId': '13',
      'reason': reason,
      'isHouseholder': 'false'
  }
     */

  @NotNull private UUID caseId;

  @NotNull private Integer agentId;

  @NotNull private Reason reason;

  @NotNull private Boolean isHouseholder;

  @NotNull private Date dateTime;
}
