package uk.gov.ons.ctp.integration.contactcentresvc.event;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneOffset;
import java.util.UUID;
import javax.persistence.PersistenceException;
import ma.glasnost.orika.MapperFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdateEvent;
import uk.gov.ons.ctp.integration.contactcentresvc.CCSvcBeanMapper;
import uk.gov.ons.ctp.integration.contactcentresvc.model.CollectionExercise;
import uk.gov.ons.ctp.integration.contactcentresvc.repository.db.CollectionExerciseRepository;

@ExtendWith(MockitoExtension.class)
public class CollectionExerciseEventReceiverTest {

  @Mock private CollectionExerciseRepository repo;
  @Spy private MapperFacade mapper = new CCSvcBeanMapper();

  @InjectMocks private CollectionExerciseEventReceiver target;

  @Captor private ArgumentCaptor<CollectionExercise> collExCaptor;

  private CollectionExerciseUpdateEvent event;

  @BeforeEach
  public void setup() {
    event = FixtureHelper.loadPackageFixtures(CollectionExerciseUpdateEvent[].class).get(0);
  }

  @Test
  public void shouldReceiveSurveyUpdateEvent() {
    target.acceptEvent(event);

    verify(repo).save(collExCaptor.capture());

    uk.gov.ons.ctp.common.event.model.CollectionExercise payload =
        event.getPayload().getCollectionExerciseUpdate();
    CollectionExercise collEx = collExCaptor.getValue();
    verifyMapping(collEx, payload);
  }

  @Test
  public void shouldRejectFailingSave() {
    when(repo.save(any())).thenThrow(PersistenceException.class);
    assertThrows(PersistenceException.class, () -> target.acceptEvent(event));
  }

  private void verifyMapping(
      CollectionExercise collEx,
      uk.gov.ons.ctp.common.event.model.CollectionExercise collExUpdate) {
    assertEquals(UUID.fromString(collExUpdate.getSurveyId()), collEx.getSurvey().getId());
    assertEquals(UUID.fromString(collExUpdate.getCollectionExerciseId()), collEx.getId());
    assertEquals(collExUpdate.getName(), collEx.getName());
    assertEquals(collExUpdate.getReference(), collEx.getReference());

    assertEquals(
        collExUpdate.getStartDate().toInstant(), collEx.getStartDate().toInstant(ZoneOffset.UTC));
    assertEquals(
        collExUpdate.getEndDate().toInstant(), collEx.getEndDate().toInstant(ZoneOffset.UTC));

    var meta = collExUpdate.getMetadata();
    assertAll(
        () -> assertEquals(meta.getNumberOfWaves(), collEx.getNumberOfWaves()),
        () -> assertEquals(meta.getWaveLength(), collEx.getWaveLength()),
        () -> assertEquals(meta.getCohorts(), collEx.getCohorts()),
        () -> assertEquals(meta.getCohortSchedule(), collEx.getCohortSchedule()));
  }
}
