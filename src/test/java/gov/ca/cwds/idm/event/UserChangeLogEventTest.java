package gov.ca.cwds.idm.event;

import static gov.ca.cwds.config.api.idm.Roles.COUNTY_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.CWS_WORKER;
import static gov.ca.cwds.config.api.idm.Roles.OFFICE_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.STATE_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.getRoleNameById;
import static gov.ca.cwds.idm.event.SystemCausedChangeLogEvent.SYSTEM_USER_LOGIN;
import static gov.ca.cwds.idm.event.UserChangeLogEvent.CAP_EVENT_SOURCE;
import static gov.ca.cwds.idm.event.UserEnabledStatusChangedEvent.ACTIVE;
import static gov.ca.cwds.idm.event.UserEnabledStatusChangedEvent.INACTIVE;
import static gov.ca.cwds.idm.event.UserLockedEvent.LOCKED;
import static gov.ca.cwds.idm.event.UserLockedEvent.UNLOCKED;
import static gov.ca.cwds.idm.event.UserRegistrationCompleteEvent.EVENT_TYPE_USER_REGISTRATION_COMPLETE;
import static gov.ca.cwds.idm.event.UserRegistrationCompleteEvent.REGISTERED;
import static gov.ca.cwds.idm.event.UserRegistrationCompleteEvent.UNREGISTERED;
import static gov.ca.cwds.idm.event.WorkerPhoneChangedEvent.EVENT_TYPE_WORKER_PHONE_CHANGED;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCurrentUser;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCurrentUserFirstName;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCurrentUserLastName;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCurrentUserName;
import static gov.ca.cwds.util.Utils.toSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import gov.ca.cwds.UniversalUserToken;
import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserChangeLogRecord;
import gov.ca.cwds.idm.persistence.ns.entity.Permission;
import gov.ca.cwds.idm.service.authorization.UserRolesService;
import gov.ca.cwds.idm.service.diff.BooleanDiff;
import gov.ca.cwds.idm.service.diff.StringDiff;
import gov.ca.cwds.idm.service.diff.StringSetDiff;
import gov.ca.cwds.util.CurrentAuthenticatedUserUtil;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = {"gov.ca.cwds.util.CurrentAuthenticatedUserUtil",
    "gov.ca.cwds.idm.service.authorization.UserRolesService"})
public class UserChangeLogEventTest {

  private static final String TEST_USER_ID = "testId";
  private static final String TEST_FIRST_NAME = "testFirstName";
  private static final String TEST_LAST_NAME = "testLastName";
  private static final String TEST_COUNTY = "testCounty";
  private static final String TEST_OFFICE_ID = "testOfficeId";
  private static final String PERMISSION_1 = "Permission1";
  private static final String PERMISSION_2 = "Permission2";
  private static final String PERMISSION_3 = "Permission3";
  private static final String PERMISSION_4 = "Permission4";
  private static final String PERMISSION_DESCRIPTION = "description";
  private static final String ADMIN_LOGIN = "TEST_USER_NAME";
  private static final String TEST_ADMIN_ROLE = "Test-admin";
  private static final String TEST_ADMIN_LAST_NAME = "Samba";
  private static final String TEST_ADMIN_FIRST_NAME = "Cherno";
  private static final String TEST_ADMIN_NAME_FORMATTED = "Samba, Cherno";
  private static final String NEW_EMAIL = "newEmail@gmail.com";
  private static final String OLD_EMAIL = "oldEmail@gmail.com";
  private static final String NEW_NOTES = "new notes";
  private static final String OLD_NOTES = "old notes";
  private static final String OLD_WORKER_PHONE = "1234567890";
  private static final String OLD_WORKER_PHONE_EXT = "11";

  @Before
  public void before() {
    mockStatic(CurrentAuthenticatedUserUtil.class);
    mockStatic(UserRolesService.class);
  }

