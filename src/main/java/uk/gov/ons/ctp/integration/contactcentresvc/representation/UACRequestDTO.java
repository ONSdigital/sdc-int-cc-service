package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AD request object for get uac for case
 *
 * @author philwhiles
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UACRequestDTO {

  @NotNull private String adLocationId;

  @NotNull private Boolean individual;
}
