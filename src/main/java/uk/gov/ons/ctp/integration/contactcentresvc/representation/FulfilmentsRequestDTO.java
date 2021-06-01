package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.CaseType;

/**
 * The request object when contact centre requests a list of fulfilments to be returned
 *
 * @author philwhiles
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FulfilmentsRequestDTO {

  private CaseType caseType;

  private Region region;

  private DeliveryChannel deliveryChannel;

  private Boolean individual;

  private ProductGroup productGroup;
}
