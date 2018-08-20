package gov.ca.cwds.healthcheck;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.amazonaws.services.cognitoidp.model.AWSCognitoIdentityProviderException;

import gov.ca.cwds.idm.service.cognito.CognitoServiceFacade;

@Profile("idm")
@Component(value = "cognitoHealthCheck")
public class CognitoHealthContributor implements HealthIndicator {

  @Autowired
  private CognitoServiceFacade cognitoServiceFacade;

  @Override
  public Health health() {
    try {
      cognitoServiceFacade.healthCheck();
      return Health.status(new Status("UP", "Connection is OK")).build();
    } catch (AWSCognitoIdentityProviderException awsException) {
      return Health.down().withException(awsException).build();
    }
  }

}
