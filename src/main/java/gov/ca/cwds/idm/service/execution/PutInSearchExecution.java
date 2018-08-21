package gov.ca.cwds.idm.service.execution;

import com.amazonaws.services.cognitoidp.model.UserType;
import gov.ca.cwds.idm.persistence.model.UserLog;
import org.springframework.http.ResponseEntity;

public abstract class PutInSearchExecution extends
    OptionalExecution<UserType, ResponseEntity<String>> {

  private OptionalExecution<String, UserLog> userLogExecution;

  public PutInSearchExecution(
      UserType input) {
    super(input);
  }

  public OptionalExecution<String, UserLog> getUserLogExecution() {
    return userLogExecution;
  }

  public void setUserLogExecution(
      OptionalExecution<String, UserLog> userLogExecution) {
    this.userLogExecution = userLogExecution;
  }
}
