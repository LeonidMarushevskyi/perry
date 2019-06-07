package gov.ca.cwds.idm.dto.serializer;

import static gov.ca.cwds.idm.util.TestUtils.asJsonString;
import static gov.ca.cwds.util.Utils.toSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import gov.ca.cwds.idm.dto.UserUpdate;
import org.junit.Test;

public class UserUpdateJsonSerializeTest {

  @Test
  public void testEmpty() {
    assertThat(asJsonString(new UserUpdate()), is("{}"));
  }

  @Test
  public void testNullStringProperty() {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setCellPhoneNumber(null);
    assertThat(asJsonString(userUpdate), is("{\"cell_phone_number\":null}"));
  }

  @Test
  public void testNotNullStringProperty() {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setCellPhoneNumber("1234567890");
    assertThat(asJsonString(userUpdate), is("{\"cell_phone_number\":\"1234567890\"}"));
  }

  @Test
  public void testNullBooleanProperty() {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(null);
    assertThat(asJsonString(userUpdate), is("{\"enabled\":null}"));
  }

  @Test
  public void testNotNullBooleanProperty() {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(false);
    assertThat(asJsonString(userUpdate), is("{\"enabled\":false}"));
  }

  @Test
  public void testNullStringSetProperty() {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setRoles(null);
    assertThat(asJsonString(userUpdate), is("{\"roles\":null}"));
  }

  @Test
  public void testNotNullStringSetProperty() {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setRoles(toSet("one", "two"));
    assertThat(asJsonString(userUpdate), is("{\"roles\":[\"one\",\"two\"]}"));
  }
}
