package gov.ca.cwds.idm.service;

import static gov.ca.cwds.idm.service.cognito.StandardUserAttribute.EMAIL;
import static gov.ca.cwds.idm.service.cognito.StandardUserAttribute.RACFID_STANDARD;
import static gov.ca.cwds.idm.service.cognito.util.CognitoUsersSearchCriteriaUtil.composeToGetFirstPageByEmail;
import static gov.ca.cwds.idm.service.cognito.util.CognitoUtils.getRACFId;
import static gov.ca.cwds.service.messages.MessageCode.DUPLICATE_USERID_FOR_RACFID_IN_CWSCMS;
import static gov.ca.cwds.service.messages.MessageCode.IDM_MAPPING_SCRIPT_ERROR;
import static gov.ca.cwds.service.messages.MessageCode.NOT_AUTHORIZED_TO_ADD_USER_FOR_OTHER_COUNTY;
import static gov.ca.cwds.service.messages.MessageCode.NO_USER_WITH_RACFID_IN_CWSCMS;
import static gov.ca.cwds.service.messages.MessageCode.UNABLE_CREATE_IDM_USER_IN_ES;
import static gov.ca.cwds.service.messages.MessageCode.UNABLE_UPDATE_IDM_USER_IN_ES;
import static gov.ca.cwds.service.messages.MessageCode.USER_WITH_EMAIL_EXISTS_IN_IDM;
import static gov.ca.cwds.util.Utils.toUpperCase;
import static java.util.stream.Collectors.toSet;

import com.amazonaws.services.cognitoidp.model.UserType;
import gov.ca.cwds.PerryProperties;
import gov.ca.cwds.data.persistence.auth.CwsOffice;
import gov.ca.cwds.data.persistence.auth.StaffPerson;
import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserUpdate;
import gov.ca.cwds.idm.dto.UserVerificationResult;
import gov.ca.cwds.idm.dto.UsersPage;
import gov.ca.cwds.idm.dto.UsersSearchCriteria;
import gov.ca.cwds.idm.service.cognito.CognitoServiceFacade;
import gov.ca.cwds.idm.service.cognito.StandardUserAttribute;
import gov.ca.cwds.idm.service.cognito.dto.CognitoUserPage;
import gov.ca.cwds.idm.service.cognito.dto.CognitoUsersSearchCriteria;
import gov.ca.cwds.idm.service.cognito.util.CognitoUsersSearchCriteriaUtil;
import gov.ca.cwds.rest.api.domain.PerryException;
import gov.ca.cwds.rest.api.domain.auth.GovernmentEntityType;
import gov.ca.cwds.service.CwsUserInfoService;
import gov.ca.cwds.service.dto.CwsUserInfo;
import gov.ca.cwds.service.messages.MessageCode;
import gov.ca.cwds.service.messages.MessagesService;
import gov.ca.cwds.service.scripts.IdmMappingScript;
import gov.ca.cwds.util.CurrentAuthenticatedUserUtil;
import gov.ca.cwds.util.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.script.ScriptException;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@Profile("idm")
@SuppressWarnings({"fb-contrib:EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS"})
public class IdmServiceImpl implements IdmService {

  private static final Logger LOGGER = LoggerFactory.getLogger(IdmServiceImpl.class);

  @Autowired private CognitoServiceFacade cognitoServiceFacade;

  @Autowired private CwsUserInfoService cwsUserInfoService;

  @Autowired private PerryProperties configuration;

  @Autowired private MessagesService messages;

  @Autowired private UserLogService userLogService;

  @Autowired private SearchService searchService;

  @Override
  public User findUser(String id) {
    UserType cognitoUser = cognitoServiceFacade.getCognitoUserById(id);
    return enrichCognitoUser(cognitoUser);
  }

