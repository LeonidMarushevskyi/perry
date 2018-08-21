package gov.ca.cwds.idm.service.cognito;

import static gov.ca.cwds.idm.service.cognito.CustomUserAttribute.COUNTY;
import static gov.ca.cwds.idm.service.cognito.CustomUserAttribute.OFFICE;
import static gov.ca.cwds.idm.service.cognito.CustomUserAttribute.PERMISSIONS;
import static gov.ca.cwds.idm.service.cognito.CustomUserAttribute.RACFID_CUSTOM;
import static gov.ca.cwds.idm.service.cognito.CustomUserAttribute.RACFID_CUSTOM_2;
import static gov.ca.cwds.idm.service.cognito.CustomUserAttribute.ROLES;
import static gov.ca.cwds.idm.service.cognito.StandardUserAttribute.EMAIL;
import static gov.ca.cwds.idm.service.cognito.StandardUserAttribute.EMAIL_VERIFIED;
import static gov.ca.cwds.idm.service.cognito.StandardUserAttribute.FIRST_NAME;
import static gov.ca.cwds.idm.service.cognito.StandardUserAttribute.LAST_NAME;
import static gov.ca.cwds.idm.service.cognito.StandardUserAttribute.PHONE_NUMBER;
import static gov.ca.cwds.idm.service.cognito.StandardUserAttribute.RACFID_STANDARD;
import static gov.ca.cwds.util.Utils.toSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.AdminCreateUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminGetUserResult;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.cognitoidp.model.ListUsersRequest;
import com.amazonaws.services.cognitoidp.model.ListUsersResult;
import com.amazonaws.services.cognitoidp.model.UserNotFoundException;
import com.amazonaws.services.cognitoidp.model.UserType;
import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.service.cognito.dto.CognitoUsersSearchCriteria;
import gov.ca.cwds.rest.api.domain.PerryException;
import gov.ca.cwds.rest.api.domain.UserNotFoundPerryException;
import gov.ca.cwds.service.messages.MessagesService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

public class CognitoServiceFacadeTest {

  private CognitoServiceFacade facade;

  private AWSCognitoIdentityProvider identityProvider = mock(AWSCognitoIdentityProvider.class);

  private MessagesService messagesService = mock(MessagesService.class);

  @Before
  public void before() {
    CognitoProperties properties = new CognitoProperties();
    properties.setIamAccessKeyId("iamAccessKeyId");
    properties.setIamSecretKey("iamSecretKey");
    properties.setUserpool("userpool");
    properties.setRegion("us-east-2");

    facade = new CognitoServiceFacade();
    facade.setProperties(properties);
    facade.setIdentityProvider(identityProvider);
    facade.setMessagesService(messagesService);
  }

  @Test
  public void testInit() {
    facade.init();
    AWSCognitoIdentityProvider identityProvider = facade.getIdentityProvider();
    assertThat(identityProvider, is(notNullValue()));
  }

  @Test
  public void testGetById() {
    AdminGetUserResult mockResult = new AdminGetUserResult();
    mockResult.setUsername("id");
    mockResult.setEnabled(Boolean.TRUE);
    mockResult.setUserStatus("userstatus");

    List<AttributeType> mockAttrs = new ArrayList<>();
    AttributeType mockAttr = new AttributeType();
    mockAttr.setName("attrName");
    mockAttr.setValue("attrValue");
    mockAttrs.add(mockAttr);
    mockResult.setUserAttributes(mockAttrs);

    mockResult.setUserCreateDate(new Date(1000));
    mockResult.setUserLastModifiedDate(new Date(2000));

    when(identityProvider.adminGetUser(any(AdminGetUserRequest.class)))
        .thenReturn(mockResult);

    UserType UserType = facade.getCognitoUserById("id");
    AdminGetUserRequest expectedRequest = new AdminGetUserRequest().withUsername("id")
        .withUserPoolId("userpool");
    verify(identityProvider, times(1)).adminGetUser(expectedRequest);
    assertThat(UserType.getUsername(), is("id"));
    assertThat(UserType.getEnabled(), is(Boolean.TRUE));
    assertThat(UserType.getUserStatus(), is("userstatus"));
    assertThat(UserType.getUserCreateDate(), is(new Date(1000)));
    assertThat(UserType.getUserLastModifiedDate(), is(new Date(2000)));

    List<AttributeType> attrs = UserType.getAttributes();
    assertThat(attrs, is(notNullValue()));
    assertThat(attrs, hasSize(1));
    AttributeType attr = attrs.get(0);
    assertThat(attr.getName(), is("attrName"));
    assertThat(attr.getValue(), is("attrValue"));
  }

  @Test(expected = UserNotFoundPerryException.class)
  public void testGetByIdUserNotFoundException() {
    when(identityProvider.adminGetUser(any(AdminGetUserRequest.class)))
        .thenThrow(new UserNotFoundException("user not found"));
    facade.getCognitoUserById("id");
  }

  @Test(expected = PerryException.class)
  public void testGetByIdException() {
    when(identityProvider.adminGetUser(any(AdminGetUserRequest.class)))
        .thenThrow(new RuntimeException());
    facade.getCognitoUserById("id");
  }

