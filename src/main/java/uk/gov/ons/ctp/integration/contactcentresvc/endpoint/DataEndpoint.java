package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;

/** Example endpoint to access data from database */
@Slf4j
@RestController
@RequestMapping(value = "/data", produces = "application/json")
public class DataEndpoint {
  @Autowired CaseRepository caseRepo;

  @RequestMapping(value = "/case", method = RequestMethod.GET)
  public ResponseEntity<List<Case>> findCases() {
    log.info("Entering GET findCases");

    List<Case> result = caseRepo.findAll();

    return ResponseEntity.ok(result);
  }
}