  @Test
  public void testSetUpUserChangeLogEvent() {
    User user = mockUser();
    UserCreatedEvent userCreatedEvent = new UserCreatedEvent(user);
    UserChangeLogRecord changeLogRecord = userCreatedEvent.getEvent();
    assertEquals(TEST_ADMIN_ROLE, changeLogRecord.getAdminRole());
    assertEquals(TEST_ADMIN_NAME_FORMATTED, changeLogRecord.getAdminName());
    assertEquals(TEST_COUNTY, changeLogRecord.getCountyName());
    assertEquals(TEST_OFFICE_ID, changeLogRecord.getOfficeId());
    assertEquals(TEST_USER_ID, changeLogRecord.getUserId());
    assertEquals(TEST_LAST_NAME + ", " + TEST_FIRST_NAME, changeLogRecord.getUserName());
    assertEquals(CAP_EVENT_SOURCE, userCreatedEvent.getEventSource());
    assertEquals(ADMIN_LOGIN, userCreatedEvent.getUserLogin());
  }

  @Test
  public void testUserCreatedEvent() {
    User user = mockUser();
    UserCreatedEvent userCreatedEvent = new UserCreatedEvent(user);
    assertEquals(UserCreatedEvent.EVENT_TYPE_USER_CREATED, userCreatedEvent.getEventType());
    assertEquals(String.join(", ", COUNTY_ADMIN, CWS_WORKER),
        userCreatedEvent.getEvent().getNewValue());
    assertEquals(String.join(", ", getRoleNameById(CWS_WORKER), getRoleNameById(COUNTY_ADMIN)),
        userCreatedEvent.getEvent().getUserRoles());
  }

  @Test
  public void testUserRegistrationResentEvent() {
    User user = mockUser();
    UserRegistrationResentEvent event = new UserRegistrationResentEvent(user);
    assertEquals(UserRegistrationResentEvent.EVENT_TYPE_REGISTRATION_RESENT, event.getEventType());
    assertEquals(TEST_LAST_NAME + ", " + TEST_FIRST_NAME, event.getEvent().getUserName());
    assertEquals(String.join(", ", getRoleNameById(CWS_WORKER), getRoleNameById(COUNTY_ADMIN)),
        event.getEvent().getUserRoles());
  }

  @Test
  public void testUserRoleChangedEvent() {
    StringSetDiff diff = new StringSetDiff(
        toSet(COUNTY_ADMIN, CWS_WORKER), toSet(OFFICE_ADMIN, STATE_ADMIN));

    UserRoleChangedEvent userRoleChangedEvent = new UserRoleChangedEvent(mockUser(), diff);
    assertEquals(UserRoleChangedEvent.EVENT_TYPE_USER_ROLE_CHANGED,
        userRoleChangedEvent.getEventType());
    assertEquals("CWS Worker, County Administrator",
        userRoleChangedEvent.getEvent().getOldValue());
    assertEquals("Office Administrator, State Administrator",
        userRoleChangedEvent.getEvent().getNewValue());
    assertEquals("Office Administrator, State Administrator",
        userRoleChangedEvent.getEvent().getUserRoles());
  }

  @Test
  public void testPermissionsChangedEvent() {

    StringSetDiff diff = new StringSetDiff(toSet(PERMISSION_1, PERMISSION_2),
        toSet(PERMISSION_3, PERMISSION_4));

    List<Permission> permissions = Stream.of(
        new Permission(PERMISSION_1, PERMISSION_1 + PERMISSION_DESCRIPTION),
        new Permission(PERMISSION_2, PERMISSION_2 + PERMISSION_DESCRIPTION),
        new Permission(PERMISSION_3, PERMISSION_3 + PERMISSION_DESCRIPTION),
        new Permission(PERMISSION_4, PERMISSION_4 + PERMISSION_DESCRIPTION)
    ).collect(Collectors.toList());

    PermissionsChangedEvent event = new PermissionsChangedEvent(mockUser(), diff, permissions);
    assertEquals(PermissionsChangedEvent.EVENT_TYPE_PERMISSIONS_CHANGED,
        event.getEventType());
    assertEquals(StringUtils.join(
        new String[]{
            PERMISSION_1 + PERMISSION_DESCRIPTION,
            PERMISSION_2 + PERMISSION_DESCRIPTION}, ", "),
        event.getEvent().getOldValue());
    assertEquals(StringUtils.join(
        new String[]{
            PERMISSION_3 + PERMISSION_DESCRIPTION,
            PERMISSION_4 + PERMISSION_DESCRIPTION}, ", "), event.getEvent().getNewValue());
    assertEquals(String.join(", ", getRoleNameById(CWS_WORKER), getRoleNameById(COUNTY_ADMIN)),
        event.getEvent().getUserRoles());
  }

