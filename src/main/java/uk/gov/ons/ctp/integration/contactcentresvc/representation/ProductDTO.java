package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.DeliveryChannel;
import uk.gov.ons.ctp.common.domain.ProductGroup;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
  private UUID id;
  private DeliveryChannel deliveryChannel;
  private ProductGroup productGroup;
  private String packCode;
  private String description;
  private Map<String, Object> metadata;
}