  @Override
  @PreAuthorize("@cognitoServiceFacade.getCountyName(#id) == principal.getParameter('county_name')")
  public void updateUser(String id, UserUpdate updateUserDto) {

    UserType existedCognitoUser = cognitoServiceFacade.getCognitoUserById(id);

    boolean updateAttributesExecuted =
        cognitoServiceFacade.updateUserAttributes(id, existedCognitoUser, updateUserDto);
    if(updateAttributesExecuted) {
      updateUserInSearch(id);
    }

    boolean enableExecuted =
        cognitoServiceFacade.changeUserEnabledStatus(id, existedCognitoUser.getEnabled(), updateUserDto.getEnabled());
    if(enableExecuted) {
      updateUserInSearch(id);
    }
  }

  @Override
  public String createUser(User user) {
    String id = cognitoServiceFacade.createUser(user);
    createUserInSearch(id);
    return id;
  }

  @Override
  public UsersPage getUserPage(String paginationToken) {
    CognitoUserPage userPage = cognitoServiceFacade.searchPage(CognitoUsersSearchCriteriaUtil.composeToGetPage(paginationToken));
    List<User> users = enrichCognitoUsersByCws(userPage.getUsers());
    return new UsersPage(users, userPage.getPaginationToken());
  }

  @Override
  public List<User> searchUsers(UsersSearchCriteria criteria) {
    StandardUserAttribute searchAttr = criteria.getSearchAttr();
    Set<String> values = transformSearchValues(criteria.getValues(), searchAttr);

    List<UserType> cognitoUsers = new ArrayList<>();

    for(String value : values) {
      CognitoUsersSearchCriteria cognitoSearchCriteria =
          CognitoUsersSearchCriteriaUtil.composeToGetFirstPageByAttribute(searchAttr, value);
      cognitoUsers.addAll(cognitoServiceFacade.searchAllPages(cognitoSearchCriteria));
    }
    return enrichCognitoUsersByCws(cognitoUsers);
  }

  @Override
  public UserVerificationResult verifyUser(String racfId, String email) {
    CwsUserInfo cwsUser = getCwsUserByRacfId(racfId);
    if (cwsUser == null) {
      return composeNegativeResultWithMessage(NO_USER_WITH_RACFID_IN_CWSCMS, racfId);
    }
    Collection<UserType> cognitoUsers =
        cognitoServiceFacade.searchPage(composeToGetFirstPageByEmail(email)).getUsers();

    if (!CollectionUtils.isEmpty(cognitoUsers)) {
      return composeNegativeResultWithMessage(USER_WITH_EMAIL_EXISTS_IN_IDM, email);
    }
    User user = composeUser(cwsUser, email);
    if (!Objects.equals(CurrentAuthenticatedUserUtil.getCurrentUserCountyName(), user.getCountyName())) {
      return composeNegativeResultWithMessage(NOT_AUTHORIZED_TO_ADD_USER_FOR_OTHER_COUNTY);
    }
    return UserVerificationResult.Builder.anUserVerificationResult()
        .withUser(user)
        .withVerificationPassed().build();
  }

  static Set<String> transformSearchValues(Set<String> values, StandardUserAttribute searchAttr) {
    if(searchAttr == RACFID_STANDARD) {
      values = applyFunctionToValues(values, Utils::toUpperCase);
    } else if (searchAttr == EMAIL) {
      values = applyFunctionToValues(values, Utils::toLowerCase);
    }
    return values;
  }

  private static Set<String> applyFunctionToValues(Set<String> values, Function<String, String> function) {
    return values.stream().map(function::apply).collect(toSet());
  }