  @Test
  public void testEmailChangedEvent() {

    StringDiff diff = new StringDiff(OLD_EMAIL, NEW_EMAIL);

    EmailChangedEvent event = new EmailChangedEvent(mockUser(), diff);
    assertEquals(EmailChangedEvent.EVENT_TYPE_EMAIL_CHANGED,
        event.getEventType());
    assertEquals(OLD_EMAIL, event.getEvent().getOldValue());
    assertEquals(NEW_EMAIL, event.getEvent().getNewValue());
    assertEquals(String.join(", ", getRoleNameById(CWS_WORKER), getRoleNameById(COUNTY_ADMIN)),
        event.getEvent().getUserRoles());
  }

  @Test
  public void testNotesChangedEvent() {
    StringDiff diff = new StringDiff(OLD_NOTES, NEW_NOTES);

    NotesChangedEvent event = new NotesChangedEvent(mockUser(), diff);
    assertEquals(NotesChangedEvent.EVENT_TYPE_NOTES_CHANGED, event.getEventType());
    assertEquals(OLD_NOTES, event.getEvent().getOldValue());
    assertEquals(NEW_NOTES, event.getEvent().getNewValue());
    assertEquals(String.join(", ", getRoleNameById(CWS_WORKER), getRoleNameById(COUNTY_ADMIN)),
        event.getEvent().getUserRoles());
  }

  @Test
  public void testUserEnabledStatusChangedEvent() {
    BooleanDiff diff = new BooleanDiff(Boolean.FALSE, Boolean.TRUE);

    UserEnabledStatusChangedEvent event = new UserEnabledStatusChangedEvent(mockUser(), diff);
    assertEquals(UserEnabledStatusChangedEvent.USER_ACCOUNT_STATUS_CHANGED,
        event.getEventType());
    assertEquals(INACTIVE, event.getEvent().getOldValue());
    assertEquals(ACTIVE, event.getEvent().getNewValue());
    assertEquals(String.join(", ", getRoleNameById(CWS_WORKER), getRoleNameById(COUNTY_ADMIN)),
        event.getEvent().getUserRoles());

  }

  @Test
  public void testUserLockedEvent() {
    UserLockedEvent event = new UserLockedEvent(mockUser());
    assertEquals(UserLockedEvent.EVENT_TYPE_USER_LOCKED,
        event.getEventType());
    assertEquals(UNLOCKED, event.getEvent().getOldValue());
    assertEquals(LOCKED, event.getEvent().getNewValue());
    assertEquals(SYSTEM_USER_LOGIN, event.getUserLogin());
    assertEquals(SYSTEM_USER_LOGIN, event.getEvent().getAdminName());
  }

  @Test
  public void testUserUnlockedEvent() {
    UserUnlockedEvent event = new UserUnlockedEvent(mockUser());
    assertEquals(UserUnlockedEvent.EVENT_TYPE_USER_UNLOCKED, event.getEventType());
    assertEquals(LOCKED, event.getEvent().getOldValue());
    assertEquals(UNLOCKED, event.getEvent().getNewValue());
  }

