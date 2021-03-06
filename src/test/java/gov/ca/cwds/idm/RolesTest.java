package gov.ca.cwds.idm;

import static gov.ca.cwds.config.api.idm.Roles.OFFICE_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.STATE_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.SUPER_ADMIN;
import static gov.ca.cwds.idm.util.AssertFixtureUtils.assertStrict;
import static org.junit.Assert.assertEquals;

import gov.ca.cwds.config.api.idm.Roles;
import gov.ca.cwds.idm.util.WithMockCustomUser;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

public class RolesTest extends BaseIdmIntegrationTest {

  @Test
  @WithMockCustomUser
  public void testGetRoles() throws Exception {
    assertGetRolesSuccess();
  }

  @Test
  @WithMockCustomUser(roles = {"OtherRole"})
  public void testGetRolesWithOtherRole() throws Exception {
    assertGetRolesUnauthorized();
  }

  @Test
  @WithMockCustomUser(roles = {SUPER_ADMIN})
  public void testGetRolesSuperAdmin() throws Exception {
    assertGetRolesSuccess();
  }

  @Test
  @WithMockCustomUser(roles = {STATE_ADMIN})
  public void testGetRolesStateAdmin() throws Exception {
    assertGetRolesSuccess();
  }

  @Test
  @WithMockCustomUser(roles = {OFFICE_ADMIN})
  public void testGetRolesOfficeAdmin() throws Exception {
    assertGetRolesSuccess();
  }

  @Test
  public void getRoleNameByIdTest() {
    assertEquals("Office Administrator", Roles.getRoleNameById(Roles.OFFICE_ADMIN));
  }

  private void assertGetRolesSuccess() throws Exception {
    MvcResult result =
        mockMvc
            .perform(MockMvcRequestBuilders.get("/idm/roles"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(JSON_CONTENT_TYPE))
            .andReturn();

    assertStrict(result, "fixtures/idm/roles/valid.json");
  }

  private void assertGetRolesUnauthorized() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/idm/roles"))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andReturn();
  }
}
