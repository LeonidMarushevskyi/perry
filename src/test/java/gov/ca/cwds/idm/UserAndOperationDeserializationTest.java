package gov.ca.cwds.idm;

import static io.dropwizard.testing.FixtureHelpers.fixture;

import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.ca.cwds.idm.dto.UserAndOperation;
import gov.ca.cwds.idm.persistence.model.OperationType;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;

public class UserAndOperationDeserializationTest {

  @Test
  public void testDeserialization() throws Exception {
    String json = fixture("fixtures/idm/failed-operations/users-and-operations.json");

    List<UserAndOperation> objects = new ObjectMapper().readValue(json, new TypeReference<List<UserAndOperation>>(){});

    assertTrue(objects.size() == 2);
    UserAndOperation first = objects.get(0);

    if (first.getUser().getId().equals("24051d54-9321-4dd2-a92f-6425d6c455be")){
      assertTrue(first.getOperation() == OperationType.UPDATE);
      assertTrue(first.getUser().getEmail().equals("julio@gmail.com"));
    } else {
      assertTrue(first.getUser().getId().equals("2be3221f-8c2f-4386-8a95-a68f0282efb0"));
      assertTrue(first.getOperation() == OperationType.CREATE);
      assertTrue(first.getUser().getStatus().equals("FORCE_CHANGE_PASSWORD"));
    }
  }

}
