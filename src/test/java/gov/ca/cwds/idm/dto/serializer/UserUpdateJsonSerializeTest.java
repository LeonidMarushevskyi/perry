package gov.ca.cwds.idm.dto.serializer;

import static gov.ca.cwds.idm.util.TestUtils.asJsonString;

import gov.ca.cwds.idm.dto.UserUpdate;
import org.junit.Test;

public class UserUpdateJsonSerializeTest {
  @Test
  public void testSerialize() {
    UserUpdate userUpdate = new UserUpdate();
    System.out.println(asJsonString(userUpdate));
  }

}
