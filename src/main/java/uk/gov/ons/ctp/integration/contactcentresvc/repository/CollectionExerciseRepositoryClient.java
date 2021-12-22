package uk.gov.ons.ctp.integration.contactcentresvc.repository;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CollectionExerciseRepository;

@Slf4j
@Service
public class CollectionExerciseRepositoryClient {
  private CollectionExerciseRepository collexRepo;

  public CollectionExerciseRepositoryClient(CollectionExerciseRepository collexRepo) {
    this.collexRepo = collexRepo;
  }

  public CollectionExercise getCollectionExerciseById(UUID collexId) throws CTPException {
    log.debug("Find collectionExercise details by ID", kv("collexId", collexId));

    CollectionExercise collex =
        collexRepo
            .findById(collexId)
            .orElseThrow(
                () ->
                    new CTPException(
                        Fault.RESOURCE_NOT_FOUND,
                        "Could not find collectionExercise for ID: " + collexId));

    log.debug("Found collectionExercise details for collex ID", kv("collexId", collexId));
    return collex;
  }
}
