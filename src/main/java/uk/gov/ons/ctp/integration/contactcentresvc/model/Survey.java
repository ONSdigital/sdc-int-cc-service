package uk.gov.ons.ctp.integration.contactcentresvc.model;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@Getter
@Setter
@Entity
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
@Table(name = "survey")
public class Survey {

  @Id private UUID id;

  private String name;

  private String sampleDefinitionUrl;

  @Type(type = "jsonb")
  private Object sampleDefinition;
}
