package uk.gov.ons.ctp.integration.contactcentresvc.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class UacMetadata {

  @Column(name = "wave_num")
  private int wave;
}
