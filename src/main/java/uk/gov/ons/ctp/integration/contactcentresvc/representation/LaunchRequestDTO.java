package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The request object when contact centre requests an EQ launch URL
 *
 * @author philwhiles
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaunchRequestDTO {

  @NotNull private Integer agentId;

  @NotNull private Boolean individual;
}
