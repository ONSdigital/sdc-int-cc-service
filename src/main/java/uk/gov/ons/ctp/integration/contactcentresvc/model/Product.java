package uk.gov.ons.ctp.integration.contactcentresvc.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
 * Representation of Product from database table.
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
@Table(name = "product")
public class Product {
  @ToString.Include
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(optional = false)
  private Survey survey;

  @ToString.Include
  @Enumerated(EnumType.STRING)
  private ProductType type;

  @ToString.Include private String packCode;
  @ToString.Include private String description;

  private String metadataAsString;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  @JsonRawValue()
  private String metadataAsJsonb;

  
  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
//  @JsonRawValue()
//  @Convert(converter = HashMapConverter.class)
  private Map<String, ?> metadata;
  
  public void serializeMetadata() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    
//    String productStr = "{\n"
//        + "            \"suitableRegions\": [\n"
//        + "              \"E\",\n"
//        + "              \"N\"\n"
//        + "            ],"
//        + "            \"name\": \"value\""
//        + "}\n";
//  
//    JsonNode p = mapper.readTree(productStr);
//  
//    HashMap<String, Object> p2 = mapper.readValue(productStr.getBytes(), HashMap.class);
//    this.metadata = p2;
    
    this.metadataAsString = mapper.writeValueAsString(metadata);
    
    this.metadataAsJsonb = mapper.writeValueAsString(metadata);
  }
  
  public void deserializeMetadata() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    this.metadata = mapper.readValue(metadataAsString, HashMap.class);
  }
}