  @Test
  public void testUserPasswordChangedEvent() {
    UserPasswordChangedEvent event = new UserPasswordChangedEvent(mockUser());

    assertThat(event.getUserLogin(), is(TEST_USER_ID));
    assertThat(event.getEvent().getUserId(), is(TEST_USER_ID));
    assertThat(event.getEventType(), is("User Password Changed"));
    assertThat(event.getEvent().getAdminName(), is("testLastName, testFirstName"));
    assertThat(event.getEvent().getUserName(), is("testLastName, testFirstName"));
    assertThat(event.getEvent().getAdminRole(), is("CWS Worker, County Administrator"));
    assertThat(event.getEvent().getUserRoles(), is("CWS Worker, County Administrator"));
    assertThat(event.getEvent().getOldValue(), is(nullValue()));
    assertThat(event.getEvent().getNewValue(), is(nullValue()));
    assertThat(event.getEvent().getCountyName(), is(TEST_COUNTY));
    assertThat(event.getEvent().getOfficeId(), is(TEST_OFFICE_ID));
  }

  @Test
  public void testUserRegistrationCompleteEvent() {
    UserRegistrationCompleteEvent event = new UserRegistrationCompleteEvent(mockUser());

    assertThat(event.getEvent().getUserId(), is(TEST_USER_ID));
    assertThat(event.getEventType(), is(EVENT_TYPE_USER_REGISTRATION_COMPLETE));
    assertNull(event.getEvent().getAdminName());
    assertThat(event.getEvent().getUserName(), is("testLastName, testFirstName"));
    assertNull(event.getEvent().getAdminRole());
    assertThat(event.getEvent().getUserRoles(), is("CWS Worker, County Administrator"));
    assertEquals(UNREGISTERED, event.getEvent().getOldValue());
    assertEquals(REGISTERED, event.getEvent().getNewValue());
    assertThat(event.getEvent().getCountyName(), is(TEST_COUNTY));
    assertThat(event.getEvent().getOfficeId(), is(TEST_OFFICE_ID));
  }

  @Test
  public void testCellPhoneChangedEvent() {
    final String OLD_CELL_PHONE = "1234567890";
    final String NEW_CELL_PHONE = "9876543210";

    CellPhoneChangedEvent event =
        new CellPhoneChangedEvent(mockUser(), new StringDiff(OLD_CELL_PHONE, NEW_CELL_PHONE));
    Assert.assertThat(event.getEventType(), is(CellPhoneChangedEvent.EVENT_TYPE_CELL_PHONE_CHANGED));
    Assert.assertThat(event.getEvent().getOldValue(), is("(123) 456-7890"));
    Assert.assertThat(event.getEvent().getNewValue(), is("(987) 654-3210"));
    Assert.assertThat(event.getEvent().getUserRoles(), is("CWS Worker, County Administrator"));

    event = new CellPhoneChangedEvent(mockUser(), new StringDiff(null, NEW_CELL_PHONE));
    Assert.assertThat(event.getEventType(), is(CellPhoneChangedEvent.EVENT_TYPE_CELL_PHONE_CHANGED));
    Assert.assertThat(event.getEvent().getOldValue(), is(nullValue()));
    Assert.assertThat(event.getEvent().getNewValue(), is("(987) 654-3210"));
    Assert.assertThat(event.getEvent().getUserRoles(), is("CWS Worker, County Administrator"));

    event = new CellPhoneChangedEvent(mockUser(), new StringDiff(OLD_CELL_PHONE, null));
    Assert.assertThat(event.getEventType(), is(CellPhoneChangedEvent.EVENT_TYPE_CELL_PHONE_CHANGED));
    Assert.assertThat(event.getEvent().getOldValue(), is("(123) 456-7890"));
    Assert.assertThat(event.getEvent().getNewValue(), is(nullValue()));
    Assert.assertThat(event.getEvent().getUserRoles(), is("CWS Worker, County Administrator"));
  }

