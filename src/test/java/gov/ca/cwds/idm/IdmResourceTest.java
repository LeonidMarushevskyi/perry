package gov.ca.cwds.idm;

import static gov.ca.cwds.idm.BaseLiquibaseTest.CMS_STORE_URL;
import static gov.ca.cwds.idm.BaseLiquibaseTest.TOKEN_STORE_URL;
import static gov.ca.cwds.idm.IdmResource.DATETIME_FORMAT_PATTERN;
import static gov.ca.cwds.idm.persistence.model.OperationType.CREATE;
import static gov.ca.cwds.idm.persistence.model.OperationType.UPDATE;
import static gov.ca.cwds.idm.service.cognito.CustomUserAttribute.COUNTY;
import static gov.ca.cwds.idm.service.cognito.CustomUserAttribute.PERMISSIONS;
import static gov.ca.cwds.idm.service.cognito.CustomUserAttribute.RACFID_CUSTOM;
import static gov.ca.cwds.idm.service.cognito.CustomUserAttribute.RACFID_CUSTOM_2;
import static gov.ca.cwds.idm.service.cognito.CustomUserAttribute.ROLES;
import static gov.ca.cwds.idm.service.cognito.StandardUserAttribute.EMAIL;
import static gov.ca.cwds.idm.service.cognito.StandardUserAttribute.FIRST_NAME;
import static gov.ca.cwds.idm.service.cognito.StandardUserAttribute.LAST_NAME;
import static gov.ca.cwds.idm.service.cognito.StandardUserAttribute.RACFID_STANDARD;
import static gov.ca.cwds.idm.service.cognito.util.CognitoUsersSearchCriteriaUtil.DEFAULT_PAGESIZE;
import static gov.ca.cwds.idm.service.cognito.util.CognitoUsersSearchCriteriaUtil.composeToGetFirstPageByAttribute;
import static gov.ca.cwds.idm.util.AssertFixtureUtils.assertExtensible;
import static gov.ca.cwds.idm.util.AssertFixtureUtils.assertNonStrict;
import static gov.ca.cwds.idm.util.AssertFixtureUtils.assertStrict;
import static gov.ca.cwds.util.Utils.toSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.AdminCreateUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminCreateUserResult;
import com.amazonaws.services.cognitoidp.model.AdminDisableUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminDisableUserResult;
import com.amazonaws.services.cognitoidp.model.AdminEnableUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminEnableUserResult;
import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminGetUserResult;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesResult;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.cognitoidp.model.InternalErrorException;
import com.amazonaws.services.cognitoidp.model.InvalidParameterException;
import com.amazonaws.services.cognitoidp.model.ListUsersRequest;
import com.amazonaws.services.cognitoidp.model.ListUsersResult;
import com.amazonaws.services.cognitoidp.model.UserNotFoundException;
import com.amazonaws.services.cognitoidp.model.UserType;
import com.amazonaws.services.cognitoidp.model.UsernameExistsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserUpdate;
import gov.ca.cwds.idm.persistence.UserLogRepository;
import gov.ca.cwds.idm.persistence.model.OperationType;
import gov.ca.cwds.idm.persistence.model.UserLog;
import gov.ca.cwds.idm.service.IdmServiceImpl;
import gov.ca.cwds.idm.service.SearchService;
import gov.ca.cwds.idm.service.UserLogService;
import gov.ca.cwds.idm.service.cognito.CognitoProperties;
import gov.ca.cwds.idm.service.cognito.CognitoServiceFacade;
import gov.ca.cwds.service.messages.MessagesService;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import liquibase.util.StringUtils;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InOrder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles({"dev", "idm"})
@SpringBootTest(
  properties = {
    "perry.identityManager.idmBasicAuthUser=" + IdmResourceTest.IDM_BASIC_AUTH_USER,
    "perry.identityManager.idmBasicAuthPass=" + IdmResourceTest.IDM_BASIC_AUTH_PASS,
    "perry.identityManager.idmMapping=config/idm.groovy",
    "spring.jpa.hibernate.ddl-auto=none",
    "perry.tokenStore.datasource.url=" + TOKEN_STORE_URL,
    "spring.datasource.url=" + CMS_STORE_URL
  }
)
public class IdmResourceTest extends BaseLiquibaseTest {

  private static final String USER_NO_RACFID_ID = "2be3221f-8c2f-4386-8a95-a68f0282efb0";
  private static final String USER_WITH_RACFID_ID = "24051d54-9321-4dd2-a92f-6425d6c455be";
  private static final String USER_WITH_RACFID_AND_DB_DATA_ID =
      "d740ec1d-80ae-4d84-a8c4-9bed7a942f5b";
  private static final String NEW_USER_SUCCESS_ID = "17067e4e-270f-4623-b86c-b4d4fa527a34";
  private static final String ABSENT_USER_ID = "absentUserId";
  private static final String ERROR_USER_ID = "errorUserId";
  private static final String NEW_USER_ES_FAIL_ID = "08e14c57-6e5e-48dd-8172-e8949c2a7f76";
  private static final String ES_ERROR_CREATE_USER_EMAIL = "es.error@create.com";

