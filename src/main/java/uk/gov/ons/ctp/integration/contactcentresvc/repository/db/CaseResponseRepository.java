package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CaseResponse;

import java.util.UUID;

public interface CaseResponseRepository extends JpaRepository<CaseResponse, UUID> {
}


