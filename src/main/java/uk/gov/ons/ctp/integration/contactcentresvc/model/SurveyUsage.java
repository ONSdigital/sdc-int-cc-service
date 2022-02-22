package uk.gov.ons.ctp.integration.contactcentresvc.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import uk.gov.ons.ctp.common.domain.SurveyType;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "survey_usage")
public class SurveyUsage {
  @ToString.Include @Id private UUID id;

  @ToString.Include
  @Enumerated(EnumType.STRING)
  private SurveyType surveyType;

  @JsonIgnore
  @ManyToMany(mappedBy = "surveyUsages")
  private List<User> users;
}