  private static final String USERPOOL = "userpool";
  private static final String SOME_PAGINATION_TOKEN = "somePaginationToken";

  static public final String IDM_BASIC_AUTH_USER = "user";
  static public final String IDM_BASIC_AUTH_PASS = "pass";

  private static final String BASIC_AUTH_HEADER = prepareBasicAuthHeader();

  private static final MediaType JSON_CONTENT_TYPE =
      new MediaType(
          MediaType.APPLICATION_JSON.getType(),
          MediaType.APPLICATION_JSON.getSubtype(),
          Charset.forName("utf8"));

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private CognitoServiceFacade cognitoServiceFacade;

  @Autowired private MessagesService messagesService;

  @Autowired  private IdmServiceImpl idmService;

  @Autowired private UserLogRepository userLogRepository;

  @Autowired  private UserLogService userLogService;

  private SearchService searchService = mock(SearchService.class);

  private MockMvc mockMvc;

  private AWSCognitoIdentityProvider cognito;

  @BeforeClass
  public static void beforeClass() throws Exception {
    runLiquibaseScript(CMS_STORE_URL, "liquibase/cms-data.xml");
  }

  @Before
  public void before() {
    cognitoServiceFacade.setMessagesService(messagesService);
    idmService.setSearchService(searchService);
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    cognito = cognitoServiceFacade.getIdentityProvider();
  }

  private static String prepareBasicAuthHeader() {
    String authString = IDM_BASIC_AUTH_USER + ":" + IDM_BASIC_AUTH_PASS;
    byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
    String authStringEnc = new String(authEncBytes);
    return "Basic " + authStringEnc;
  }

  @Test
  @WithMockCustomUser
  public void testGetPermissions() throws Exception {

    MvcResult result =
        mockMvc
            .perform(MockMvcRequestBuilders.get("/idm/permissions"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(JSON_CONTENT_TYPE))
            .andReturn();

    assertStrict(result, "fixtures/idm/permissions/valid.json");
  }

  @Test
  @WithMockCustomUser(roles = {"OtherRole"})
  public void testGetPermissionsWithOtherRole() throws Exception {

    mockMvc
        .perform(MockMvcRequestBuilders.get("/idm/permissions"))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andReturn();
  }

  @Test
  @WithMockCustomUser(roles = {"CARES-admin"})
  public void testGetPermissionsWithCaresAdminRole() throws Exception {

    MvcResult result =
        mockMvc
            .perform(MockMvcRequestBuilders.get("/idm/permissions"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(JSON_CONTENT_TYPE))
            .andReturn();

    assertStrict(result, "fixtures/idm/permissions/valid.json");
  }

  @Test
  @WithMockCustomUser
  public void testGetUserNoRacfId() throws Exception {
    testGetValidYoloUser(USER_NO_RACFID_ID, "fixtures/idm/get-user/no-racfid-valid.json");
  }

  @Test
  @WithMockCustomUser
  public void testGetUserWithRacfId() throws Exception {
    testGetValidYoloUser(USER_WITH_RACFID_ID, "fixtures/idm/get-user/with-racfid-valid.json");
  }

  @Test
  @WithMockCustomUser
  public void testGetUserWithRacfIdAndDbData() throws Exception {
    testGetValidYoloUser(
        USER_WITH_RACFID_AND_DB_DATA_ID,
        "fixtures/idm/get-user/with-racfid-and-db-data-valid.json");
  }

  @Test
  @WithMockCustomUser
  public void testGetAbsentUser() throws Exception {

    mockMvc
        .perform(MockMvcRequestBuilders.get("/idm/users/" + ABSENT_USER_ID))
        .andExpect(MockMvcResultMatchers.status().isNotFound())
        .andReturn();
  }

  @Test
  @WithMockCustomUser
  public void testGetUserError() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/idm/users/" + ERROR_USER_ID))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andReturn();
  }

  @Test
  @WithMockCustomUser(county = "Madera")
  public void testGetUserByOtherCountyAdmin() throws Exception {

    mockMvc
        .perform(MockMvcRequestBuilders.get("/idm/users/" + USER_NO_RACFID_ID))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andReturn();
  }

  @Test
  @WithMockCustomUser(roles = {"OtherRole"})
  public void testGetUserWithOtherRole() throws Exception {

    mockMvc
        .perform(MockMvcRequestBuilders.get("/idm/users/" + USER_NO_RACFID_ID))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andReturn();
  }

  @Test
  public void testGetUsers() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/idm/users")
                    .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(JSON_CONTENT_TYPE))
            .andReturn();

