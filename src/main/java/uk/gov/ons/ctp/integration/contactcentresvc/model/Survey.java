package uk.gov.ons.ctp.integration.contactcentresvc.model;

import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Representation of Survey from database table.
 *
 * <p>Implementation note: avoid Lombok Data annotation, since generated toString, equals and
 * hashcode are considered dangerous in combination with Entity annotation.
 */
@Getter
@Setter
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
  private JsonNode sampleDefinition;
  
  @JsonIgnore
  @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Product> allowedFulfilments;

//  @JsonSetter("sampleDefinition")
//  void setMetadataFromJson(String jsonString) throws JsonMappingException, JsonProcessingException {
//    ObjectMapper mapper = new ObjectMapper();
//    this.sampleDefinition = mapper.readTree(jsonString);
//  }

  public void setSampleDefinition(String jsonString) throws JsonMappingException, JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    this.sampleDefinition = mapper.readTree(jsonString);
  }

}
