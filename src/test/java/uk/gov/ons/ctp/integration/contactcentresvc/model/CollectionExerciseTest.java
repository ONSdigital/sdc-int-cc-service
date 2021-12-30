package uk.gov.ons.ctp.integration.contactcentresvc.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CollectionExerciseTest {

  CollectionExercise collex;

  @BeforeEach
  public void initCollex() {
    collex = new CollectionExercise();
    collex.setStartDate(LocalDateTime.of(2022, 1, 1, 0, 0, 0));
    collex.setEndDate(LocalDateTime.of(2022, 3, 31, 23, 59, 59));
    collex.setWaveLength(20);
    collex.setNumberOfWaves(3);
  }

  // STRICT TESTS - considers operational part of wave only
  @Test
  public void dateWithinFirstWaveStrict() {
    Optional<Integer> waveOpt = collex.calcWaveForDate(LocalDateTime.of(2022, 1, 10, 12, 0), true);
    assertTrue(waveOpt.isPresent() && waveOpt.get() == 0);
  }

  @Test
  public void dateIsLastOperationalDayOfFirstWaveStrict() {
    Optional<Integer> waveOpt =
        collex.calcWaveForDate(LocalDateTime.of(2022, 1, 20, 23, 59, 59), true);
    assertTrue(waveOpt.isPresent() && waveOpt.get() == 0);
  }

  @Test
  public void dateWithinNonOperationalPeriodOfFirstWaveStrict() {
    Optional<Integer> waveOpt = collex.calcWaveForDate(LocalDateTime.of(2022, 1, 21, 12, 0), true);
    assertTrue(waveOpt.isEmpty());
  }

  @Test
  public void dateWithinSecondWaveStrict() {
    Optional<Integer> waveOpt = collex.calcWaveForDate(LocalDateTime.of(2022, 2, 10, 12, 0), true);
    assertTrue(waveOpt.isPresent() && waveOpt.get() == 1);
  }

  @Test
  public void dateWithinNonOperationalPeriodOfSecondWaveStrict() {
    Optional<Integer> waveOpt = collex.calcWaveForDate(LocalDateTime.of(2022, 2, 25, 12, 0), true);
    assertTrue(waveOpt.isEmpty());
  }

  @Test
  public void dateBeforeCollexStartsStrict() {
    Optional<Integer> waveOpt = collex.calcWaveForDate(LocalDateTime.of(2021, 12, 31, 12, 0), true);
    assertTrue(waveOpt.isEmpty());
  }

  @Test
  public void dateAfterCollexEndsStrict() {
    Optional<Integer> waveOpt = collex.calcWaveForDate(LocalDateTime.of(2022, 4, 1, 12, 0), true);
    assertTrue(waveOpt.isEmpty());
  }

  // RELAXED TESTS - considers wave to be composed of both operational and non operational days
  @Test
  public void dateWithinFirstWaveRelaxed() {
    Optional<Integer> waveOpt = collex.calcWaveForDate(LocalDateTime.of(2022, 1, 10, 12, 0), false);
    assertTrue(waveOpt.isPresent() && waveOpt.get() == 0);
  }

  @Test
  public void dateIsLastOperationalDayOfFirstWaveRelaxed() {
    Optional<Integer> waveOpt =
        collex.calcWaveForDate(LocalDateTime.of(2022, 1, 20, 23, 59, 59), false);
    assertTrue(waveOpt.isPresent() && waveOpt.get() == 0);
  }

  @Test
  public void dateWithinNonOperationalPeriodOfFirstWaveRelaxed() {
    Optional<Integer> waveOpt = collex.calcWaveForDate(LocalDateTime.of(2022, 1, 21, 12, 0), false);
    assertTrue(waveOpt.isPresent() && waveOpt.get() == 0);
  }

  @Test
  public void dateWithinSecondWaveRelaxed() {
    Optional<Integer> waveOpt = collex.calcWaveForDate(LocalDateTime.of(2022, 2, 10, 12, 0), false);
    assertTrue(waveOpt.isPresent() && waveOpt.get() == 1);
  }

  @Test
  public void dateWithinNonOperationalPeriodOfSecondWaveRelaxed() {
    Optional<Integer> waveOpt = collex.calcWaveForDate(LocalDateTime.of(2022, 2, 25, 12, 0), false);
    assertTrue(waveOpt.isPresent() && waveOpt.get() == 1);
  }

  @Test
  public void dateBeforeCollexStartsRelaxed() {
    Optional<Integer> waveOpt =
        collex.calcWaveForDate(LocalDateTime.of(2021, 12, 31, 12, 0), false);
    assertTrue(waveOpt.isEmpty());
  }

  @Test
  public void dateAfterCollexEndsRelaxed() {
    Optional<Integer> waveOpt = collex.calcWaveForDate(LocalDateTime.of(2022, 4, 1, 12, 0), false);
    assertTrue(waveOpt.isEmpty());
  }
}