    assertNonStrict(result, "fixtures/idm/get-users/all-valid.json");
  }

  @Test
  public void testSearchUsersByRacfid() throws Exception {

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/idm/users/search")
                    .contentType(JSON_CONTENT_TYPE)
                    .content("[\"YOLOD\", \"SMITHBO\"]")
                    .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(JSON_CONTENT_TYPE))
            .andReturn();

    assertNonStrict(result, "fixtures/idm/users-search/valid.json");
  }

  @Test
  public void testSearchUsersByRacfidFilterOutRepeats() throws Exception {

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/idm/users/search")
                    .contentType(JSON_CONTENT_TYPE)
                    .content("[\"YOLOD\", \"yolod\", \"YOLOD\"]")
                    .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(JSON_CONTENT_TYPE))
            .andReturn();

    assertNonStrict(result, "fixtures/idm/users-search/yolod.json");
  }

  @Test
  public void testgetUsersPage() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/idm/users?paginationToken=" + SOME_PAGINATION_TOKEN)
                    .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(JSON_CONTENT_TYPE))
            .andReturn();

    assertNonStrict(result, "fixtures/idm/get-users/search-valid.json");
  }

  @Test
  @WithMockCustomUser(roles = {"OtherRole"})
  public void testGetUsersWithOtherRole() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/idm/users"))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andReturn();
  }

  @Test
  @WithMockCustomUser()
  public void testGetUsersWithAdminRole() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/idm/users"))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andReturn();
  }

  @Test
  @WithMockCustomUser
  public void testCreateUserSuccess() throws Exception {
    User user = user();
    AdminCreateUserRequest request = cognitoServiceFacade.createAdminCreateUserRequest(user);
    setCreateUserResult(request, NEW_USER_SUCCESS_ID);

    AdminGetUserRequest getUserRequest =
        cognitoServiceFacade.createAdminGetUserRequest(NEW_USER_SUCCESS_ID);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/idm/users")
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(user)))
        .andExpect(MockMvcResultMatchers.status().isCreated())
        .andExpect(header().string("location", "http://localhost/idm/users/" + NEW_USER_SUCCESS_ID))
        .andReturn();

    verify(cognito, times(1)).adminGetUser(getUserRequest);
    verify(cognito, times(1)).adminCreateUser(request);
    verify(searchService, times(1)).createUser(any(User.class));
  }

  @Test
  @WithMockCustomUser
  public void testCreateUserDoraFail() throws Exception {

    int oldUserLogsSize = Iterables.size(userLogRepository.findAll());

    User user = user();
    user.setEmail(ES_ERROR_CREATE_USER_EMAIL);

    doThrow(new RestClientException("Elastic Search error"))
        .when(searchService).createUser(any(User.class));

    AdminCreateUserRequest request = cognitoServiceFacade.createAdminCreateUserRequest(user);
    setCreateUserResult(request, NEW_USER_ES_FAIL_ID);

    AdminGetUserRequest getUserRequest =
        cognitoServiceFacade.createAdminGetUserRequest(NEW_USER_ES_FAIL_ID);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/idm/users")
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(user)))
        .andExpect(MockMvcResultMatchers.status().isCreated())
        .andExpect(header().string("location", "http://localhost/idm/users/" + NEW_USER_ES_FAIL_ID))
        .andReturn();

    verify(cognito, times(1)).adminGetUser(getUserRequest);
    verify(cognito, times(1)).adminCreateUser(request);
    verify(searchService, times(1)).createUser(any(User.class));

    Iterable<UserLog> userLogs = userLogRepository.findAll();
    int newUserLogsSize = Iterables.size(userLogs);
    assertThat(newUserLogsSize, is(oldUserLogsSize + 1));

    UserLog lastUserLog = Iterables.getLast(userLogs);
    assertTrue(lastUserLog.getOperationType() == OperationType.CREATE);
    assertThat(lastUserLog.getUsername(), is(NEW_USER_ES_FAIL_ID));
  }

  @Test
  @WithMockCustomUser
  public void testCreateUserAlreadyExists() throws Exception {
    User user = user();
    user.setEmail("some.existing@email");

    AdminCreateUserRequest request = cognitoServiceFacade.createAdminCreateUserRequest(user);
    when(cognito.adminCreateUser(request))
        .thenThrow(new UsernameExistsException("user already exists"));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/idm/users")
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(user)))
        .andExpect(MockMvcResultMatchers.status().isConflict())
        .andReturn();

    verify(cognito, times(1)).adminCreateUser(request);
    verify(searchService, times(0)).createUser(any(User.class));
  }

  @Test
  @WithMockCustomUser
  public void testCreateUserInOtherCounty() throws Exception {
    User user = user();
    user.setCountyName("OtherCounty");

    AdminCreateUserRequest request = cognitoServiceFacade.createAdminCreateUserRequest(user);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/idm/users")
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(user)))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andReturn();

    verify(cognito, times(0)).adminCreateUser(request);
    verify(searchService, times(0)).createUser(any(User.class));
  }

  @Test
  @WithMockCustomUser
  public void testCreateUserWithEmptyEmail() throws Exception {
    User user = user();
    user.setEmail("");
    testCreateUserValidationError(user);
  }

  @Test
  @WithMockCustomUser
  public void testCreateUserWithNullFirstName() throws Exception {
    User user = user();
    user.setFirstName(null);
    testCreateUserValidationError(user);
  }

  @Test
  @WithMockCustomUser
  public void testCreateUserWithBlankLastName() throws Exception {
    User user = user();
    user.setLastName("   ");
    testCreateUserValidationError(user);
  }

  @Test
  @WithMockCustomUser
  public void testCreateUserWithEmptyCountyName() throws Exception {
    User user = user();
    user.setCountyName("");
    testCreateUserValidationError(user);
  }

  @Test
  @WithMockCustomUser
  public void testCreateUserCognitoValidationError() throws Exception {
    User user = user();
    user.setOffice("too long string");
    AdminCreateUserRequest request = cognitoServiceFacade.createAdminCreateUserRequest(user);
    when(cognito.adminCreateUser(request))
        .thenThrow(new InvalidParameterException("invalid parameter"));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/idm/users")
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(user)))
        .andExpect(MockMvcResultMatchers.status().isBadRequest())
        .andReturn();

    verify(cognito, times(1)).adminCreateUser(request);
  }

  @Test
  @WithMockCustomUser
  public void testUpdateUser() throws Exception {

    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.FALSE);
    userUpdate.setPermissions(toSet("RFA-rollout", "Hotline-rollout"));

    AdminUpdateUserAttributesRequest updateAttributesRequest =
        setUpdateUserAttributesRequestAndResult(
            USER_NO_RACFID_ID, attr(PERMISSIONS.getName(), "RFA-rollout:Hotline-rollout"));

    AdminDisableUserRequest disableUserRequest = setDisableUserRequestAndResult(USER_NO_RACFID_ID);

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/idm/users/" + USER_NO_RACFID_ID)
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(userUpdate)))
        .andExpect(MockMvcResultMatchers.status().isNoContent())
        .andReturn();

    verify(cognito, times(1)).adminUpdateUserAttributes(updateAttributesRequest);
    verify(cognito, times(1)).adminDisableUser(disableUserRequest);
    verify(searchService, times(2)).updateUser(any(User.class));

    InOrder inOrder = inOrder(cognito);
    inOrder.verify(cognito).adminUpdateUserAttributes(updateAttributesRequest);
    inOrder.verify(cognito).adminDisableUser(disableUserRequest);
  }

  @Test
  @WithMockCustomUser
  public void testUpdateUserDoraFail() throws Exception {

    doThrow(new RestClientException("Elastic Search error"))
        .when(searchService).updateUser(any(User.class));

    int oldUserLogsSize = Iterables.size(userLogRepository.findAll());

    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.FALSE);
    userUpdate.setPermissions(toSet("RFA-rollout", "Hotline-rollout"));

    AdminUpdateUserAttributesRequest updateAttributesRequest =
        setUpdateUserAttributesRequestAndResult(
            USER_WITH_RACFID_ID, attr(PERMISSIONS.getName(), "RFA-rollout:Hotline-rollout"));

    AdminDisableUserRequest disableUserRequest = setDisableUserRequestAndResult(USER_WITH_RACFID_ID);

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/idm/users/" + USER_WITH_RACFID_ID)
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(userUpdate)))
        .andExpect(MockMvcResultMatchers.status().isNoContent())
        .andReturn();

    verify(cognito, times(1)).adminUpdateUserAttributes(updateAttributesRequest);
    verify(cognito, times(1)).adminDisableUser(disableUserRequest);
    verify(searchService, times(2)).updateUser(any(User.class));

    InOrder inOrder = inOrder(cognito);
    inOrder.verify(cognito).adminUpdateUserAttributes(updateAttributesRequest);
    inOrder.verify(cognito).adminDisableUser(disableUserRequest);

    Iterable<UserLog> userLogs = userLogRepository.findAll();
    int newUserLogsSize = Iterables.size(userLogs);
    assertTrue(newUserLogsSize == oldUserLogsSize + 2);

    UserLog beforeLastUserLog = Iterables.get(userLogs, newUserLogsSize - 2);
    assertTrue(beforeLastUserLog.getOperationType() == OperationType.UPDATE);
    assertThat(beforeLastUserLog.getUsername(), is(USER_WITH_RACFID_ID));

    UserLog lastUserLog = Iterables.getLast(userLogs);
    assertTrue(lastUserLog.getOperationType() == OperationType.UPDATE);
    assertThat(lastUserLog.getUsername(), is(USER_WITH_RACFID_ID));
  }

  @Test
  @WithMockCustomUser
  public void testUpdateUserNoChanges() throws Exception {

    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.TRUE);
    userUpdate.setPermissions(toSet("RFA-rollout", "Snapshot-rollout"));

    AdminUpdateUserAttributesRequest updateAttributesRequest =
        setUpdateUserAttributesRequestAndResult(
            USER_NO_RACFID_ID, attr(PERMISSIONS.getName(), "RFA-rollout:Snapshot-rollout"));

    AdminEnableUserRequest enableUserRequest = setEnableUserRequestAndResult(USER_NO_RACFID_ID);

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/idm/users/" + USER_NO_RACFID_ID)
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(userUpdate)))
        .andExpect(MockMvcResultMatchers.status().isNoContent())
        .andReturn();

    verify(cognito, times(0)).adminUpdateUserAttributes(updateAttributesRequest);

    verify(cognito, times(0)).adminEnableUser(enableUserRequest);

    verify(searchService, times(0)).createUser(any(User.class));
  }

  @Test
  @WithMockCustomUser
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

    verify(searchService, times(0)).createUser(any(User.class));
  }

  @Test
  @WithMockCustomUser(county = "Madera")
  public void testUpdateUserByOtherCountyAdmin() throws Exception {

    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.FALSE);
    userUpdate.setPermissions(toSet("RFA-rollout", "Hotline-rollout"));

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/idm/users/" + USER_NO_RACFID_ID)
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(userUpdate)))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andReturn();
  }

  @Test
  @WithMockCustomUser(roles = {"OtherRole"})
  public void testUpdateUserWithOtherRole() throws Exception {

    UserUpdate userUpdate = new UserUpdate();
    userUpdate.setEnabled(Boolean.FALSE);
    userUpdate.setPermissions(toSet("RFA-rollout", "Hotline-rollout"));

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/idm/users/" + USER_NO_RACFID_ID)
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(userUpdate)))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andReturn();
  }

  @Test
  @WithMockCustomUser
  public void testVerifyUsers() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/idm/users/verify?email=test@test.com&racfid=SMITHBO"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(JSON_CONTENT_TYPE))
            .andReturn();

    assertNonStrict(result, "fixtures/idm/verify-user/verify-valid.json");
  }

  @Test
  @WithMockCustomUser
  public void testVerifyUsersRacfidInLowerCase() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/idm/users/verify?email=test@test.com&racfid=smithbo"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(JSON_CONTENT_TYPE))
            .andReturn();

    assertNonStrict(result, "fixtures/idm/verify-user/verify-valid.json");
  }

  @Test
  @WithMockCustomUser
  public void testVerifyUsersWithEmailInMixedCase() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/idm/users/verify?email=Test@Test.com&racfid=SMITHBO"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(JSON_CONTENT_TYPE))
            .andReturn();

    assertNonStrict(result, "fixtures/idm/verify-user/verify-valid.json");
  }

  @Test
  @WithMockCustomUser
  public void testVerifyUsersNoRacfId() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/idm/users/verify?email=test@test.com&racfid=SMITHB1"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(JSON_CONTENT_TYPE))
            .andReturn();

    assertNonStrict(result, "fixtures/idm/verify-user/verify-no-racfid.json");
  }

  @Test
  @WithMockCustomUser
  public void testVerifyUsersCognitoUserIsPresent() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/idm/users/verify?email=julio@gmail.com&racfid=SMITHBO"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(JSON_CONTENT_TYPE))
            .andReturn();

    assertNonStrict(result, "fixtures/idm/verify-user/verify-user-present.json");
  }

  @Test
  @WithMockCustomUser(roles = {"OtherRole"})
  public void testVerifyUserWithOtherRole() throws Exception {

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/idm/users/verify?email=test@test.com&racfid=CWDS"))
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andReturn();
  }

  @Test
  @WithMockCustomUser(county = "Madera")
  public void testVerifyUsersOtherCounty() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/idm/users/verify?email=test@test.com&racfid=SMITHBO"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(JSON_CONTENT_TYPE))
            .andReturn();

    assertNonStrict(result, "fixtures/idm/verify-user/verify-other-county.json");
  }

  @Test
  public void testGetFailedOperations() throws Exception {
    userLogRepository.deleteAll();

    userLog(USER_WITH_RACFID_AND_DB_DATA_ID, CREATE, 1000);
    userLog("this-id-should-be-unused", CREATE, 2000);
    userLog(USER_NO_RACFID_ID, CREATE, 3000);
    userLog(USER_WITH_RACFID_ID, CREATE, 4000);
    userLog(USER_WITH_RACFID_ID, UPDATE, 5000);
    userLog(USER_WITH_RACFID_AND_DB_DATA_ID, UPDATE, 6000);

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/idm/users/failed-operations?date=" + getDateString(new Date(2000)))
                    .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(JSON_CONTENT_TYPE))
            .andReturn();
    System.out.println(result.getResponse().getContentAsString());

    assertNonStrict(result, "fixtures/idm/failed-operations/failed-operations-valid.json");
  }

  @Test
  @WithMockCustomUser
  public void testGetFailedOperationsNoBasicAuth() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/idm/users/failed-operations?date=" + getDateString(new Date(2000))))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andReturn();
  }

  @Test
  public void testGetFailedOperationsInvalidDateFormat() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/idm/users/failed-operations?date=2018-08-01-13.26.33")
                    .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andReturn();
    assertExtensible(result, "fixtures/idm/failed-operations/failed-operations-invalid-date.json");
  }

  private UserLog userLog(String userName, OperationType operation,  long date) {
    UserLog log = new UserLog();
    log.setUsername(userName);
    log.setOperationType(operation);
    log.setOperationTime(new Date(date));
    return userLogRepository.save(log);
  }

  private static String getDateString(Date date) {
    DateFormat dateFormat = new SimpleDateFormat(DATETIME_FORMAT_PATTERN);
    return dateFormat.format(date);
  }

  private void testGetValidYoloUser(String userId, String fixtureFilePath) throws Exception {

    MvcResult result =
        mockMvc
            .perform(MockMvcRequestBuilders.get("/idm/users/" + userId))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(JSON_CONTENT_TYPE))
            .andReturn();

    assertNonStrict(result, fixtureFilePath);
  }

  private void testCreateUserValidationError(User user) throws Exception {

    AdminCreateUserRequest request = cognitoServiceFacade.createAdminCreateUserRequest(user);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/idm/users")
                .contentType(JSON_CONTENT_TYPE)
                .content(asJsonString(user)))
        .andExpect(MockMvcResultMatchers.status().isBadRequest())
        .andReturn();

    verify(cognito, times(0)).adminCreateUser(request);
    verify(searchService, times(0)).createUser(any(User.class));
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

  private AdminEnableUserRequest setEnableUserRequestAndResult(String id) {
    AdminEnableUserRequest request =
        new AdminEnableUserRequest().withUsername(id).withUserPoolId(USERPOOL);
    AdminEnableUserResult result = new AdminEnableUserResult();
    when(cognito.adminEnableUser(request)).thenReturn(result);
    return request;
  }

  private static User user() {
    User user = new User();
    user.setEmail("gonzales@gmail.com");
    user.setFirstName("Garcia");
    user.setLastName("Gonzales");
    user.setCountyName(WithMockCustomUser.COUNTY);
    return user;
  }

  private AdminCreateUserRequest setCreateUserResult(AdminCreateUserRequest request, String newId) {

    UserType newUser = new UserType();
    newUser.setUsername(newId);
    newUser.setEnabled(true);
    newUser.setUserStatus("FORCE_CHANGE_PASSWORD");
    newUser.withAttributes(request.getUserAttributes());

    AdminCreateUserResult result = new AdminCreateUserResult().withUser(newUser);
    when(cognito.adminCreateUser(request)).thenReturn(result);
    return request;
  }

  private static String asJsonString(final Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Component
  static class TestPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName)
        throws BeansException {
      if (beanName.equals("cognitoServiceFacade")) {
        return new TestCognitoServiceFacade();
      } else {
        return bean;
      }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName)
        throws BeansException {
      return bean;
    }
  }

  public static class TestCognitoServiceFacade extends CognitoServiceFacade {

    private AWSCognitoIdentityProvider cognito;

    @PostConstruct
    @Override
    public void init() {
      cognito = mock(AWSCognitoIdentityProvider.class);

      CognitoProperties properties = new CognitoProperties();
      properties.setIamAccessKeyId("iamAccessKeyId");
      properties.setIamSecretKey("iamSecretKey");
      properties.setUserpool(USERPOOL);
      properties.setRegion("us-east-2");

      setProperties(properties);
      setIdentityProvider(cognito);

      TestUser userWithoutRacfid =
          testUser(
              USER_NO_RACFID_ID,
              Boolean.TRUE,
              "FORCE_CHANGE_PASSWORD",
              date(2018, 5, 4),
              date(2018, 5, 30),
              "donzano@gmail.com",
              "Don",
              "Manzano",
              WithMockCustomUser.COUNTY,
              "RFA-rollout:Snapshot-rollout:",
              "CWS-worker:CWS-admin",
              null);

      TestUser userWithRacfid =
          testUser(
              USER_WITH_RACFID_ID,
              Boolean.TRUE,
              "CONFIRMED",
              date(2018, 5, 4),
              date(2018, 5, 29),
              "julio@gmail.com",
              "Julio",
              "Iglecias",
              WithMockCustomUser.COUNTY,
              "Hotline-rollout",
              "CWS-worker:CWS-admin",
              "YOLOD");

      TestUser userWithRacfidAndDbData =
          testUser(
              USER_WITH_RACFID_AND_DB_DATA_ID,
              Boolean.TRUE,
              "CONFIRMED",
              date(2018, 5, 3),
              date(2018, 5, 31),
              "garcia@gmail.com",
              "Garcia",
              "Gonzales",
              WithMockCustomUser.COUNTY,
              "test",
              null,
              "SMITHBO");

      TestUser newSuccessUser =
          testUser(
              NEW_USER_SUCCESS_ID,
              Boolean.TRUE,
              "FORCE_CHANGE_PASSWORD",
              date(2018, 5, 4),
              date(2018, 5, 30),
              "gonzales@gmail.com",
              "Garcia",
              "Gonzales",
              WithMockCustomUser.COUNTY,
              null,
              null,
              null);

      TestUser doraFailUser =
          testUser(
              NEW_USER_ES_FAIL_ID,
              Boolean.TRUE,
              "FORCE_CHANGE_PASSWORD",
              date(2018, 5, 4),
              date(2018, 5, 30),
              ES_ERROR_CREATE_USER_EMAIL,
              "Garcia",
              "Gonzales",
              WithMockCustomUser.COUNTY,
              null,
              null,
              null);

      setUpGetAbsentUserRequestAndResult();

      setUpGetErrorUserRequestAndResult();

      setListUsersRequestAndResult("", userWithoutRacfid, userWithRacfid, userWithRacfidAndDbData);

      setListUsersRequestAndResult(SOME_PAGINATION_TOKEN, userWithoutRacfid);

      setSearchUsersByEmailRequestAndResult("julio@gmail.com", "test@test.com", userWithRacfid);

      setSearchByRacfidRequestAndResult(userWithRacfid);

      setSearchByRacfidRequestAndResult(userWithRacfidAndDbData);
    }

    private void setListUsersRequestAndResult(String paginationToken, TestUser... testUsers) {
      ListUsersRequest request =
          new ListUsersRequest().withUserPoolId(USERPOOL).withLimit(DEFAULT_PAGESIZE);

      if (StringUtils.isNotEmpty(paginationToken)) {
        request.withPaginationToken(paginationToken);
      }

      List<UserType> userTypes =
          Arrays.stream(testUsers)
              .map(TestCognitoServiceFacade::userType)
              .collect(Collectors.toList());

      ListUsersResult result = new ListUsersResult().withUsers(userTypes);

      when(cognito.listUsers(request)).thenReturn(result);
    }

    private TestUser testUser(
        String id,
        Boolean enabled,
        String status,
        Date userCreateDate,
        Date lastModifiedDate,
        String email,
        String firstName,
        String lastName,
        String county,
        String permissions,
        String roles,
        String racfId) {

      TestUser testUser =
          new TestUser(
              id,
              enabled,
              status,
              userCreateDate,
              lastModifiedDate,
              email,
              firstName,
              lastName,
              county,
              permissions,
              roles,
              racfId);

      setUpGetUserRequestAndResult(testUser);

      return testUser;
    }

    private static Collection<AttributeType> attrs(TestUser testUser) {
      Collection<AttributeType> attrs = new ArrayList<>();

      if (testUser.getEmail() != null) {
        attrs.add(attr(EMAIL.getName(), testUser.getEmail()));
      }
      if (testUser.getFirstName() != null) {
        attrs.add(attr(FIRST_NAME.getName(), testUser.getFirstName()));
      }
      if (testUser.getLastName() != null) {
        attrs.add(attr(LAST_NAME.getName(), testUser.getLastName()));
      }
      if (testUser.getCounty() != null) {
        attrs.add(attr(COUNTY.getName(), testUser.getCounty()));
      }
      if (testUser.getPermissions() != null) {
        attrs.add(attr(PERMISSIONS.getName(), testUser.getPermissions()));
      }
      if (testUser.getRoles() != null) {
        attrs.add(attr(ROLES.getName(), testUser.getRoles()));
      }
      if (testUser.getRacfId() != null) {
        attrs.add(attr(RACFID_CUSTOM.getName(), testUser.getRacfId()));
        attrs.add(attr(RACFID_STANDARD.getName(), testUser.getRacfId()));
        attrs.add(attr(RACFID_CUSTOM_2.getName(), testUser.getRacfId()));
      }
      return attrs;
    }

    private static UserType userType(TestUser testUser) {
      UserType userType =
          new UserType()
              .withUsername(testUser.getId())
              .withEnabled(testUser.getEnabled())
              .withUserCreateDate(testUser.getUserCreateDate())
              .withUserLastModifiedDate(testUser.getLastModifiedDate())
              .withUserStatus(testUser.getStatus());

      userType.withAttributes(attrs(testUser));
      return userType;
    }

    private void setSearchUsersByEmailRequestAndResult(
        String email_correct, String email_wrong, TestUser... testUsers) {
      ListUsersRequest request_correct =
          new ListUsersRequest()
              .withUserPoolId(USERPOOL)
              .withLimit(DEFAULT_PAGESIZE)
              .withFilter("email = \"" + email_correct + "\"");

      ListUsersRequest request_wrong =
          new ListUsersRequest()
              .withUserPoolId(USERPOOL)
              .withLimit(DEFAULT_PAGESIZE)
              .withFilter("email = \"" + email_wrong + "\"");

      List<UserType> userTypes =
          Arrays.stream(testUsers)
              .map(TestCognitoServiceFacade::userType)
              .collect(Collectors.toList());

      ListUsersResult result = new ListUsersResult().withUsers(userTypes);
      ListUsersResult result_empty = new ListUsersResult();

      when(cognito.listUsers(request_correct)).thenReturn(result);
      when(cognito.listUsers(request_wrong)).thenReturn(result_empty);
    }

    private void setUpGetUserRequestAndResult(TestUser testUser) {

      AdminGetUserRequest getUserRequest =
          new AdminGetUserRequest().withUsername(testUser.getId()).withUserPoolId(USERPOOL);

      AdminGetUserResult getUserResult = new AdminGetUserResult();
      getUserResult.setUsername(testUser.getId());
      getUserResult.setEnabled(testUser.getEnabled());
      getUserResult.setUserStatus(testUser.getStatus());
      getUserResult.setUserCreateDate(testUser.getUserCreateDate());
      getUserResult.setUserLastModifiedDate(testUser.getLastModifiedDate());

      getUserResult.withUserAttributes(attrs(testUser));

      when(cognito.adminGetUser(getUserRequest)).thenReturn(getUserResult);
    }

    private void setUpGetAbsentUserRequestAndResult() {

      AdminGetUserRequest getUserRequest =
          new AdminGetUserRequest().withUsername(ABSENT_USER_ID).withUserPoolId(USERPOOL);

      when(cognito.adminGetUser(getUserRequest))
          .thenThrow(new UserNotFoundException("user not found"));
    }

    private void setUpGetErrorUserRequestAndResult() {

      AdminGetUserRequest getUserRequest =
          new AdminGetUserRequest().withUsername(ERROR_USER_ID).withUserPoolId(USERPOOL);

      when(cognito.adminGetUser(getUserRequest))
          .thenThrow(new InternalErrorException("internal error"));
    }

    ListUsersRequest setSearchByRacfidRequestAndResult(TestUser testUser){

      ListUsersRequest request =
          composeListUsersRequest(composeToGetFirstPageByAttribute(RACFID_STANDARD, testUser.getRacfId()));

      ListUsersResult result = new ListUsersResult().withUsers(userType(testUser));

      when(cognito.listUsers(request)).thenReturn(result);

      return request;
    }
  }

  private static AttributeType attr(String name, String value) {
    AttributeType attr = new AttributeType();
    attr.setName(name);
    attr.setValue(value);
    return attr;
  }

  private static Date date(int year, int month, int dayOfMonth) {
    return java.sql.Date.valueOf((LocalDate.of(year, month, dayOfMonth)));
  }

  static class TestUser {

    private String id;
    private Boolean enabled;
    private String status;
    private Date userCreateDate;
    private Date lastModifiedDate;
    private String email;
    private String firstName;
    private String lastName;
    private String county;
    private String permissions;
    private String roles;
    private String racfId;

    TestUser(
        String id,
        Boolean enabled,
        String status,
        Date userCreateDate,
        Date lastModifiedDate,
        String email,
        String firstName,
        String lastName,
        String county,
        String permissions,
        String roles,
        String racfId) {
      this.id = id;
      this.enabled = enabled;
      this.status = status;
      this.userCreateDate = userCreateDate;
      this.lastModifiedDate = lastModifiedDate;
      this.email = email;
      this.firstName = firstName;
      this.lastName = lastName;
      this.county = county;
      this.permissions = permissions;
      this.roles = roles;
      this.racfId = racfId;
    }

    public String getId() {
      return id;
    }

    public Boolean getEnabled() {
      return enabled;
    }

    public String getStatus() {
      return status;
    }

    public Date getUserCreateDate() {
      return userCreateDate;
    }

    public Date getLastModifiedDate() {
      return lastModifiedDate;
    }

    public String getEmail() {
      return email;
    }

    public String getFirstName() {
      return firstName;
    }

    public String getLastName() {
      return lastName;
    }

    public String getCounty() {
      return county;
    }

    public String getPermissions() {
      return permissions;
    }

    public String getRoles() {
      return roles;
    }

    public String getRacfId() {
      return racfId;
    }
  }
}
