package uk.gov.ons.ctp.integration.contactcentresvc.model;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import lombok.Data;
import org.hibernate.annotations.Type;

@Data
@Entity
public class EventToSend {
  @Id private UUID id;

  @Column(name = "event_type")
  private String type;

  @Lob
  @Type(type = "org.hibernate.type.BinaryType")
  @Column
  private byte[] payload;

  public void setPayload(String payload) {
    this.payload = payload == null ? null : payload.getBytes();
  }

  public String getPayload() {
    return payload == null ? null : new String(payload);
  }
}
