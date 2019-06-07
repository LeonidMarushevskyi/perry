package gov.ca.cwds.idm.dto;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.ca.cwds.idm.dto.serializer.UpdatePropertySerializer;
import java.io.Serializable;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class UserUpdate implements Serializable {

  private static final long serialVersionUID = 9182424503106881350L;

  private UpdateProperty<String> email = UpdateProperty.empty();

  private UpdateProperty<Boolean> enabled = UpdateProperty.empty();

  private UpdateProperty<String> phoneNumber = UpdateProperty.empty();

  private UpdateProperty<String> phoneExtensionNumber = UpdateProperty.empty();

  private UpdateProperty<String> cellPhoneNumber = UpdateProperty.empty();

  private UpdateProperty<String> notes = UpdateProperty.empty();

  private UpdateProperty<Set<String>> permissions = UpdateProperty.empty();

  private UpdateProperty<Set<String>> roles = UpdateProperty.empty();

  @JsonSerialize(using = UpdatePropertySerializer.class)
  public UpdateProperty<String> getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = UpdateProperty.of(email);
  }

  @JsonSerialize(using = UpdatePropertySerializer.class)
  public UpdateProperty<Boolean> getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = UpdateProperty.of(enabled);
  }

  @JsonSerialize(using = UpdatePropertySerializer.class)
  public UpdateProperty<String> getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = UpdateProperty.of(phoneNumber);
  }

  @JsonSerialize(using = UpdatePropertySerializer.class)
  public UpdateProperty<String> getPhoneExtensionNumber() {
    return phoneExtensionNumber;
  }

  public void setPhoneExtensionNumber(String phoneExtensionNumber) {
    this.phoneExtensionNumber = UpdateProperty.of(phoneExtensionNumber);
  }

  @JsonSerialize(using = UpdatePropertySerializer.class)
  public UpdateProperty<String> getCellPhoneNumber() {
    return cellPhoneNumber;
  }

  public void setCellPhoneNumber(String cellPhoneNumber) {
    this.cellPhoneNumber = UpdateProperty.of(cellPhoneNumber);
  }

  @JsonSerialize(using = UpdatePropertySerializer.class)
  public UpdateProperty<Set<String>> getPermissions() {
    return permissions;
  }

  public void setPermissions(Set<String> permissions) {
    this.permissions = UpdateProperty.of(permissions);
  }

  @JsonSerialize(using = UpdatePropertySerializer.class)
  public UpdateProperty<Set<String>> getRoles() {
    return roles;
  }

  public void setRoles(Set<String> roles) {
    this.roles = UpdateProperty.of(roles);
  }

  @JsonSerialize(using = UpdatePropertySerializer.class)
  public UpdateProperty<String> getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = UpdateProperty.of(notes);
  }
}
