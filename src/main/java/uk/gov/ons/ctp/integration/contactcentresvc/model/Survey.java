package uk.gov.ons.ctp.integration.contactcentresvc.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
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

  @Type(type = "jsonb")
  private Object sampleDefinition;

  private String sampleDefinitionUrl;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private Map<String, ?> metadata;

  @JsonIgnore
  @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Product> allowedFulfilments;
}
