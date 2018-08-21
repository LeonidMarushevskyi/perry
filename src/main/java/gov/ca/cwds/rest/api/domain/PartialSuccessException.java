package gov.ca.cwds.rest.api.domain;

import gov.ca.cwds.service.messages.MessageCode;
import java.util.Arrays;
import java.util.List;

public class PartialSuccessException extends RuntimeException {

  private static final long serialVersionUID = 7129790376050513025L;

  private final String userId;

  private final MessageCode errorCode;

  private final List<Exception> causes;

  public PartialSuccessException (String userId, String message, MessageCode errorCode, Exception... causes) {
    super(message);
    this.userId = userId;
    this.errorCode = errorCode;
    this.causes = Arrays.asList(causes);
  }

  public String getUserId() {
    return userId;
  }

  public MessageCode getErrorCode() {
    return errorCode;
  }

  public List<Exception> getCauses() {
    return causes;
  }
}
