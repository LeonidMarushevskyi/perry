package gov.ca.cwds.idm.dto.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import gov.ca.cwds.idm.dto.UserUpdate.UpdateProperty;
import java.io.IOException;

public class UpdatePropertySerializer<T> extends JsonSerializer<UpdateProperty<T>> {

  @Override
  public void serialize(
      UpdateProperty<T> updateProperty, JsonGenerator jsonGenerator,
      SerializerProvider serializerProvider) throws IOException {
    if(updateProperty.isSet()) {
      jsonGenerator.writeObject(updateProperty.get());
    }
  }

  @Override
  public boolean isEmpty(SerializerProvider provider, UpdateProperty<T> updateProperty) {
    return (!updateProperty.isSet());
  }
}
