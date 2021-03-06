package gov.ca.cwds.idm.service;

import static gov.ca.cwds.BaseIntegrationTest.H2_DRIVER_CLASS_NAME;
import static gov.ca.cwds.BaseIntegrationTest.IDM_BASIC_AUTH_PASS;
import static gov.ca.cwds.BaseIntegrationTest.IDM_BASIC_AUTH_USER;
import static gov.ca.cwds.idm.service.IdmServiceImpl.transformSearchValues;
import static gov.ca.cwds.idm.service.cognito.attribute.StandardUserAttribute.EMAIL;
import static gov.ca.cwds.idm.service.cognito.attribute.StandardUserAttribute.FIRST_NAME;
import static gov.ca.cwds.idm.service.cognito.attribute.StandardUserAttribute.RACFID_STANDARD;
import static gov.ca.cwds.idm.util.TestHelper.user;
import static gov.ca.cwds.service.messages.MessageCode.USER_CREATE_SAVE_TO_SEARCH_AND_DB_LOG_ERRORS;
import static gov.ca.cwds.service.messages.MessageCode.USER_CREATE_SAVE_TO_SEARCH_ERROR;
import static gov.ca.cwds.service.messages.MessageCode.USER_PARTIAL_UPDATE;
import static gov.ca.cwds.service.messages.MessageCode.USER_PARTIAL_UPDATE_AND_SAVE_TO_SEARCH_AND_DB_LOG_ERRORS;
import static gov.ca.cwds.service.messages.MessageCode.USER_PARTIAL_UPDATE_AND_SAVE_TO_SEARCH_ERRORS;
import static gov.ca.cwds.service.messages.MessageCode.USER_UPDATE_SAVE_TO_SEARCH_AND_DB_LOG_ERRORS;
import static gov.ca.cwds.util.LiquibaseUtils.CMS_STORE_URL;
import static gov.ca.cwds.util.LiquibaseUtils.SPRING_BOOT_H2_PASSWORD;
import static gov.ca.cwds.util.LiquibaseUtils.SPRING_BOOT_H2_USER;
import static gov.ca.cwds.util.LiquibaseUtils.TOKEN_STORE_URL;
import static gov.ca.cwds.util.LiquibaseUtils.createCmsDatabase;
import static gov.ca.cwds.util.LiquibaseUtils.createTokenStoreDatabase;
import static gov.ca.cwds.util.Utils.toSet;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserUpdate;
import gov.ca.cwds.idm.exception.PartialSuccessException;
import gov.ca.cwds.idm.persistence.ns.entity.UserLog;
import gov.ca.cwds.idm.service.cognito.CognitoServiceFacade;
import gov.ca.cwds.idm.service.search.UserIndexService;
import gov.ca.cwds.idm.util.WithMockCustomUser;
import java.util.List;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ActiveProfiles({"dev", "idm"})
@SpringBootTest(properties = {
    "perry.identityManager.idmBasicAuthUser=" + IDM_BASIC_AUTH_USER,
    "perry.identityManager.idmBasicAuthPass=" + IDM_BASIC_AUTH_PASS,
    "perry.identityManager.idmMapping=config/idm.groovy",
    "spring.jpa.hibernate.ddl-auto=none",
    "perry.tokenStore.datasource.url=" + TOKEN_STORE_URL,
    "spring.datasource.hikari.jdbcUrl=" + CMS_STORE_URL,
    "spring.datasource.hikari.username=" + SPRING_BOOT_H2_USER,
    "spring.datasource.hikari.password=" + SPRING_BOOT_H2_PASSWORD,
    "search.doraBasicAuthUser=ba_user",
    "search.doraBasicAuthPass=ba_pwd"
})
public class IdmServiceImplTest {

  @Autowired
  private IdmServiceImpl service;

  @MockBean
  private CognitoServiceFacade cognitoServiceFacadeMock;

  @MockBean
  private UserLogTransactionalService userLogTransactionalServiceMock;

  @MockBean
  private UserService userServiceMock;

  @MockBean
  private TransactionalUserService transactionalUserServiceMock;

  @MockBean
  private UserIndexService userIndexService;

  @BeforeClass
  public static void prepareDatabases() throws Exception {
    Class.forName(H2_DRIVER_CLASS_NAME);
    createTokenStoreDatabase();
    createCmsDatabase();
  }

  @Before
  public void before() {
    when(transactionalUserServiceMock.updateUserAttributes(any(UserUpdateRequest.class)))
        .thenReturn(true);
  }

