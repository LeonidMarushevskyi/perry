package gov.ca.cwds.smoketest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import gov.ca.cwds.security.jwt.JwtService;

@Component
public class JwtCheck implements HealthIndicator {

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtCheck.class);

  private final static String TEST_IDENTITY = "identity";

  private JwtService jwtService;

  @Override
  public Health health() {
    LOGGER.debug("JWT health check: start");
    try {
      final String jwt = jwtService.generate("id", "subject", TEST_IDENTITY);
      final String jwtContent = jwtService.validate(jwt);
      if (!TEST_IDENTITY.equals(jwtContent)) {
        LOGGER.error("JWT health check: IDENTITY CHECK FAILED!");
        return Health.down().withDetail("message", "Token validation failed").build();
      }

      final Health ret = Health.up().build();
      LOGGER.info("JWT health check: health: {}", ret);
      return ret;
    } catch (Exception e) {
      LOGGER.error("JWT health check: ERROR! {}", e.getMessage(), e);
      return Health.down().withException(e).build();
    }
  }

  @Autowired
  public void setJwtService(JwtService jwtService) {
    this.jwtService = jwtService;
  }

}
