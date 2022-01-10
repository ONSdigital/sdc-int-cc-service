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
import uk.gov.ons.ctp.common.event.model.CollectionExerciseUpdate;
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

    verify(repo).saveAndFlush(collExCaptor.capture());

    CollectionExerciseUpdate payload = event.getPayload().getCollectionExerciseUpdate();
    CollectionExercise persistedCollEx = collExCaptor.getValue();
    verifyMapping(persistedCollEx, payload);
  }

  @Test
  public void shouldRejectFailingSave() {
    when(repo.saveAndFlush(any())).thenThrow(PersistenceException.class);
    assertThrows(PersistenceException.class, () -> target.acceptEvent(event));
  }

  private void verifyMapping(
      CollectionExercise persistedCollEx, CollectionExerciseUpdate collExPayload) {
    assertEquals(UUID.fromString(collExPayload.getSurveyId()), persistedCollEx.getSurvey().getId());
    assertEquals(UUID.fromString(collExPayload.getCollectionExerciseId()), persistedCollEx.getId());
    assertEquals(collExPayload.getName(), persistedCollEx.getName());
    assertEquals(collExPayload.getReference(), persistedCollEx.getReference());

    assertEquals(
        collExPayload.getStartDate().toInstant(),
        persistedCollEx.getStartDate().toInstant(ZoneOffset.UTC));
    assertEquals(
        collExPayload.getEndDate().toInstant(),
        persistedCollEx.getEndDate().toInstant(ZoneOffset.UTC));

    var meta = collExPayload.getMetadata();
    assertAll(
        () -> assertEquals(meta.getNumberOfWaves(), persistedCollEx.getNumberOfWaves()),
        () -> assertEquals(meta.getWaveLength(), persistedCollEx.getWaveLength()),
        () -> assertEquals(meta.getCohorts(), persistedCollEx.getCohorts()),
        () -> assertEquals(meta.getCohortSchedule(), persistedCollEx.getCohortSchedule()));
  }
}
