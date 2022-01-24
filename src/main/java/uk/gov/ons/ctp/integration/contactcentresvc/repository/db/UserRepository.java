package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import uk.gov.ons.ctp.integration.contactcentresvc.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByName(String name);
}
