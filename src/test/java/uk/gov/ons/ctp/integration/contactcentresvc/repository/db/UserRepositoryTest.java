package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.utility.ParallelTestLocks;
import uk.gov.ons.ctp.integration.contactcentresvc.event.EventToSendPoller;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Role;
import uk.gov.ons.ctp.integration.contactcentresvc.model.User;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test-containers-flyway")
@Testcontainers
@TestPropertySource(properties = {"GOOGLE_CLOUD_PROJECT=sdc-cc-test"})
@MockBean({EventToSendPoller.class, EventPublisher.class})
@MockBean(name = "caseEventInbound", value = PubSubInboundChannelAdapter.class)
@MockBean(name = "surveyEventInbound", value = PubSubInboundChannelAdapter.class)
@MockBean(name = "collectionExerciseEventInbound", value = PubSubInboundChannelAdapter.class)
@ResourceLock(value = ParallelTestLocks.SPRING_TEST, mode = READ_WRITE)
@Transactional
public class UserRepositoryTest {
  private static final UUID FRED_UUID = UUID.fromString("5d788f49-a256-4ae3-9fcf-5d59e8ad4228");
  private static final UUID JOE_UUID = UUID.fromString("ff838710-4b6f-11ec-b4ab-4c3275913db5");
  private static final UUID SHOPKEEPER_UUID =
      UUID.fromString("2733fc72-4b70-11ec-adec-4c3275913db5");

  @Autowired private UserRepository userRepo;

  @Autowired private RoleRepository roleRepo;

  @Autowired private TransactionalOps txOps;

  @BeforeEach
  public void setup() {
    userRepo.deleteAll();
    roleRepo.deleteAll();
  }

  @Test
  public void shouldBeCleanBeforeEachTest() {
    assertTrue(userRepo.findAll().isEmpty());
    assertTrue(roleRepo.findAll().isEmpty());
  }

  @Test
  public void shouldFindUser() {
    assertTrue(userRepo.findAll().isEmpty());
    assertTrue(roleRepo.findAll().isEmpty());

    User user = User.builder().id(FRED_UUID).name("Fred").build();
    userRepo.save(user);

    User fred = userRepo.findByName("Fred");
    assertEquals("Fred", fred.getName());
    assertEquals(FRED_UUID, fred.getId());
    assertTrue(fred.isActive());
  }

  @Test
  public void shouldCreateUser() {
    User user = User.builder().id(JOE_UUID).name("Joe").build();
    userRepo.save(user);
    assertEquals("Joe", userRepo.findByName("Joe").getName());
    assertEquals("Joe", userRepo.getById(user.getId()).getName());
  }

  @Test
  public void shouldDeleteUser() {
    User user = User.builder().id(JOE_UUID).name("Joe").build();
    userRepo.save(user);
    assertNotNull(userRepo.findByName("Joe"));
    userRepo.delete(user);
    assertNull(userRepo.findByName("Joe"));
  }

  /**
   * Separate class that can create database items and commit the results so that subsequent
   * operations can see the effect.
   */
  @Component
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public static class TransactionalOps {
    private UserRepository userRepo;
    private RoleRepository roleRepo;

    public TransactionalOps(UserRepository userRepo, RoleRepository roleRepo) {
      this.userRepo = userRepo;
      this.roleRepo = roleRepo;
    }

    public void createJoeTheShopkeeper() {
      Role role = Role.builder().id(SHOPKEEPER_UUID).name("shopkeeper").build();
      roleRepo.save(role);
      var roles = new ArrayList<Role>();
      roles.add(role);
      User user = User.builder().id(JOE_UUID).name("Joe").userRoles(roles).build();
      userRepo.save(user);
    }

    public void verifyJoeTheShopkeeper() {
      User joe = userRepo.findByName("Joe");
      Role shopkeeper = roleRepo.findByName("shopkeeper");
      assertEquals(shopkeeper, joe.getUserRoles().get(0));
      assertEquals(joe, shopkeeper.getUsers().get(0));
    }

    public void createJoeTheShopkeeperAdmin() {
      Role role = Role.builder().id(SHOPKEEPER_UUID).name("shopkeeper").build();
      roleRepo.save(role);
      var roles = new ArrayList<Role>();
      roles.add(role);
      User user = User.builder().id(JOE_UUID).name("Joe").adminRoles(roles).build();
      userRepo.save(user);
    }

    public void verifyJoeTheShopkeeperAdmin() {
      User joe = userRepo.findByName("Joe");
      Role shopkeeper = roleRepo.findByName("shopkeeper");
      assertEquals(shopkeeper, joe.getAdminRoles().get(0));
      assertEquals(joe, shopkeeper.getAdmins().get(0));
    }
  }

  @Test
  public void shouldAssignRoleToUser() {
    txOps.createJoeTheShopkeeper();
    txOps.verifyJoeTheShopkeeper();
  }

  @Test
  public void shouldAssignAdminRoleToUser() {
    txOps.createJoeTheShopkeeperAdmin();
    txOps.verifyJoeTheShopkeeperAdmin();
  }
}
