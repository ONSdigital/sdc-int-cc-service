package uk.gov.ons.ctp.integration.contactcentresvc.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Representation of Collection Exercise from database table.
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
@Table(name = "collection_exercise")
public class CollectionExercise {
  @ToString.Include @Id private UUID id;

  @ManyToOne(optional = false)
  private Survey survey;

  @ToString.Include private String name;

  private String reference;
  private LocalDateTime startDate;
  private LocalDateTime endDate;
  private int cohortSchedule;
  private int cohorts;
  private int numberOfWaves;
  private int waveLength;

  /**
   * Calculate if a given date is within the window of a wave within this exercise, and return its
   * wave num, optionally taking into account the non operational days at the end of each wave
   *
   * <p>This calculation applies a relaxed or strict test, as current requirements do not define if
   * CC is to allow tele capture when today is within a wave but after its operational period. We do
   * not know at this stage which of the two tests will be used or if we need to be both strict and
   * relaxed in different scenarios.
   *
   * <p>Currently only the CC needs to perform this calc - if this becomes a concern for RH also,
   * this should be moved to the EventPublishers model object, CollectionExerciseUpdate, or a util
   * class that takes that object as an arg.
   *
   * @param date the given date
   * @param strict true if the date is not allowed within the non operational window of a wave
   * @return the wave num else empty
   */
  public Optional<Integer> calcWaveForDate(LocalDateTime date, boolean strict) {
    long collexDuration = ChronoUnit.DAYS.between(startDate, endDate);
    long periodBetweenWaves = collexDuration / numberOfWaves;
    long waveCutOffDays = strict ? waveLength : periodBetweenWaves;
    int wave = -1;
    for (int n = 0; n < numberOfWaves; n++) {
      LocalDateTime waveStart = startDate.plusDays(n * periodBetweenWaves);
      if (date.isAfter(waveStart) && date.isBefore(waveStart.plusDays(waveCutOffDays))) {
        wave = n;
        break;
      }
    }
    return wave == -1 ? Optional.empty() : Optional.of(wave);
  }
}