  @Test
  public void testCreateAdminCreateUserRequest() {
    User user = user();
    user.setEmail("GONZALES@Gmail.com");

    AdminCreateUserRequest request = facade.createAdminCreateUserRequest(user);

    assertThat(request.getUsername(), is("gonzales@gmail.com"));

    assertThat(request.getUserPoolId(), is("userpool"));
    assertThat(request.getDesiredDeliveryMediums(), is(Arrays.asList("EMAIL")));

    List<AttributeType> attrs = request.getUserAttributes();
    assertThat(attrs.isEmpty(), is(false));
    assertThat(attrs.size(), is(12));

    Map<String, String> attrMap = attrMap(attrs);

    assertAttr(attrMap, EMAIL, "gonzales@gmail.com");
    assertAttr(attrMap, FIRST_NAME, "Garcia");
    assertAttr(attrMap, LAST_NAME, "Gonzales");
    assertAttr(attrMap, COUNTY, "Madera");
    assertAttr(attrMap, OFFICE, "River Office");
    assertAttr(attrMap, PHONE_NUMBER, "+19161111111");
    assertAttr(attrMap, RACFID_CUSTOM, "RUBBLBA");
    assertAttr(attrMap, RACFID_STANDARD, "RUBBLBA");
    assertAttr(attrMap, RACFID_CUSTOM_2, "RUBBLBA");
    assertAttr(attrMap, EMAIL_VERIFIED, "True");
    assertAttr(attrMap, PERMISSIONS, "RFA-rollout:Hotline-rollout");
    assertAttr(attrMap, ROLES, "CWS-admin:CWS-worker");
  }

  @Test
  public void testRacifIdLowerCase() {
    User user = user();
    user.setRacfid("rubblba ");

    AdminCreateUserRequest request = facade.createAdminCreateUserRequest(user);
    Map<String, String> attrMap = attrMap(request.getUserAttributes());
    assertAttr(attrMap, RACFID_CUSTOM, "RUBBLBA");
    assertAttr(attrMap, RACFID_STANDARD, "RUBBLBA");
    assertAttr(attrMap, RACFID_CUSTOM_2, "RUBBLBA");
  }

  @Test
  public void testSearchAllPages() {

    UserType userType0 = userType("user0");
    UserType userType1 = userType("user1");
    UserType userType2 = userType("user2");
    UserType userType3 = userType("user3");
    UserType userType4 = userType("user4");

    CognitoUsersSearchCriteria searchCriteria = new CognitoUsersSearchCriteria();
    searchCriteria.setSearchAttr(EMAIL, "searchPage@all.email");
    searchCriteria.setPageSize(2);

    ListUsersRequest request0 =
        setListUsersRequestAndResponse(searchCriteria, null, "1", userType0, userType1);

    ListUsersRequest request1 =
        setListUsersRequestAndResponse(searchCriteria, "1", "2", userType2, userType3);

    ListUsersRequest request2 =
        setListUsersRequestAndResponse(searchCriteria, "2", null, userType4);

    List<UserType> userTypes = facade.searchAllPages(searchCriteria);

    verify(identityProvider, times(1)).listUsers(request0);
    verify(identityProvider, times(1)).listUsers(request1);
    verify(identityProvider, times(1)).listUsers(request2);

    InOrder inOrder = inOrder(identityProvider);
    inOrder.verify(identityProvider).listUsers(request0);
    inOrder.verify(identityProvider).listUsers(request1);
    inOrder.verify(identityProvider).listUsers(request2);

    assertThat(userTypes, hasSize(5));
    assertThat(userTypes.get(0).getUsername(), is("user0"));
    assertThat(userTypes.get(1).getUsername(), is("user1"));
    assertThat(userTypes.get(2).getUsername(), is("user2"));
    assertThat(userTypes.get(3).getUsername(), is("user3"));
    assertThat(userTypes.get(4).getUsername(), is("user4"));
  }

  @Test
  public void testComposeListUsersRequest(){
    CognitoUsersSearchCriteria criteria = new CognitoUsersSearchCriteria();
    criteria.setSearchAttr(RACFID_STANDARD, "ABC");
    ListUsersRequest request = facade.composeListUsersRequest(criteria);
    assertThat(request.getFilter(), is(RACFID_STANDARD.getName() + " = \"ABC\""));
  }

  private ListUsersRequest setListUsersRequestAndResponse(
      CognitoUsersSearchCriteria searchCriteria, String requestPaginationToken,
      String responsePaginationToken, UserType... userTypes) {

    CognitoUsersSearchCriteria searchCriteria1 = new CognitoUsersSearchCriteria(searchCriteria);
    searchCriteria1.setPaginationToken(requestPaginationToken);
    ListUsersRequest request = facade.composeListUsersRequest(searchCriteria1);
    ListUsersResult listUsersResult =
        new ListUsersResult().withUsers(userTypes).withPaginationToken(responsePaginationToken);
    when(identityProvider.listUsers(request)).thenReturn(listUsersResult);
    return request;
  }

  private User user() {
    User user = new User();
    user.setEmail("gonzales@gmail.com");
    user.setFirstName("Garcia");
    user.setLastName("Gonzales");
    user.setCountyName("Madera");
    user.setRacfid("RUBBLBA ");
    user.setOffice("River Office");
    user.setPhoneNumber("+19161111111");
    user.setPermissions(toSet("RFA-rollout", "Hotline-rollout"));
    user.setRoles(toSet("CWS-admin", "CWS-worker"));
    return user;
  }

  private UserType userType(String name) {
    UserType userType = new UserType();
    userType.setUsername(name);
    return userType;
  }

  private static Map<String, String> attrMap(List<AttributeType> attrs) {
    return attrs.stream().collect(Collectors.toMap(AttributeType::getName, AttributeType::getValue));
  }

  private static void assertAttr(Map<String, String> attrMap, UserAttribute attr, String value) {
    assertTrue(attrMap.containsKey(attr.getName()));
    assertThat(attrMap.get(attr.getName()), is(value));
  }
}
