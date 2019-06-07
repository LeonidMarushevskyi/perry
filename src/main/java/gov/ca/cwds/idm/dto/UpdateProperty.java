package gov.ca.cwds.idm.dto;

import java.io.Serializable;
import java.util.NoSuchElementException;

public class UpdateProperty<T> implements Serializable {

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
//    if (!isSet) {
//      throw new NoSuchElementException("property is not set");
//    }
    return value;
  }
}