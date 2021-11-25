package uk.gov.ons.ctp.integration.contactcentresvc.repository.db;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.ctp.integration.contactcentresvc.model.Product;

public interface ProductRepository extends JpaRepository<Product, UUID> {}
