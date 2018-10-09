package gov.ca.cwds.idm.service.authorization;

import static gov.ca.cwds.config.api.idm.Roles.COUNTY_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.CWS_WORKER;
import static gov.ca.cwds.config.api.idm.Roles.OFFICE_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.STATE_ADMIN;
import static gov.ca.cwds.idm.service.authorization.AuthorizationTestHelper.admin;
import static gov.ca.cwds.idm.service.authorization.AuthorizationTestHelper.user;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCurrentUser;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCurrentUserCountyName;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCurrentUserOfficeIds;
import static gov.ca.cwds.util.Utils.toSet;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import gov.ca.cwds.util.CurrentAuthenticatedUserUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "gov.ca.cwds.util.CurrentAuthenticatedUserUtil")
public class AuthorizationServiceImplTest {

  private AuthorizationServiceImpl service;

  @Before
  public void before() {
    service = new AuthorizationServiceImpl();
    mockStatic(CurrentAuthenticatedUserUtil.class);
  }

  @Test
  public void testAdminCantUpdateHimself() {
    String adminId = "someId";
    when(CurrentAuthenticatedUserUtil.getCurrentUserName()).thenReturn(adminId);
    assertFalse(service.canUpdateUser(adminId, null));
  }

  @Test
  public void testByUserAndAdmin_StateAdminSameCounty() {
    when(getCurrentUser()).thenReturn(
        admin(toSet(STATE_ADMIN, OFFICE_ADMIN), "Yolo", toSet("Yolo_2")));
    assertTrue(service.canViewUser(user("Yolo", "Yolo_1")));
  }

  @Test
  public void testByUserAndAdmin_StateAdminDifferentCounty() {
    when(getCurrentUser())
        .thenReturn(admin(toSet(STATE_ADMIN), "Yolo", null));
    assertTrue(service.canViewUser(user("Madera", "Madera_1")));
  }

  @Test
  public void testByUserAndAdmin_StateAdminNoCounty() {
    when(getCurrentUser())
        .thenReturn(admin(toSet(STATE_ADMIN), null, null));
    assertTrue(service.canViewUser(user("Madera", "Madera_1")));
  }

  @Test
  public void testByUserAndAdmin_CountyAdminSameCounty() {
    when(getCurrentUser())
        .thenReturn(admin(toSet(COUNTY_ADMIN, OFFICE_ADMIN),
            "Yolo", toSet("Yolo_2")));
    when(getCurrentUserCountyName()).thenReturn("Yolo");
    assertTrue(service.canViewUser(user("Yolo", "Yolo_1")));
  }

  @Test
  public void testByUserAndAdmin_CountyAdminSameCountyNoOffice() {
    when(getCurrentUser())
        .thenReturn(admin(toSet(COUNTY_ADMIN), "Yolo", null));
    when(getCurrentUserCountyName())
        .thenReturn("Yolo");
    assertTrue(service.canViewUser(user("Yolo", "Yolo_1")));
  }

  @Test
  public void testByUserAndAdmin_CountyAdminDifferentCounty() {
    when(getCurrentUser())
        .thenReturn(admin(toSet(COUNTY_ADMIN), "Madera", null));
    assertFalse(service.canViewUser(user("Yolo", "Yolo_1")));
  }

  @Test
  public void testByUserAndAdmin_OfficeAdminSameOffice() {
    when(getCurrentUser())
        .thenReturn(admin(toSet(OFFICE_ADMIN), "Yolo", toSet("Yolo_1")));
    when(getCurrentUserCountyName()).thenReturn("Yolo");
    when(getCurrentUserOfficeIds()).thenReturn(toSet("Yolo_1"));
    assertTrue(service.canViewUser(user("Yolo", "Yolo_1")));
  }

  @Test
  public void testByUserAndAdmin_OfficeAdminDifferentOffice() {
    when(getCurrentUser())
        .thenReturn(admin(toSet(OFFICE_ADMIN), "Yolo", toSet("Yolo_2")));
    assertFalse(
        service.canCreateUser(user("Yolo", "Yolo_1")));
  }

  @Test
  public void testByUserAndAdmin_OfficeAdmin_UserNoOffice() {
    when(getCurrentUser())
        .thenReturn(admin(toSet(OFFICE_ADMIN), "Yolo", toSet("Yolo_2")));
    assertFalse(
        service.canCreateUser(user("Yolo", null)));
  }

  @Test
  public void testCanViewUser_OfficeAdmin() {
    when(getCurrentUser())
        .thenReturn(admin(toSet(OFFICE_ADMIN), "Yolo", toSet("Yolo_1", "Yolo_2")));
    when(getCurrentUserOfficeIds()).thenReturn(toSet("Yolo_1", "Yolo_2"));
    when(getCurrentUserCountyName()).thenReturn("Yolo");

    assertTrue(service.canViewUser(user(toSet(CWS_WORKER), "Yolo", "Yolo_1")));
    assertTrue(service.canViewUser(
        user(toSet(CWS_WORKER), "Yolo", "Yolo_3")));
    assertTrue(service.canViewUser(
        user(toSet(STATE_ADMIN), "Yolo", "Yolo_1")));
    assertTrue(service.canViewUser(
        user(toSet(COUNTY_ADMIN), "Yolo", "Yolo_1")));
    assertFalse(service.canViewUser(
        user(toSet(STATE_ADMIN), "Yolo", "Yolo_3")));
    assertFalse(service.canViewUser(
        user(toSet(COUNTY_ADMIN), "Yolo", "Yolo_3")));
  }

}
