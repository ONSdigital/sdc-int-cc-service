package uk.gov.ons.ctp.integration.contactcentresvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Case;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CaseRepository;

/** Example endpoint to access data from database */
@RestController
@RequestMapping(value = "/data", produces = "application/json")
public class DataEndpoint {
  private static final Logger log = LoggerFactory.getLogger(DataEndpoint.class);

  @Autowired CaseRepository caseRepo;

  @RequestMapping(value = "/case", method = RequestMethod.GET)
  public ResponseEntity<List<Case>> findCases() {
    log.info("Entering GET findCases");

    List<Case> result = caseRepo.findAll();

    return ResponseEntity.ok(result);
  }
}
