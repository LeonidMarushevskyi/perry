package gov.ca.cwds.idm.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import gov.ca.cwds.idm.persistence.model.OperationType;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class UserAndOperation implements Serializable {

  private static final long serialVersionUID = 2272859030318351815L;

  private final User user;

  private final OperationType operation;

  @JsonCreator
  public UserAndOperation(
      @JsonProperty("user") User user, @JsonProperty("operation") OperationType operation) {
    this.user = user;
    this.operation = operation;
  }

  public User getUser() {
    return user;
  }

  public OperationType getOperation() {
    return operation;
  }
}
