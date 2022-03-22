package uk.gov.ons.ctp.integration.contactcentresvc.representation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The request object when contact centre requests to invalidate a case
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvalidateCaseDTO {
    private String note;
}
