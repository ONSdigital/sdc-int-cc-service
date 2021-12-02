package uk.gov.ons.ctp.integration.contactcentresvc.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

/**
 * Representation of Survey from database table.
 *
 * <p>Implementation note: avoid Lombok Data annotation, since generated toString, equals and
 * hashcode are considered dangerous in combination with Entity annotation.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Entity
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
@Table(name = "survey")
public class Survey {

  @ToString.Include @Id private UUID id;

  @ToString.Include private String name;

  private String sampleDefinitionUrl;

  @Type(type = "jsonb")
  private Object sampleDefinition;

  @JsonIgnore
  @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Product> allowedFulfilments;

  // PMB
  //  @JsonSetter("sampleDefinition")
  //  void setMetadataFromJson(String jsonString) throws JsonMappingException,
  // JsonProcessingException {
  //    ObjectMapper mapper = new ObjectMapper();
  //    this.sampleDefinition = mapper.readTree(jsonString);
  //  }
  //
  //  @JsonSetter("sampleDefinition")
  //  public void setSampleDefinition(String jsonString) throws JsonMappingException,
  // JsonProcessingException {
  //    ObjectMapper mapper = new ObjectMapper();
  //    this.sampleDefinition = mapper.readTree(jsonString);
  //  }
}