  @Test
  @WithMockCustomUser
  public void testCreateUserSuccess() {
    User user = user();
    String USER_ID = user.getId();
    mockUserService(user);
    String id = service.createUser(user);
    assertThat(id, is(USER_ID));
  }

  @Test
  @WithMockCustomUser
  public void testCreateUser_SearchFail() {
    User user = user();
    String USER_ID = user.getId();
    Exception doraError = new RuntimeException("Dora error");
    mockUserService(user);
    when(userIndexService.createUserInIndex(any(User.class))).thenThrow(doraError);

    try {
      service.createUser(user);
      fail("should throw PartialSuccessException");
    } catch (PartialSuccessException e) {
      assertThat(e.getUserId(), is(USER_ID));
      assertThat(e.getErrorCode(), is(USER_CREATE_SAVE_TO_SEARCH_ERROR));

      List<Exception> causes = e.getCauses();
      assertThat(causes.size(), is(1));
      assertThat(causes.get(0), is(doraError));
    }
  }

  @Test
  @WithMockCustomUser
  public void testCreateUser_SearchAndDbLogFail() {
    User user = user();
    String USER_ID = user.getId();
    mockUserService(user);

    Exception doraError = new RuntimeException("Dora error");
    when(userIndexService.createUserInIndex(any(User.class))).thenThrow(doraError);

    Exception dbError = new RuntimeException("DB error");
    when(userLogTransactionalServiceMock.save(any(UserLog.class))).thenThrow(dbError);

    try {
      service.createUser(user);
      fail("should throw PartialSuccessException");
    } catch (PartialSuccessException e) {
      assertThat(e.getUserId(), is(USER_ID));
      assertThat(e.getErrorCode(), is(USER_CREATE_SAVE_TO_SEARCH_AND_DB_LOG_ERRORS));

      List<Exception> causes = e.getCauses();
      assertThat(causes.size(), is(2));
      assertThat(causes.get(0), is(doraError));
      assertThat(causes.get(1), is(dbError));
    }
  }

  @Test
  @WithMockCustomUser
  public void testUpdateUser_SearchAndDbLogFail() {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setPermissions(toSet("Snapshot-rollout"));

    User existedUser = user();
    String USER_ID = existedUser.getId();
    existedUser.setPermissions(toSet("Hotline-rollout"));

    setGetUserById(USER_ID, existedUser);

    Exception doraError = new RuntimeException("Dora error");
    when(userIndexService.updateUserInIndex(any(User.class))).thenThrow(doraError);

    Exception dbError = new RuntimeException("DB error");
    when(userLogTransactionalServiceMock.save(any(UserLog.class))).thenThrow(dbError);

    try {
      service.updateUser(USER_ID, userUpdate);
      fail("should throw PartialSuccessException");
    } catch (PartialSuccessException e) {
      assertThat(e.getUserId(), is(USER_ID));
      assertThat(e.getErrorCode(), is(USER_UPDATE_SAVE_TO_SEARCH_AND_DB_LOG_ERRORS));

      List<Exception> causes = e.getCauses();
      assertThat(causes.size(), is(2));
      assertThat(causes.get(0), is(doraError));
      assertThat(causes.get(1), is(dbError));
    }
  }

  @Test
  @WithMockCustomUser
  public void testPartialUpdateUser() {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setPermissions(toSet("Snapshot-rollout"));
    userUpdate.setEnabled(Boolean.FALSE);

    User existedUser = user();
    String USER_ID = existedUser.getId();
    existedUser.setPermissions(toSet("RFA-rollout"));

    setGetUserById(USER_ID, existedUser);

    RuntimeException enableStatusError = new RuntimeException("Change Enable Status Error");
    setChangeUserEnabledStatusFail(enableStatusError);

    try {
      service.updateUser(USER_ID, userUpdate);
      fail("should throw PartialSuccessException");
    } catch (PartialSuccessException e) {
      assertThat(e.getUserId(), is(USER_ID));
      assertThat(e.getErrorCode(), is(USER_PARTIAL_UPDATE));

      List<Exception> causes = e.getCauses();
      assertThat(causes.size(), is(1));
      assertThat(causes.get(0), is(enableStatusError));
    }
  }

