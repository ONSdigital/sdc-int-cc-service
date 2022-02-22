package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.ctp.common.domain.SurveyType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.SurveyUsage;

public interface UserSurveyUsageRepository extends JpaRepository<SurveyUsage, UUID> {
  Optional<SurveyUsage> findBySurveyType(SurveyType surveyType);
}
