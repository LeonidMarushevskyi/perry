package gov.ca.cwds.idm;

import static gov.ca.cwds.config.TokenServiceConfiguration.TOKEN_TRANSACTION_MANAGER;
import static gov.ca.cwds.config.api.idm.Roles.COUNTY_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.CWS_WORKER;
import static gov.ca.cwds.config.api.idm.Roles.OFFICE_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.STATE_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.SUPER_ADMIN;
import static gov.ca.cwds.idm.service.cognito.attribute.StandardUserAttribute.EMAIL;
import static gov.ca.cwds.idm.service.cognito.attribute.StandardUserAttribute.EMAIL_VERIFIED;
import static gov.ca.cwds.idm.service.cognito.attribute.StandardUserAttribute.PHONE_NUMBER;
import static gov.ca.cwds.idm.util.AssertFixtureUtils.assertExtensible;
import static gov.ca.cwds.idm.util.TestCognitoServiceFacade.COUNTY_ADMIN_ID;
import static gov.ca.cwds.idm.util.TestCognitoServiceFacade.INACTIVE_USER_WITH_ACTIVE_RACFID_IN_CMS;
import static gov.ca.cwds.idm.util.TestCognitoServiceFacade.INACTIVE_USER_WITH_NO_ACTIVE_RACFID_IN_CMS;
import static gov.ca.cwds.idm.util.TestCognitoServiceFacade.INACTIVE_USER_WITH_NO_RACFID;
import static gov.ca.cwds.idm.util.TestCognitoServiceFacade.NO_EDIT_USER_ID;
import static gov.ca.cwds.idm.util.TestCognitoServiceFacade.NULL_CELL_PHONE_USER_ID;
import static gov.ca.cwds.idm.util.TestCognitoServiceFacade.OFFICE_ADMIN_ID;
import static gov.ca.cwds.idm.util.TestCognitoServiceFacade.STATE_ADMIN_ID;
import static gov.ca.cwds.idm.util.TestCognitoServiceFacade.SUPER_ADMIN_ID;
import static gov.ca.cwds.idm.util.TestCognitoServiceFacade.USERPOOL;
import static gov.ca.cwds.idm.util.TestCognitoServiceFacade.USER_NO_RACFID_ID;
import static gov.ca.cwds.idm.util.TestCognitoServiceFacade.USER_WITH_RACFID_AND_DB_DATA_ID;
import static gov.ca.cwds.idm.util.TestCognitoServiceFacade.USER_WITH_RACFID_ID;
import static gov.ca.cwds.idm.util.TestUtils.asJsonString;
import static gov.ca.cwds.idm.util.TestUtils.attr;
import static gov.ca.cwds.idm.util.WithMockCustomUserSecurityContextFactory.ADMIN_ID;
import static gov.ca.cwds.util.Utils.toSet;
import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

import ch.qos.logback.classic.spi.LoggingEvent;
import com.amazonaws.services.cognitoidp.model.AdminDisableUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminDisableUserResult;
import com.amazonaws.services.cognitoidp.model.AdminEnableUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminEnableUserResult;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesResult;
import com.amazonaws.services.cognitoidp.model.AliasExistsException;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.google.common.collect.Iterables;
import gov.ca.cwds.config.LoggingRequestIdFilter;
import gov.ca.cwds.config.LoggingUserIdFilter;
import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserUpdate;
import gov.ca.cwds.idm.event.AuditEvent;
import gov.ca.cwds.idm.event.CellPhoneChangedEvent;
import gov.ca.cwds.idm.event.EmailChangedEvent;
import gov.ca.cwds.idm.event.NotesChangedEvent;
import gov.ca.cwds.idm.event.PermissionsChangedEvent;
import gov.ca.cwds.idm.event.UserEnabledStatusChangedEvent;
import gov.ca.cwds.idm.event.UserRoleChangedEvent;
import gov.ca.cwds.idm.event.WorkerPhoneChangedEvent;
import gov.ca.cwds.idm.persistence.ns.OperationType;
import gov.ca.cwds.idm.persistence.ns.entity.NsUser;
import gov.ca.cwds.idm.persistence.ns.entity.UserLog;
import gov.ca.cwds.idm.util.WithMockCustomUser;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

@Transactional(value = TOKEN_TRANSACTION_MANAGER)
public class UpdateUserTest extends BaseIdmIntegrationWithSearchTest {

  @Test
  @WithMockCustomUser(roles = {STATE_ADMIN})
  public void testUpdateUser() throws Exception {

    final String NEW_EMAIL = "error.mail@mail.com";
    final String NEW_PHONE = "6889228010";
    final String NEW_PHONE_EXTENSION = "123";
    final String NEW_CELL_PHONE = "6668889999";
    final String NEW_NOTES = "New notes text";

    NsUser existedNsUser = assertNsUserInDb(USER_NO_RACFID_ID);
    LocalDateTime oldLastModifiedTime = existedNsUser.getLastModifiedTime();

    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.FALSE);
    userUpdate.setEmail(NEW_EMAIL);
    userUpdate.setPhoneNumber(NEW_PHONE);
    userUpdate.setPhoneExtensionNumber(NEW_PHONE_EXTENSION);
    userUpdate.setCellPhoneNumber(NEW_CELL_PHONE);
    userUpdate.setPermissions(toSet("RFA-rollout", "Hotline-rollout"));
    userUpdate.setRoles(toSet(OFFICE_ADMIN, CWS_WORKER));
    userUpdate.setNotes(NEW_NOTES);

