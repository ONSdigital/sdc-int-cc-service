package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.integration.contactcentresvc.model.EventToSend;

public class EventToSendRepositoryIT3Works extends PostgresTestBase {

  @Autowired private EventToSendRepository repo;
  @Autowired private TransactionalOps3 txOps;

  {
    System.out.println("-------------------- starting ");
  }
  
  @BeforeEach
  public void setup() {
    txOps.deleteAll();
  }

  @Test
  public void shouldLimitEvents() {
    assertEquals(10, 9);
  }

  /**
   * Separate class that can create/update database items and commit the results so that subsequent
   * operations can see the effect.
   */
  @Component
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public static class TransactionalOps3 {
    private EventToSendRepository repo;

    public TransactionalOps3(EventToSendRepository repo) {
      this.repo = repo;
    }

    public void deleteAll() {
      repo.deleteAll();
    }

//    public void createEvent(LocalDateTime created) {
//      Survey event =
//          Survey.builder()
//              .id(UUID.randomUUID())
//              .name("TestSurvey")
//              .sampleDefinitionUrl("http://hatSizeSurvey.gov.uk")
//              .sampleDefinition("[{\"columnName\":\"addressLine1\",\"rules\":[{\"className\":\"uk.gov.ons.ssdc.common.validation.MandatoryRule\"},{\"className\":\"uk.gov.ons.ssdc.common.validation.LengthRule\",\"maxLength\":60}]}]")
//              //.type(TopicType.SURVEY_UPDATE.name())
//              //.createdDateTime(created)
//              .build();
///*
//  private String sampleDefinitionUrl;
//
//  @Type(type = "jsonb")
//  private Object sampleDefinition;
//
//  @JsonIgnore
//  @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
//  private List<Product> allowedPrintFulfilments;
//
//  @JsonIgnore
//  @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
//  private List<Product> allowedSmsFulfilments;
//
//  @JsonIgnore
//  @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
//  private List<Product> allowedEmailFulfilments;
//
// */
//      repo.save(event);
//    }
  }
}