  @Test
  @WithMockCustomUser
  public void testPartialUpdateUser_SearchFail() {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setPermissions(toSet("Snapshot-rollout"));
    userUpdate.setEnabled(Boolean.FALSE);

    User existedUser = user();
    String USER_ID = existedUser.getId();
    existedUser.setPermissions(toSet("RFA-rollout"));

    setGetUserById(USER_ID, existedUser);

    RuntimeException enableStatusError = new RuntimeException("Change Enable Status Error");
    setChangeUserEnabledStatusFail(enableStatusError);

    Exception doraError = new RuntimeException("Dora error");
    when(userIndexService.updateUserInIndex(any(User.class))).thenThrow(doraError);

    try {
      service.updateUser(USER_ID, userUpdate);
      fail("should throw PartialSuccessException");
    } catch (PartialSuccessException e) {
      assertThat(e.getUserId(), is(USER_ID));
      assertThat(e.getErrorCode(), is(USER_PARTIAL_UPDATE_AND_SAVE_TO_SEARCH_ERRORS));

      List<Exception> causes = e.getCauses();
      assertThat(causes.size(), is(2));
      assertThat(causes.get(0), is(enableStatusError));
      assertThat(causes.get(1), is(doraError));
    }
  }

  @Test
  @WithMockCustomUser
  public void testPartialUpdateUser_SearchAndDbLogFail() {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setPermissions(toSet("Snapshot-rollout"));
    userUpdate.setEnabled(Boolean.FALSE);

    User existedUser = user();
    String USER_ID = existedUser.getId();
    existedUser.setPermissions(toSet("RFA-rollout"));

    setGetUserById(USER_ID, existedUser);

    RuntimeException enableStatusError = new RuntimeException("Change Enable Status Error");
    setChangeUserEnabledStatusFail(enableStatusError);

    Exception doraError = new RuntimeException("Dora error");
    when(userIndexService.updateUserInIndex(any(User.class))).thenThrow(doraError);

    Exception dbError = new RuntimeException("DB error");
    when(userLogTransactionalServiceMock.save(any(UserLog.class))).thenThrow(dbError);

    try {
      service.updateUser(USER_ID, userUpdate);
      fail("should throw PartialSuccessException");
    } catch (PartialSuccessException e) {
      assertThat(e.getUserId(), is(USER_ID));
      assertThat(e.getErrorCode(), is(USER_PARTIAL_UPDATE_AND_SAVE_TO_SEARCH_AND_DB_LOG_ERRORS));

      List<Exception> causes = e.getCauses();
      assertThat(causes.size(), is(3));
      assertThat(causes.get(0), is(enableStatusError));
      assertThat(causes.get(1), is(doraError));
      assertThat(causes.get(2), is(dbError));
    }
  }

  @Test
  @WithMockCustomUser
  public void testUpdateUser_AttrsNotSetAndEnableStatusError() {
    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.FALSE);

    User existedUser = user();
    String USER_ID = existedUser.getId();
    existedUser.setPermissions(toSet("old permission"));

    setGetUserById(USER_ID, existedUser);

    when(transactionalUserServiceMock.updateUserAttributes(any(UserUpdateRequest.class)))
        .thenReturn(false);

    RuntimeException enableStatusError = new RuntimeException("Change Enable Status Error");
    setChangeUserEnabledStatusFail(enableStatusError);

    try {
      service.updateUser(USER_ID, userUpdate);
      fail("should throw RuntimeException");
    } catch (RuntimeException e) {
      assertThat(e, is(enableStatusError));
    }
  }

  @Test
  public void testTransformSearchValues() {
    assertThat(
        transformSearchValues(toSet("ROOBLA", "roobla", "Roobla"), RACFID_STANDARD),
        is(toSet("ROOBLA")));
    assertThat(
        transformSearchValues(toSet("some@email.com", "SOME@EMAIL.COM", "Some@email.com"), EMAIL),
        is(toSet("some@email.com")));
    assertThat(
        transformSearchValues(toSet("John", "JOHN", "john"), FIRST_NAME),
        is(toSet("John", "JOHN", "john")));
  }

  private void mockUserService(User user) {
    when(userServiceMock.enrichWithCwsData(user)).thenReturn(user);
    when(userServiceMock.createUser(user)).thenReturn(user);
  }

  private void setChangeUserEnabledStatusFail(RuntimeException error) {
    doThrow(error).when(cognitoServiceFacadeMock).changeUserEnabledStatus(any(String.class), any(Boolean.class));
 }

  private void setGetUserById(String userId, User result) {
    when(userServiceMock.getUser(userId)).thenReturn(result);
  }
}