    AdminUpdateUserAttributesRequest updateAttributesRequest =
        setUpdateUserAttributesRequestAndResult(
            USER_NO_RACFID_ID,
            attr(EMAIL, NEW_EMAIL),
            attr(EMAIL_VERIFIED, "True"),
            attr(PHONE_NUMBER, "+" + NEW_CELL_PHONE)
        );

    AdminDisableUserRequest disableUserRequest = setDisableUserRequestAndResult(USER_NO_RACFID_ID);

    setDoraSuccess();

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/idm/users/" + USER_NO_RACFID_ID)
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(userUpdate)))
        .andExpect(MockMvcResultMatchers.status().isNoContent())
        .andReturn();

    verify(cognito, times(1)).adminDisableUser(disableUserRequest);
    verify(spyUserIndexService, times(1)).updateUserInIndex(any(User.class));

    InOrder inOrder = inOrder(cognito);
    inOrder.verify(cognito).adminUpdateUserAttributes(updateAttributesRequest);
    inOrder.verify(cognito).adminDisableUser(disableUserRequest);

    NsUser updatedNsUser =  assertNsUserInDb(USER_NO_RACFID_ID);
    assertThat(updatedNsUser.getUsername(), is(USER_NO_RACFID_ID));
    assertThat(updatedNsUser.getPhoneNumber(), is(NEW_PHONE));
    assertThat(updatedNsUser.getPhoneExtensionNumber(), is(NEW_PHONE_EXTENSION));
    assertThat(updatedNsUser.getRoles(), is(toSet(OFFICE_ADMIN, CWS_WORKER)));
    assertThat(updatedNsUser.getPermissions(), is(toSet("RFA-rollout", "Hotline-rollout")));
    assertThat(updatedNsUser.getNotes(), is(NEW_NOTES));

    LocalDateTime newLastModifiedTime = updatedNsUser.getLastModifiedTime();
    assertThat(newLastModifiedTime, is(notNullValue()));
    assertThat(newLastModifiedTime, is(not(equalTo(oldLastModifiedTime))));

    verifyDoraCalls(1);
    ArgumentCaptor<List<? extends AuditEvent>> captor = ArgumentCaptor.forClass(List.class);
    verify(auditEventService, times(1)).saveAuditEvents(captor.capture());
    List<? extends AuditEvent> events = captor.getValue();
    assertEquals(6, events.size());
    assertTrue(events.stream().anyMatch(e -> e instanceof UserRoleChangedEvent));
    assertTrue(events.stream().anyMatch(e -> e instanceof PermissionsChangedEvent));
    assertTrue(events.stream().anyMatch(e -> e instanceof EmailChangedEvent));
    assertTrue(events.stream().anyMatch(e -> e instanceof NotesChangedEvent));
    assertTrue(events.stream().anyMatch(e -> e instanceof CellPhoneChangedEvent));
    assertTrue(events.stream().anyMatch(e -> e instanceof WorkerPhoneChangedEvent));
    verify(auditEventService, times(1))
        .saveAuditEvent(any(UserEnabledStatusChangedEvent.class));
  }

  @Test
  @WithMockCustomUser(roles = {STATE_ADMIN})
  public void testEmptyJsonIsTreatedAsNoUpdate() throws Exception {

    final String USER_ID = NO_EDIT_USER_ID;
    String updateJson = fixture("fixtures/idm/update-user/empty-update.json");

    NsUser existedNsUser = assertNsUserInDb(USER_ID);
    LocalDateTime oldLastModifiedTime = existedNsUser.getLastModifiedTime();
    setDoraSuccess();

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/idm/users/" + USER_ID)
                .contentType(JSON_CONTENT_TYPE)
                .content(updateJson))
        .andExpect(MockMvcResultMatchers.status().isNoContent())
        .andReturn();

    verify(cognito, times(0))
        .adminUpdateUserAttributes(argThat(r -> r.getUsername().equals(NO_EDIT_USER_ID)));

    verify(spyUserIndexService, times(0)).updateUserInIndex(any(User.class));
    verifyDoraCalls(0);

    NsUser updatedNsUser =  assertNsUserInDb(USER_ID);
    LocalDateTime newLastModifiedTime = updatedNsUser.getLastModifiedTime();
    assertThat(newLastModifiedTime, is(equalTo(oldLastModifiedTime)));

    verify(auditEventService, times(0)).saveAuditEvents(any(List.class));
  }

  @Test
  @WithMockCustomUser(roles = {STATE_ADMIN})
  public void testJsonNullIsTreatedAsDelete() throws Exception {

    final String USER_ID = NULL_CELL_PHONE_USER_ID;
    String updateJson = fixture("fixtures/idm/update-user/cell-phone-null.json");

    NsUser existedNsUser = assertNsUserInDb(USER_ID);
    LocalDateTime oldLastModifiedTime = existedNsUser.getLastModifiedTime();
    setDoraSuccess();

    AdminUpdateUserAttributesRequest updateAttributesRequest =
        setUpdateUserAttributesRequestAndResult(
            USER_ID,
            attr(PHONE_NUMBER, "")
        );

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/idm/users/" + USER_ID)
                .contentType(JSON_CONTENT_TYPE)
                .content(updateJson))
        .andExpect(MockMvcResultMatchers.status().isNoContent())
        .andReturn();

    verify(cognito, times(1))
        .adminUpdateUserAttributes(updateAttributesRequest);

    verify(spyUserIndexService, times(1)).updateUserInIndex(any(User.class));
    verifyDoraCalls(1);

    ArgumentCaptor<List<? extends AuditEvent>> captor = ArgumentCaptor.forClass(List.class);
    verify(auditEventService, times(1)).saveAuditEvents(captor.capture());
    List<? extends AuditEvent> events = captor.getValue();
    assertEquals(1, events.size());
    assertTrue(events.stream().anyMatch(e -> e instanceof CellPhoneChangedEvent));
  }

  @Test
  @Transactional(value = TOKEN_TRANSACTION_MANAGER, propagation = NOT_SUPPORTED)//to turn off class level @Transactional
  @WithMockCustomUser(roles = {STATE_ADMIN})
  public void testUpdateCognitoAttributesFail() throws Exception {

    final String NEW_EMAIL = "newmail@mail.com";
    final String OLD_NOTES = "Some notes text";
    final String NEW_NOTES = "New notes text";
    assertThat(NEW_NOTES, not(OLD_NOTES));

    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEmail(NEW_EMAIL);
    userUpdate.setNotes(NEW_NOTES);

    NsUser existedNsUser = assertNsUserInDb(USER_NO_RACFID_ID);
    assertThat(existedNsUser.getNotes(), is(OLD_NOTES));

    AdminUpdateUserAttributesRequest cognitoUpdateAttributesRequest =
        new AdminUpdateUserAttributesRequest()
            .withUsername(USER_NO_RACFID_ID)
            .withUserPoolId(USERPOOL)
            .withUserAttributes(attr(EMAIL, NEW_EMAIL), attr(EMAIL_VERIFIED, "True"));

    when(cognito.adminUpdateUserAttributes(cognitoUpdateAttributesRequest))
        .thenThrow(new RuntimeException("Cognito update attributes error"));

    MvcResult result = mockMvc.perform(
        MockMvcRequestBuilders.patch("/idm/users/" + USER_NO_RACFID_ID)
            .contentType(JSON_CONTENT_TYPE)
            .content(asJsonString(userUpdate)))
        .andExpect(MockMvcResultMatchers.status().isInternalServerError())
        .andReturn();

    assertExtensible(result, "fixtures/idm/update-user/cognito-update-attrs-fail.json");

    verify(cognito, times(1)).adminUpdateUserAttributes(cognitoUpdateAttributesRequest);

    NsUser updatedNsUser =  assertNsUserInDb(USER_NO_RACFID_ID);
    assertThat(updatedNsUser.getNotes(), is(OLD_NOTES));

    verify(spyUserIndexService, times(0)).updateUserInIndex(any(User.class));
    verifyDoraCalls(0);

    ArgumentCaptor<List<? extends AuditEvent>> captor = ArgumentCaptor.forClass(List.class);
    verify(auditEventService, times(0)).saveAuditEvents(captor.capture());
  }

  @Test
  @Transactional(value = TOKEN_TRANSACTION_MANAGER, propagation = NOT_SUPPORTED)//to turn off class level @Transactional
  @WithMockCustomUser(roles = {STATE_ADMIN})
  public void testUpdateNsDatabaseFail() throws Exception {

    final String NEW_EMAIL = "newmail@mail.com";
    final String OLD_NOTES = null;
    final String NEW_NOTES = "New notes text";
    assertThat(NEW_NOTES, not(OLD_NOTES));

    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEmail(NEW_EMAIL);
    userUpdate.setNotes(NEW_NOTES);

    final String USER_ID = USER_WITH_RACFID_ID;

    NsUser existedNsUser = assertNsUserInDb(USER_ID);
    assertThat(existedNsUser.getNotes(), is(OLD_NOTES));

    AdminUpdateUserAttributesRequest cognitoUpdateAttributesRequest =
        new AdminUpdateUserAttributesRequest()
            .withUsername(USER_ID)
            .withUserPoolId(USERPOOL)
            .withUserAttributes(attr(EMAIL, NEW_EMAIL), attr(EMAIL_VERIFIED, "True"));

    EntityManager entityManager = transactionalUserService.getEntityManager();
    EntityManager spyEntityManager = spy(transactionalUserService.getEntityManager());
    transactionalUserService.setEntityManager(spyEntityManager);
    doThrow(new RuntimeException("DB error")).when(spyEntityManager).flush();

    MvcResult result;
    try {
      result = mockMvc.perform(
          MockMvcRequestBuilders.patch("/idm/users/" + USER_ID)
              .contentType(JSON_CONTENT_TYPE)
              .content(asJsonString(userUpdate)))
          .andExpect(MockMvcResultMatchers.status().isInternalServerError())
          .andReturn();
      assertExtensible(result, "fixtures/idm/update-user/ns-db-update-fail.json");
    } finally {
      transactionalUserService.setEntityManager(entityManager);
    }

    verify(cognito, times(0)).adminUpdateUserAttributes(cognitoUpdateAttributesRequest);

    NsUser updatedNsUser =  assertNsUserInDb(USER_ID);
    assertThat(updatedNsUser.getNotes(), is(OLD_NOTES));

    verify(spyUserIndexService, times(0)).updateUserInIndex(any(User.class));
    verifyDoraCalls(0);

    ArgumentCaptor<List<? extends AuditEvent>> captor = ArgumentCaptor.forClass(List.class);
    verify(auditEventService, times(0)).saveAuditEvents(captor.capture());
  }

  @Test
  @WithMockCustomUser
  public void testUpdateUserNotAllowedRole() throws Exception {

    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.FALSE);
    userUpdate.setRoles(toSet("State-admin"));

    assertUpdateBadRequest(USER_WITH_RACFID_AND_DB_DATA_ID, userUpdate,
        "fixtures/idm/update-user/not-allowed-role-error.json");
  }

  @Test
  @WithMockCustomUser(roles = {STATE_ADMIN})
  public void testUpdatingRolesIsNotAllowed() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.TRUE);
    userUpdate.setRoles(toSet("County-admin"));
    MvcResult result = mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/idm/users/" + STATE_ADMIN_ID)
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(userUpdate)))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized()).andReturn();
    assertExtensible(result, "fixtures/idm/update-user/update-roles-not-allowed.json");
  }

  @Test
  @WithMockCustomUser
  public void testUpdateRemoveAllRoles() throws Exception {

    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.FALSE);
    userUpdate.setRoles(toSet());

    assertUpdateBadRequest(USER_WITH_RACFID_AND_DB_DATA_ID, userUpdate,
        "fixtures/idm/update-user/no-roles-error.json");
  }

  @Test
  @WithMockCustomUser
  public void testUpdateRolesAreNotChanged() throws Exception {

    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.FALSE);

    setDoraSuccess();
    setDisableUserRequestAndResult(USER_NO_RACFID_ID);

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/idm/users/" + USER_NO_RACFID_ID)
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(userUpdate)))
        .andExpect(MockMvcResultMatchers.status().isNoContent())
        .andReturn();
    verify(auditEventService, never()).saveAuditEvent(any(
        UserRoleChangedEvent.class));
    verify(auditEventService, never()).saveAuditEvent(any(
        EmailChangedEvent.class));
  }

  @Test
  @WithMockCustomUser
  public void testValidationUpdateUserChangeInactiveToActive_throwsNoRacfIdInCWS() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.TRUE);
    userUpdate.setRoles(toSet(CWS_WORKER));

    AdminEnableUserRequest enableUserRequest = setEnableUserRequestAndResult(INACTIVE_USER_WITH_NO_ACTIVE_RACFID_IN_CMS);
    setDoraSuccess();

    assertUpdateBadRequest(INACTIVE_USER_WITH_NO_ACTIVE_RACFID_IN_CMS, userUpdate,
        "fixtures/idm/update-user/no-active-cws-user-error.json");

    verify(spyUserIndexService, times(0)).updateUserInIndex(any(User.class));
    verify(cognito, times(0)).adminEnableUser(enableUserRequest);
    verifyDoraCalls(0);
  }

  @Test
  @WithMockCustomUser
  public void testValidationUpdateUserChangeInactiveToActive_withNoRacfIdForUser() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.TRUE);
    userUpdate.setRoles(toSet(CWS_WORKER));

    AdminEnableUserRequest enableUserRequest = setEnableUserRequestAndResult(INACTIVE_USER_WITH_NO_RACFID);
    setDoraSuccess();

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.patch("/idm/users/" + INACTIVE_USER_WITH_NO_RACFID)
                    .contentType(JSON_CONTENT_TYPE)
                    .content(asJsonString(userUpdate)))
            .andExpect(MockMvcResultMatchers.status().isNoContent())
            .andReturn();

    verify(spyUserIndexService, times(1)).updateUserInIndex(any(User.class));
    verify(cognito, times(1)).adminEnableUser(enableUserRequest);
    verifyDoraCalls(1);
  }

  @Test
  @WithMockCustomUser
  public void testValidationUpdateUserChangeInactiveToActive_throwsActiveRacfIdAlreadyInCognito() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.TRUE);
    userUpdate.setRoles(toSet(CWS_WORKER));
    AdminEnableUserRequest enableUserRequest = setEnableUserRequestAndResult(INACTIVE_USER_WITH_ACTIVE_RACFID_IN_CMS);
    setDoraSuccess();

    assertUpdateBadRequest(INACTIVE_USER_WITH_ACTIVE_RACFID_IN_CMS, userUpdate,
        "fixtures/idm/update-user/active-user-with-same-racfid-in-cognito-error.json");

    verify(spyUserIndexService, times(0)).updateUserInIndex(any(User.class));
    verify(cognito, times(0)).adminEnableUser(enableUserRequest);
    verifyDoraCalls(0);
  }

  @Test
  @WithMockCustomUser(roles = {STATE_ADMIN})
  public void testUpdateUserDoraFail() throws Exception {

    setDoraError();

    int oldUserLogsSize = Iterables.size(userLogRepository.findAll());

    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.FALSE);
    userUpdate.setEmail("new@gmail.com");

    AdminUpdateUserAttributesRequest updateAttributesRequest =
        setUpdateUserAttributesRequestAndResult(
            USER_WITH_RACFID_ID,
            attr(EMAIL, "new@gmail.com"),
            attr(EMAIL_VERIFIED, "True")
        );

    AdminDisableUserRequest disableUserRequest = setDisableUserRequestAndResult(
        USER_WITH_RACFID_ID);

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.patch("/idm/users/" + USER_WITH_RACFID_ID)
                    .contentType(JSON_CONTENT_TYPE)
                    .content(asJsonString(userUpdate)))
            .andExpect(MockMvcResultMatchers.status().isInternalServerError())
            .andReturn();

    assertExtensible(result, "fixtures/idm/partial-success-user-update/log-success.json");

    verify(cognito, times(1)).adminUpdateUserAttributes(updateAttributesRequest);
    verify(cognito, times(1)).adminDisableUser(disableUserRequest);

    verify(spyUserIndexService, times(1)).updateUserInIndex(any(User.class));
    verifyDoraCalls(DORA_WS_MAX_ATTEMPTS);

    InOrder inOrder = inOrder(cognito);
    inOrder.verify(cognito).adminUpdateUserAttributes(updateAttributesRequest);
    inOrder.verify(cognito).adminDisableUser(disableUserRequest);

    Iterable<UserLog> userLogs = userLogRepository.findAll();
    int newUserLogsSize = Iterables.size(userLogs);
    assertTrue(newUserLogsSize == oldUserLogsSize + 1);

    UserLog lastUserLog = Iterables.getLast(userLogs);
    assertTrue(lastUserLog.getOperationType() == OperationType.UPDATE);
    assertThat(lastUserLog.getUsername(), is(USER_WITH_RACFID_ID));
  }

  @Test
  @WithMockCustomUser
  public void testPartiallySuccessfulUpdate() throws Exception {

    int oldUserLogsSize = Iterables.size(userLogRepository.findAll());

    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.FALSE);
    userUpdate.setEmail("new@gmail.com");

    AdminUpdateUserAttributesRequest updateAttributesRequest =
        setUpdateUserAttributesRequestAndResult(
            USER_WITH_RACFID_AND_DB_DATA_ID,
            attr(EMAIL, "new@gmail.com"),
            attr(EMAIL_VERIFIED, "True")
        );

    setDoraSuccess();

    AdminDisableUserRequest disableUserRequest = setDisableUserRequestAndFail(
        USER_WITH_RACFID_AND_DB_DATA_ID);

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.patch("/idm/users/" + USER_WITH_RACFID_AND_DB_DATA_ID)
                    .contentType(JSON_CONTENT_TYPE)
                    .content(asJsonString(userUpdate)))
            .andExpect(MockMvcResultMatchers.status().isInternalServerError())
            .andReturn();
    assertExtensible(result,
        "fixtures/idm/partial-success-user-update/partial-update.json");

    verify(cognito, times(1)).adminUpdateUserAttributes(updateAttributesRequest);
    verify(cognito, times(1)).adminDisableUser(disableUserRequest);

    verify(spyUserIndexService, times(1)).updateUserInIndex(any(User.class));
    verifyDoraCalls(1);

    InOrder inOrder = inOrder(cognito);
    inOrder.verify(cognito).adminUpdateUserAttributes(updateAttributesRequest);
    inOrder.verify(cognito).adminDisableUser(disableUserRequest);

    Iterable<UserLog> userLogs = userLogRepository.findAll();
    int newUserLogsSize = Iterables.size(userLogs);
    assertTrue(newUserLogsSize == oldUserLogsSize);
  }

  @Test
  @WithMockCustomUser
  public void testIncidentIdisPresentInCustomError() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.FALSE);
    userUpdate.setPermissions(toSet("RFA-rollout", "Hotline-rollout"));

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.patch("/idm/users/" + USER_WITH_RACFID_AND_DB_DATA_ID)
                    .contentType(JSON_CONTENT_TYPE)
                    .content(asJsonString(userUpdate)))
            .andExpect(MockMvcResultMatchers.status().isInternalServerError())
            .andReturn();

    verify(mockAppender, atLeast(1)).doAppend(captorLoggingEvent.capture());
    LoggingEvent loggingEvent = captorLoggingEvent.getValue();
    Map<String, String> mdcMap = loggingEvent.getMDCPropertyMap();
    assertTrue(mdcMap.containsKey(LoggingRequestIdFilter.REQUEST_ID));
    String requestId = mdcMap.get(LoggingRequestIdFilter.REQUEST_ID);
    assertNotNull(requestId);
    assertTrue(mdcMap.containsKey(LoggingUserIdFilter.USER_ID));
    assertThat(mdcMap.get(LoggingUserIdFilter.USER_ID), is(ADMIN_ID));
    String strResponse = result.getResponse().getContentAsString();
    assertThat(
        strResponse, containsString("\"incident_id\":\"" + requestId + "\""));
  }

  @Test
  @WithMockCustomUser(roles = {STATE_ADMIN})
  public void testUpdateUserNoPermissions() throws Exception {

    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.TRUE);

    AdminEnableUserRequest enableUserRequest = setEnableUserRequestAndResult(USER_NO_RACFID_ID);

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/idm/users/" + USER_NO_RACFID_ID)
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(userUpdate)))
        .andExpect(MockMvcResultMatchers.status().isNoContent())
        .andReturn();

    verify(cognito, times(0)).adminEnableUser(enableUserRequest);
    verify(spyUserIndexService, times(0)).updateUserInIndex(any(User.class));
    verify(auditEventService, never()).saveAuditEvent(any(
        PermissionsChangedEvent.class));
    verify(auditEventService, never()).saveAuditEvent(any(
        UserEnabledStatusChangedEvent.class));
  }

  @Test
  @WithMockCustomUser(county = "Madera")
  public void testUpdateUserByOtherCountyAdmin() throws Exception {
    assertUpdateSomeUserUnauthorized(
        "fixtures/idm/update-user/other-county-admin.json");
  }

  @Test
  @WithMockCustomUser(roles = {"OtherRole"})
  public void testUpdateUserWithOtherRole() throws Exception {
    assertUpdateSomeUserUnauthorized();
  }

  @Test
  @WithMockCustomUser
  public void testUpdateUser_CountyAdminCannotUpdateStateAdmin() throws Exception {
    assertUpdateUserUnauthorized(STATE_ADMIN_ID,
        "fixtures/idm/update-user/county-admin-cannot-update-state-admin.json");
  }

  @Test
  @WithMockCustomUser
  public void testUpdateUserNoChanges() throws Exception {
    assertUpdateNoChangesSuccess();
  }

  @Test
  @WithMockCustomUser(roles = {STATE_ADMIN}, county = "Madera")
  public void testUpdateUserStateAdminIsAuthorized() throws Exception {
    assertUpdateNoChangesSuccess();
  }

  @Test
  @WithMockCustomUser(roles = {OFFICE_ADMIN})
  public void testUpdateUserOfficeAdminIsAuthorized() throws Exception {
    assertUpdateNoChangesSuccess();
  }

  @Test
  @WithMockCustomUser(roles = {OFFICE_ADMIN}, adminOfficeIds = {"otherOfficeId"})
  public void testUpdateUserOfficeOtherOffice() throws Exception {
    assertUpdateSomeUserUnauthorized(
        "fixtures/idm/update-user/other-office.json");
  }

  @Test
  @WithMockCustomUser()
  public void testUpdateEmailOnlyCamelCase() throws Exception {

    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEmail("SomeMail@mail.com");

    AdminUpdateUserAttributesRequest updateAttributesRequest =
        setUpdateUserAttributesRequestAndResult(
            USER_NO_RACFID_ID,
            attr(EMAIL, "somemail@mail.com"),
            attr(EMAIL_VERIFIED, "True")
        );

    setDoraSuccess();

    AdminDisableUserRequest disableUserRequest = setDisableUserRequestAndResult(USER_NO_RACFID_ID);

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/idm/users/" + USER_NO_RACFID_ID)
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(userUpdate)))
        .andExpect(MockMvcResultMatchers.status().isNoContent())
        .andReturn();

    verify(cognito, times(1)).adminUpdateUserAttributes(updateAttributesRequest);
    verify(cognito, times(0)).adminDisableUser(disableUserRequest);
    verify(spyUserIndexService, times(1)).updateUserInIndex(any(User.class));
    verifyDoraCalls(1);
  }

  @Test
  @WithMockCustomUser()
  public void testUpdateEmailExists() throws Exception {

    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEmail("SomeMail@mail.com");

    AdminUpdateUserAttributesRequest request =
        new AdminUpdateUserAttributesRequest()
            .withUsername(USER_WITH_RACFID_ID)
            .withUserPoolId(USERPOOL)
            .withUserAttributes(Arrays.asList(attr(EMAIL, "somemail@mail.com"),
                attr(EMAIL_VERIFIED, "True")));
    when(cognito.adminUpdateUserAttributes(request))
        .thenThrow(new AliasExistsException("the email already used"));

    AdminDisableUserRequest disableUserRequest = setDisableUserRequestAndResult(
        USER_WITH_RACFID_ID);

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/idm/users/" + USER_WITH_RACFID_ID)
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(userUpdate)))
        .andExpect(MockMvcResultMatchers.status().isConflict())
        .andReturn();

    verify(cognito, times(0)).adminDisableUser(disableUserRequest);
    verify(spyUserIndexService, times(0)).updateUserInIndex(any(User.class));
    verifyDoraCalls(0);
  }

  @Test
  @WithMockCustomUser
  public void testUpdateNonRacfidUser_CansPermission() throws Exception {

    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setPermissions(toSet("CANS-rollout"));

    assertUpdateBadRequest(USER_NO_RACFID_ID, userUpdate,
        "fixtures/idm/update-user/non-racfid-user_cans-permission.json");
  }

  @Test
  @WithMockCustomUser
  public void testUpdateRacfidUser_CansPermission() throws Exception {
    assertCanUpdatePermissions(USER_WITH_RACFID_AND_DB_DATA_ID, toSet("CANS-rollout"));
  }

  @Test
  @WithMockCustomUser(roles = {SUPER_ADMIN})
  public void testSuperAdminCanUpdateStateAdminPermissions() throws Exception {
    assertCanUpdatePermissions(STATE_ADMIN_ID, toSet("Hotline-rollout"));
  }

  @Test
  @WithMockCustomUser(roles = {SUPER_ADMIN})
  public void testSuperAdminCanUpdateSuperAdminPermissions() throws Exception {
    assertCanUpdatePermissions(SUPER_ADMIN_ID, toSet("Hotline-rollout"));
  }

  @Test
  @WithMockCustomUser(roles = {SUPER_ADMIN})
  public void testSuperAdminCanDisableSuperAdmin() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.FALSE);
    assertSuccessfulUpdate(SUPER_ADMIN_ID, userUpdate);
  }

  @Test
  @WithMockCustomUser(roles = {SUPER_ADMIN})
  public void testSuperAdminCanDegradeSuperAdmin() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setRoles(toSet("CWS-worker"));
    assertSuccessfulUpdate(SUPER_ADMIN_ID, userUpdate);
  }

  @Test
  @WithMockCustomUser
  public void testValidationUpdateInvalidPhoneNumber() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setPhoneNumber("123-456-7890");

    assertUpdateBadRequest(USER_NO_RACFID_ID, userUpdate,
        "fixtures/idm/update-user/invalid-phone-number.json");
  }

  @Test
  @WithMockCustomUser
  public void testValidationUpdateInvalidCellPhoneNumber() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setCellPhoneNumber("123-456-7890");

    assertUpdateBadRequest(USER_NO_RACFID_ID, userUpdate,
        "fixtures/idm/update-user/invalid-cell-phone-number.json");
  }

  @Test
  @WithMockCustomUser
  public void testValidationUpdateClearPhoneNumber() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setPhoneNumber("");

    assertUpdateBadRequest(USER_NO_RACFID_ID, userUpdate,
        "fixtures/idm/update-user/clear-phone-number.json");
  }

  @Test
  @WithMockCustomUser
  public void testValidationUpdateInvalidPhoneExtension() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setPhoneExtensionNumber("abc");

    assertUpdateBadRequest(USER_NO_RACFID_ID, userUpdate,
        "fixtures/idm/update-user/invalid-phone-extension.json");
  }

  @Test
  @WithMockCustomUser(roles = {COUNTY_ADMIN}, adminOfficeIds = {"otherOfficeId"})
  public void testCountyAdminCanUpdateCountyAdminInSameCounty() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setNotes("new notes");
    assertSuccessfulUpdate(COUNTY_ADMIN_ID, userUpdate);
  }

  @Test
  @WithMockCustomUser(roles = {COUNTY_ADMIN}, county = "Madera", adminOfficeIds = {"otherOfficeId"})
  public void testCountyAdminCanNotUpdateWorkerInOtherCounty() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setNotes("new notes");
    assertUpdateUserUnauthorized(USER_WITH_RACFID_AND_DB_DATA_ID, userUpdate,
        "fixtures/idm/update-user/county-admin-updates-worker-from-other-county.json");
  }

  @Test
  @WithMockCustomUser(roles = {COUNTY_ADMIN}, adminOfficeIds = {"otherOfficeId"})
  public void testCountyAdminCanDowngradeCountyAdminToOfficeAdminInSameCounty() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setRoles(toSet(OFFICE_ADMIN));
    assertSuccessfulUpdate(COUNTY_ADMIN_ID, userUpdate);
  }

  @Test
  @WithMockCustomUser(roles = {COUNTY_ADMIN}, county = "Madera", adminOfficeIds = {"otherOfficeId"})
  public void testCountyAdminCannotDowngradeCountyAdminToOfficeAdminInOtherCounty() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setRoles(toSet(OFFICE_ADMIN));
    assertUpdateUserUnauthorized(COUNTY_ADMIN_ID, userUpdate,
        "fixtures/idm/update-user/county-admin-updates-county-admin-from-other-county.json");
  }

  @Test
  @WithMockCustomUser(roles = {COUNTY_ADMIN}, county = "Madera", adminOfficeIds = {"otherOfficeId"})
  public void testCountyAdminCannotDowngradeCountyAdminToCwsWorkerInOtherCounty() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setRoles(toSet(CWS_WORKER));
    assertUpdateUserUnauthorized(COUNTY_ADMIN_ID, userUpdate,
        "fixtures/idm/update-user/county-admin-updates-county-admin-from-other-county.json");
  }

  @Test
  @WithMockCustomUser(roles = {COUNTY_ADMIN}, adminOfficeIds = {"otherOfficeId"})
  public void testCountyAdminCanDowngradeCountyAdminToCwsWorkerInSameCounty() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setRoles(toSet(CWS_WORKER));
    assertSuccessfulUpdate(COUNTY_ADMIN_ID, userUpdate);
  }

  @Test
  @WithMockCustomUser(roles = {COUNTY_ADMIN}, adminOfficeIds = {"otherOfficeId"})
  public void testCountyAdminCannotUpgradeCwsWorkerToCountyAdminInSameCounty() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setRoles(toSet(COUNTY_ADMIN));
    assertUpdateBadRequest(USER_WITH_RACFID_AND_DB_DATA_ID, userUpdate,
        "fixtures/idm/update-user/county-admin-upgrade-to-county-admin.json");
  }

  @Test
  @WithMockCustomUser(roles = {COUNTY_ADMIN}, adminOfficeIds = {"otherOfficeId"})
  public void testCountyAdminCannotUpgradeOfficeAdminToCountyAdminInSameCounty() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setRoles(toSet(COUNTY_ADMIN));
    assertUpdateBadRequest(OFFICE_ADMIN_ID, userUpdate,
        "fixtures/idm/update-user/county-admin-upgrade-to-county-admin.json");
  }

  @Test
  @WithMockCustomUser(roles = {OFFICE_ADMIN})
  public void testOfficeAdminCanUpdateOfficeAdminInSameOffice() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setNotes("new notes");
    assertSuccessfulUpdate(OFFICE_ADMIN_ID, userUpdate);
  }

  @Test
  @WithMockCustomUser(roles = {OFFICE_ADMIN}, adminOfficeIds = {"otherOfficeId"})
  public void testOfficeAdminCannotUpdateOfficeAdminInOtherOffice() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setNotes("new notes");
    assertUpdateUserUnauthorized(OFFICE_ADMIN_ID, userUpdate,
        "fixtures/idm/update-user/office-admin-updates-office-admin-from-other-office.json");
  }

  @Test
  @WithMockCustomUser(roles = {OFFICE_ADMIN})
  public void testOfficeAdminCanDowngradeOfficeAdminInSameOffice() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setRoles(toSet(CWS_WORKER));
    assertSuccessfulUpdate(OFFICE_ADMIN_ID, userUpdate);
  }

  @Test
  @WithMockCustomUser(roles = {OFFICE_ADMIN}, adminOfficeIds = {"otherOfficeId"})
  public void testOfficeAdminCannotDowngradeOfficeAdminInOtherOffice() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setRoles(toSet(CWS_WORKER));
    assertUpdateUserUnauthorized(OFFICE_ADMIN_ID, userUpdate,
        "fixtures/idm/update-user/office-admin-updates-office-admin-from-other-office.json");
  }

  @Test
  @WithMockCustomUser(roles = {OFFICE_ADMIN})
  public void testOfficeAdminCannotUpgradeCwsWorkerToOfficeAdminInSameOffice() throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setRoles(toSet(OFFICE_ADMIN));
    assertUpdateBadRequest(USER_WITH_RACFID_AND_DB_DATA_ID, userUpdate,
        "fixtures/idm/update-user/office-admin-upgrade-to-office-admin.json");
  }

  private void assertCanUpdatePermissions(String userId, Set<String> permissions) throws Exception{
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setPermissions(permissions);

    assertSuccessfulUpdate(userId, userUpdate);
  }

  private void assertSuccessfulUpdate(String userId, UserUpdate userUpdate) throws Exception {
    setDoraSuccess();

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/idm/users/" + userId)
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(userUpdate)))
        .andExpect(MockMvcResultMatchers.status().isNoContent())
        .andReturn();
  }

  private void assertUpdateBadRequest(String userId, UserUpdate userUpdate, String fixture)
      throws Exception {
    MvcResult result = mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/idm/users/" + userId)
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(userUpdate)))
        .andExpect(MockMvcResultMatchers.status().isBadRequest())
        .andReturn();
    assertExtensible(result, fixture);
  }

  private void assertUpdateNoChangesSuccess() throws Exception {
    String userId = USER_WITH_RACFID_AND_DB_DATA_ID;
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.TRUE);
    userUpdate.setEmail("garcia@gmail.com");
    userUpdate.setPhoneNumber("4646888777");
    userUpdate.setPhoneExtensionNumber("7");
    userUpdate.setPermissions(toSet("Hotline-rollout"));
    userUpdate.setRoles(toSet("CWS-worker"));

    AdminUpdateUserAttributesRequest updateAttributesRequest =
        setUpdateUserAttributesRequestAndResult(
            userId,
            attr(EMAIL, "garcia@gmail.com"),
            attr(EMAIL_VERIFIED, "True")
        );

   AdminEnableUserRequest enableUserRequest = setEnableUserRequestAndResult(userId);
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/idm/users/" + userId)
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(userUpdate)))
        .andExpect(MockMvcResultMatchers.status().isNoContent())
        .andReturn();

    verify(auditEventService, never()).saveAuditEvent(any());
    verify(cognito, times(0)).adminUpdateUserAttributes(updateAttributesRequest);
    verify(cognito, times(0)).adminEnableUser(enableUserRequest);
    verify(spyUserIndexService, times(0)).updateUserInIndex(any(User.class));
    verify(spyNsUserRepository, times(0)).save(any(NsUser.class));
    verifyDoraCalls(0);

    NsUser updatedNsUser =  nsUserRepository.findByUsername(USER_WITH_RACFID_AND_DB_DATA_ID).get(0);
    assertThat(updatedNsUser.getNotes(), is(nullValue()));
  }

  private void assertUpdateSomeUserUnauthorized() throws Exception {
    assertUpdateUserUnauthorized(USER_WITH_RACFID_AND_DB_DATA_ID);
  }

  private void assertUpdateSomeUserUnauthorized(String fixturePath) throws Exception {
    assertUpdateUserUnauthorized(USER_WITH_RACFID_AND_DB_DATA_ID, fixturePath);
  }

  private MvcResult assertUpdateUserUnauthorized(String userId, UserUpdate userUpdate) throws Exception {
    return mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/idm/users/" + userId)
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(userUpdate)))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andReturn();
  }

  private MvcResult assertUpdateUserUnauthorized(String userId) throws Exception {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.FALSE);
    userUpdate.setPermissions(toSet("RFA-rollout", "Hotline-rollout"));
    return assertUpdateUserUnauthorized(userId, userUpdate);
  }

  private void assertUpdateUserUnauthorized(String userId, UserUpdate userUpdate,
      String fixture) throws Exception {
    MvcResult result = assertUpdateUserUnauthorized(userId, userUpdate);
    assertExtensible(result, fixture);
  }

  private void assertUpdateUserUnauthorized(String userId, String fixturePath) throws Exception {
    MvcResult result = assertUpdateUserUnauthorized(userId);
    assertExtensible(result, fixturePath);
  }

  private AdminUpdateUserAttributesRequest setUpdateUserAttributesRequestAndResult(
      String id, AttributeType... userAttributes) {
    AdminUpdateUserAttributesRequest request =
        new AdminUpdateUserAttributesRequest()
            .withUsername(id)
            .withUserPoolId(USERPOOL)
            .withUserAttributes(userAttributes);
    AdminUpdateUserAttributesResult result = new AdminUpdateUserAttributesResult();
    when(cognito.adminUpdateUserAttributes(request)).thenReturn(result);
    return request;
  }

  private AdminDisableUserRequest setDisableUserRequestAndResult(String id) {
    AdminDisableUserRequest request =
        new AdminDisableUserRequest().withUsername(id).withUserPoolId(USERPOOL);
    AdminDisableUserResult result = new AdminDisableUserResult();
    when(cognito.adminDisableUser(request)).thenReturn(result);
    return request;
  }

  private AdminDisableUserRequest setDisableUserRequestAndFail(String id) {
    AdminDisableUserRequest request =
        new AdminDisableUserRequest().withUsername(id).withUserPoolId(USERPOOL);
    when(cognito.adminDisableUser(request))
        .thenThrow(new RuntimeException("Update enable status error"));
    return request;
  }

  private AdminEnableUserRequest setEnableUserRequestAndResult(String id) {
    AdminEnableUserRequest request =
        new AdminEnableUserRequest().withUsername(id).withUserPoolId(USERPOOL);
    AdminEnableUserResult result = new AdminEnableUserResult();
    when(cognito.adminEnableUser(request)).thenReturn(result);
    return request;
  }
}
