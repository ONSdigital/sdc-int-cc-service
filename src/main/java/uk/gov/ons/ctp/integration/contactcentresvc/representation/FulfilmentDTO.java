package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.CaseType;

/**
 * The response object representing a single fulfilment record
 *
 * @author philwhiles
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FulfilmentDTO {

  private List<Region> regions;

  private List<CaseType> caseTypes;

  private String fulfilmentCode;

  private String description;

  private DeliveryChannel deliveryChannel;

  private Boolean individual;

  private ProductGroup productGroup;
}
