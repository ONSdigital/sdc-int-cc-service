package uk.gov.ons.ctp.integration.contactcentresvc.model;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Data;

@Data
@Entity
public class EventToSend {
  @Id private UUID id;

  @Column(name = "event_type")
  private String type;

  private String payload;
}
