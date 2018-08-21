package gov.ca.cwds.idm.service;

import static gov.ca.cwds.idm.service.IdmServiceImpl.transformSearchValues;
import static gov.ca.cwds.idm.service.cognito.StandardUserAttribute.EMAIL;
import static gov.ca.cwds.idm.service.cognito.StandardUserAttribute.FIRST_NAME;
import static gov.ca.cwds.idm.service.cognito.StandardUserAttribute.RACFID_STANDARD;
import static gov.ca.cwds.service.messages.MessageCode.USER_CREATE_SAVE_TO_SEARCH_AND_DB_LOG_ERRORS;
import static gov.ca.cwds.service.messages.MessageCode.USER_CREATE_SAVE_TO_SEARCH_ERROR;
import static gov.ca.cwds.service.messages.MessageCode.USER_PARTIAL_UPDATE;
import static gov.ca.cwds.service.messages.MessageCode.USER_UPDATE_SAVE_TO_SEARCH_AND_DB_LOG_ERRORS;
import static gov.ca.cwds.util.Utils.toSet;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.cognitoidp.model.UserType;
import gov.ca.cwds.idm.IdmResourceTest;
import gov.ca.cwds.idm.WithMockCustomUser;
import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserEnableStatusRequest;
import gov.ca.cwds.idm.dto.UserUpdate;
import gov.ca.cwds.idm.persistence.UserLogRepository;
import gov.ca.cwds.idm.persistence.model.UserLog;
import gov.ca.cwds.idm.service.cognito.CognitoServiceFacade;
import gov.ca.cwds.idm.service.cognito.util.CognitoUtils;
import gov.ca.cwds.rest.api.domain.PartialSuccessException;
import gov.ca.cwds.service.CwsUserInfoService;
import java.util.HashSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ActiveProfiles({"dev", "idm"})
@SpringBootTest(properties = {
    "perry.identityManager.idmBasicAuthUser=" + IdmResourceTest.IDM_BASIC_AUTH_USER,
    "perry.identityManager.idmBasicAuthPass=" + IdmResourceTest.IDM_BASIC_AUTH_PASS,
    "perry.identityManager.idmMapping=config/idm.groovy",
    "spring.jpa.hibernate.ddl-auto=none"
})
public class IdmServiceImplTest {

  private static final String USER_ID = "17067e4e-270f-4623-b86c-b4d4fa527a34";

  @Autowired
  private IdmServiceImpl service;
  @Autowired
  private UserLogService userLogService;

  private CognitoServiceFacade cognitoServiceFacadeMock = mock(CognitoServiceFacade.class);
  private CwsUserInfoService cwsUserInfoServiceMock = mock(CwsUserInfoService.class);
  private UserLogRepository userLogRepositoryMock = mock(UserLogRepository.class);
  private SearchService searchServiceMock = mock(SearchService.class);

  @Before
  public void before() {
    service.setCognitoServiceFacade(cognitoServiceFacadeMock);
    service.setCwsUserInfoService(cwsUserInfoServiceMock);
    service.setSearchService(searchServiceMock);

    userLogService.setUserLogRepository(userLogRepositoryMock);
  }

  @Test
  @WithMockCustomUser
  public void testCreateUserSuccess() {
    User user = user();
    setCreateUserResult(user, USER_ID);

    String id = service.createUser(user);
    assertThat(id, is(USER_ID));
  }

  @Test
  @WithMockCustomUser
  public void testCreateUser_SearchFail() {
    User user = user();
    setCreateUserResult(user, USER_ID);

    Exception doraError = new RuntimeException("Dora error");
    when(searchServiceMock.createUser(any(User.class))).thenThrow(doraError);

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
    setCreateUserResult(user, USER_ID);

    Exception doraError = new RuntimeException("Dora error");
    when(searchServiceMock.createUser(any(User.class))).thenThrow(doraError);

    Exception dbError = new RuntimeException("DB error");
    when(userLogRepositoryMock.save(any(UserLog.class))).thenThrow(dbError);

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
    userUpdate.setPermissions(toSet("new permission"));

    User existedUser = user();
    existedUser.setPermissions(toSet("old permission"));
    UserType existedUserType = userType(existedUser, USER_ID);

    setUpdateUserAttributesResult(USER_ID, userUpdate, true);
    setGetCognitoUserById(USER_ID, existedUserType);

    Exception doraError = new RuntimeException("Dora error");
    when(searchServiceMock.updateUser(any(User.class))).thenThrow(doraError);

    Exception dbError = new RuntimeException("DB error");
    when(userLogRepositoryMock.save(any(UserLog.class))).thenThrow(dbError);

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
    userUpdate.setPermissions(toSet("new permission"));
    userUpdate.setEnabled(Boolean.FALSE);

    User existedUser = user();
    existedUser.setPermissions(toSet("old permission"));
    UserType existedUserType = userType(existedUser, USER_ID);

    setUpdateUserAttributesResult(USER_ID, userUpdate, true);
    setGetCognitoUserById(USER_ID, existedUserType);

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

  private static User user() {
    User user = new User();
    user.setEmail("gonzales@gmail.com");
    user.setFirstName("Garcia");
    user.setLastName("Gonzales");
    user.setCountyName("Yolo");
    user.setEnabled(Boolean.TRUE);
    return user;
  }

  private void setCreateUserResult(User user, String newId) {
    UserType newUser = userType(user, newId);
    when(cognitoServiceFacadeMock.createUser(user)).thenReturn(newUser);
  }

  private void setUpdateUserAttributesResult(String userId, UserUpdate userUpdate, boolean result) {
    when(cognitoServiceFacadeMock.updateUserAttributes(eq(userId), any(UserType.class), eq(userUpdate)))
        .thenReturn(result);
  }

  private void setChangeUserEnabledStatusFail(RuntimeException error) {
    when(cognitoServiceFacadeMock.changeUserEnabledStatus(any(UserEnableStatusRequest.class)))
        .thenThrow(error);
  }

  private void setGetCognitoUserById(String userId, UserType result) {
    when(cognitoServiceFacadeMock.getCognitoUserById(userId)).thenReturn(result);
  }

  private UserType userType(User user, String userId) {
    UserType userType = new UserType();
    userType.setUsername(userId);
    userType.setEnabled(true);
    userType.setUserStatus("FORCE_CHANGE_PASSWORD");
    userType.withAttributes(CognitoUtils.buildCreateUserAttributes(user));
    return userType;
  }
}
