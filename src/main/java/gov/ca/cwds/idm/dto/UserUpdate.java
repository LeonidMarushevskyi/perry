package gov.ca.cwds.idm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.io.Serializable;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
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

  public UpdateProperty<String> getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = UpdateProperty.of(email);
  }

  public UpdateProperty<Boolean> getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = UpdateProperty.of(enabled);
  }

  public UpdateProperty<String> getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = UpdateProperty.of(phoneNumber);
  }

  public UpdateProperty<String> getPhoneExtensionNumber() {
    return phoneExtensionNumber;
  }

  public void setPhoneExtensionNumber(String phoneExtensionNumber) {
    this.phoneExtensionNumber = UpdateProperty.of(phoneExtensionNumber);
  }

  public UpdateProperty<String> getCellPhoneNumber() {
    return cellPhoneNumber;
  }

  public void setCellPhoneNumber(String cellPhoneNumber) {
    this.cellPhoneNumber = UpdateProperty.of(cellPhoneNumber);
  }

  public UpdateProperty<Set<String>> getPermissions() {
    return permissions;
  }

  public void setPermissions(Set<String> permissions) {
    this.permissions = UpdateProperty.of(permissions);
  }

  public UpdateProperty<Set<String>> getRoles() {
    return roles;
  }

  public void setRoles(Set<String> roles) {
    this.roles = UpdateProperty.of(roles);
  }

  public UpdateProperty<String> getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = UpdateProperty.of(notes);
  }

  public static class UpdateProperty<T> implements Serializable {

    private static final long serialVersionUID = -8748009502032919925L;

    private final boolean isSet;
    private final T value;

    private UpdateProperty(boolean isSet, T value) {
      this.isSet = isSet;
      this.value = value;
    }

    static <T> UpdateProperty<T> empty() {
      return new UpdateProperty<>(false, null);
    }

    static <T> UpdateProperty<T> of(T value) {
      return new UpdateProperty<>(true, value);
    }

    public boolean isSet() {
      return isSet;
    }

    public T get() {
      return value;
    }
  }
}