  @Test
  public void testWorkerPhoneChangedEvent() {

    assertWorkerPhoneChangeEvent(
        "1234567890", "9876543210",
        "11", "22",
        "(123) 456-7890 Ext 11", "(987) 654-3210 Ext 22");

    assertWorkerPhoneChangeEvent(
        "1234567890", "9876543210",
        null, "22",
        "(123) 456-7890", "(987) 654-3210 Ext 22");

    assertWorkerPhoneChangeEvent(
        "1234567890", "9876543210",
        "11", null,
        "(123) 456-7890 Ext 11", "(987) 654-3210");

    assertWorkerPhoneChangeEvent(
        "1234567890", "9876543210",
        null, null,
        "(123) 456-7890", "(987) 654-3210");

    assertWorkerPhoneChangeEvent(
        "1234567890", "1234567890",
        "11", "22",
        "(123) 456-7890 Ext 11", "(123) 456-7890 Ext 22");

    assertWorkerPhoneChangeEvent(
        "1234567890", "1234567890",
        null, "22",
        "(123) 456-7890", "(123) 456-7890 Ext 22");

    assertWorkerPhoneChangeEvent(
        "1234567890", "1234567890",
        "11", null,
        "(123) 456-7890 Ext 11", "(123) 456-7890");

    assertWorkerPhoneChangeEvent(
        null, "1234567890",
        "11", "22",
        "Ext 11", "(123) 456-7890 Ext 22");

    assertWorkerPhoneChangeEvent(
        null, "1234567890",
        "11", null,
        "Ext 11", "(123) 456-7890");

    assertWorkerPhoneChangeEvent(
        null, "1234567890",
        null, "22",
        "", "(123) 456-7890 Ext 22");
  }

  private void assertWorkerPhoneChangeEvent(
      String oldPhone, String newPhone,
      String oldExt, String newExt,
      String oldValue, String newValue) {

    User existedUser = mockUser();
    existedUser.setPhoneNumber(oldPhone);
    existedUser.setPhoneExtensionNumber(oldExt);

    WorkerPhoneChangedEvent event =
        new WorkerPhoneChangedEvent(
            existedUser,
            getOptionalStringDiff(oldPhone, newPhone),
            getOptionalStringDiff(oldExt, newExt));

    Assert.assertThat(event.getEventType(), is(EVENT_TYPE_WORKER_PHONE_CHANGED));
    Assert.assertThat(event.getEvent().getOldValue(), is(oldValue));
    Assert.assertThat(event.getEvent().getNewValue(), is(newValue));
    Assert.assertThat(event.getEvent().getUserRoles(), is("CWS Worker, County Administrator"));
  }

  private Optional<StringDiff> getOptionalStringDiff(String oldValue, String newValue) {
    if(Objects.equals(oldValue, newValue)) {
      return Optional.empty();
    } else {
      return Optional.of(new StringDiff(oldValue, newValue));
    }
  }

  private User mockUser() {
    User user = new User();
    user.setId(TEST_USER_ID);
    user.setFirstName(TEST_FIRST_NAME);
    user.setLastName(TEST_LAST_NAME);
    user.setCountyName(TEST_COUNTY);
    user.setOfficeId(TEST_OFFICE_ID);
    user.setEmail(OLD_EMAIL);
    user.setPhoneNumber(OLD_WORKER_PHONE);
    user.setPhoneExtensionNumber(OLD_WORKER_PHONE_EXT);
    user.setRoles(new HashSet<>(Arrays.asList(CWS_WORKER, COUNTY_ADMIN)));
    user.setPermissions(new TreeSet<>(Arrays.asList(PERMISSION_1, PERMISSION_2)));
    when(getCurrentUserName()).thenReturn(ADMIN_LOGIN);
    when(getCurrentUser()).thenReturn(new UniversalUserToken());
    when(UserRolesService.getStrongestAdminRole(any(UniversalUserToken.class)))
        .thenReturn(TEST_ADMIN_ROLE);
    when(getCurrentUserLastName()).thenReturn(TEST_ADMIN_LAST_NAME);
    when(getCurrentUserFirstName()).thenReturn(TEST_ADMIN_FIRST_NAME);
    return user;
  }
}
