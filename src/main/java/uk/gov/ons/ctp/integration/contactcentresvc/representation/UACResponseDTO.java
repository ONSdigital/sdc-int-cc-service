package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The response object returned to all POST that do not require any other returned data. The id is
 * to be the caseId/caseRef/uprn if the respective endpoint has such a path param, else null The
 * date-time to be the time response made
 *
 * @author philwhiles
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UACResponseDTO {

  private String id;

  private String uac;

  private Date dateTime;
}
