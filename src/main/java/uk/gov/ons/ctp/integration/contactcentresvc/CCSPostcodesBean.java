package uk.gov.ons.ctp.integration.contactcentresvc;

import static uk.gov.ons.ctp.common.log.ScopedStructuredArguments.kv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.integration.contactcentresvc.config.AppConfig;

@Slf4j
@Component
public class CCSPostcodesBean {
  @Autowired private AppConfig appConfig;

  private Set<String> ccsPostcodes;

  public boolean isInCCSPostcodes(String rawPostcode) {
    String cleanedPostcode = normalisePostcode(rawPostcode);
    return ccsPostcodes.contains(cleanedPostcode);
  }

  @PostConstruct
  void init() {
    this.ccsPostcodes = new HashSet<>();
    String strPostcodePath = appConfig.getCcsPostcodes().getCcsPostcodePath();

    boolean isRunningCC = appConfig.getChannel() == Channel.CC;

    if (isRunningCC) {
      try (BufferedReader br = new BufferedReader(new FileReader(strPostcodePath))) {
        String rawPostcode;
        while ((rawPostcode = br.readLine()) != null) {
          String postcode = normalisePostcode(rawPostcode);
          ccsPostcodes.add(postcode);
        }
        log.info("Read ccsPostcodes from file", kv("size", ccsPostcodes.size()));
      } catch (IOException e) {
        if (new File(strPostcodePath).exists()) {
          log.error(
              "APPLICATION IS MISCONFIGURED - Unable to read in postcodes from file."
                  + " Using postcodes from application.yml instead.",
              kv("strPostcodePath", strPostcodePath),
              e);
        } else {
          log.error(
              "APPLICATION IS MISCONFIGURED - Postcode file doesn't exist."
                  + " Using postcodes from application.yml instead.",
              kv("strUprnBlacklistPath", strPostcodePath));
        }
        ccsPostcodes =
            appConfig.getCcsPostcodes().getCcsDefaultPostcodes().stream()
                .map(p -> normalisePostcode(p))
                .collect(Collectors.toSet());
      }
    }
  }

  private String normalisePostcode(String rawPostcode) {
    String normalisedPostcode = rawPostcode.trim();
    normalisedPostcode = normalisedPostcode.replace(" ", "");
    return normalisedPostcode;
  }
}