  private List<User> enrichCognitoUsersByCws(Collection<UserType> cognitoUsers) {
    Map<String, String> userNameToRacfId = new HashMap<>(cognitoUsers.size());
    for (UserType user : cognitoUsers) {
      userNameToRacfId.put(user.getUsername(), getRACFId(user));
    }
    Map<String, CwsUserInfo> idToCmsUser = cwsUserInfoService.findUsers(userNameToRacfId.values())
        .stream().collect(
            Collectors.toMap(CwsUserInfo::getRacfId, e -> e, (user1, user2) -> {
              LOGGER.warn(messages.get(DUPLICATE_USERID_FOR_RACFID_IN_CWSCMS, user1.getRacfId()));
              return user1;
            }));
    IdmMappingScript mapping = configuration.getIdentityManager().getIdmMapping();
    return cognitoUsers
        .stream()
        .map( e -> { try {
          return mapping.map(e, idToCmsUser.get(userNameToRacfId.get(e.getUsername())));
        } catch (ScriptException ex) {
          LOGGER.error(messages.get(IDM_MAPPING_SCRIPT_ERROR));
          throw new PerryException(ex.getMessage(), ex);
        }}).collect(Collectors.toList());
  }

  private void updateUserInSearch(String id) {
    try {
      User updatedUser = findUser(id);
      searchService.updateUser(updatedUser);
    } catch (Exception e) {
      String msg = messages.get(UNABLE_UPDATE_IDM_USER_IN_ES, id);
      LOGGER.error(msg, e);
      userLogService.logUpdate(id);
    }
  }

  private void createUserInSearch(String id) {
    try {
      User createdUser = findUser(id);
      searchService.createUser(createdUser);
    } catch (Exception e) {
      String msg = messages.get(UNABLE_CREATE_IDM_USER_IN_ES, id);
      LOGGER.error(msg, e);
      userLogService.logCreate(id);
    }
  }

  private UserVerificationResult composeNegativeResultWithMessage(
      MessageCode errorCode, Object... params) {
    String message = messages.get(errorCode, params);
    LOGGER.info(message);
    return UserVerificationResult.Builder.anUserVerificationResult()
        .withVerificationFailed(errorCode.getValue(), message)
        .build();
  }

  private User composeUser(CwsUserInfo cwsUser, String email) {
    User user = new User();
    user.setEmail(email);
    user.setRacfid(cwsUser.getRacfId());
    enrichDataFromCwsOffice(cwsUser.getCwsOffice(), user);
    enrichDataFromStaffPerson(cwsUser.getStaffPerson(), user);
    return user;
  }

  private void enrichDataFromStaffPerson(StaffPerson staffPerson, final User user) {
    if (staffPerson != null) {
      user.setFirstName(staffPerson.getFirstName());
      user.setLastName(staffPerson.getLastName());
      user.setEndDate(staffPerson.getEndDate());
      user.setStartDate(staffPerson.getStartDate());
    }
  }

  private void enrichDataFromCwsOffice(CwsOffice office, final User user) {
    if (office != null) {
      user.setOffice(office.getCwsOfficeName());
      Optional.ofNullable(office.getPrimaryPhoneNumber())
          .ifPresent(e -> user.setPhoneNumber(e.toString()));
      Optional.ofNullable(office.getPrimaryPhoneExtensionNumber())
          .ifPresent(e -> user.setPhoneExtensionNumber(e.toString()));
      Optional.ofNullable(office.getGovernmentEntityType())
          .ifPresent(
              x -> user.setCountyName((GovernmentEntityType.findBySysId(x)).getDescription()));
    }
  }

  private User enrichCognitoUser(UserType cognitoUser) {
    String racfId = getRACFId(cognitoUser);
    CwsUserInfo cwsUser = getCwsUserByRacfId(racfId);
    try {
      return configuration.getIdentityManager().getIdmMapping().map(cognitoUser, cwsUser);
    } catch (ScriptException e) {
      LOGGER.error(messages.get(IDM_MAPPING_SCRIPT_ERROR));
      throw new PerryException(e.getMessage(), e);
    }
  }

  private CwsUserInfo getCwsUserByRacfId(String racfId) {
    CwsUserInfo cwsUser = null;
    if (racfId != null) {
      List<CwsUserInfo> users =
          cwsUserInfoService.findUsers(Collections.singletonList(toUpperCase(racfId)));
      if (!CollectionUtils.isEmpty(users)) {
        cwsUser = users.get(0);
      }
    }
    return cwsUser;
  }

  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }
}
