package gov.ca.cwds.idm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import gov.ca.cwds.idm.persistence.model.OperationType;
import java.io.Serializable;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class UserIdAndOperation implements Serializable {

  private static final long serialVersionUID = -8333565041593900653L;

  private final String id;

  private final OperationType operation;

  public UserIdAndOperation(String id, OperationType operation) {
    this.id = id;
    this.operation = operation;
  }

  public String getId() {
    return id;
  }

  public OperationType getOperation() {
    return operation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof UserIdAndOperation)) {
      return false;
    }
    UserIdAndOperation that = (UserIdAndOperation) o;
    return operation == that.operation &&
        Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, operation);
  }
}
