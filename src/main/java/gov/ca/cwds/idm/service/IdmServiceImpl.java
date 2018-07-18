package gov.ca.cwds.idm.service;

import com.amazonaws.services.cognitoidp.model.UserType;
import gov.ca.cwds.PerryProperties;
import gov.ca.cwds.data.persistence.auth.CwsOffice;
import gov.ca.cwds.data.persistence.auth.StaffPerson;
import gov.ca.cwds.idm.dto.UsersSearchCriteria;
import gov.ca.cwds.idm.service.cognito.dto.CognitoUserPage;
import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserUpdate;
import gov.ca.cwds.idm.dto.UserVerificationResult;
import gov.ca.cwds.idm.dto.UsersPage;
import gov.ca.cwds.idm.service.cognito.dto.CognitoUsersSearchCriteria;
import gov.ca.cwds.idm.service.cognito.CognitoServiceFacade;
import gov.ca.cwds.idm.service.cognito.CognitoUtils;
import gov.ca.cwds.idm.service.cognito.util.CognitoUsersSearchCriteriaUtil;
import gov.ca.cwds.rest.api.domain.PerryException;
import gov.ca.cwds.rest.api.domain.auth.GovernmentEntityType;
import gov.ca.cwds.service.CwsUserInfoService;
import gov.ca.cwds.service.dto.CwsUserInfo;
import gov.ca.cwds.service.messages.MessageCode;
import gov.ca.cwds.service.messages.MessagesService;
import gov.ca.cwds.service.scripts.IdmMappingScript;
import gov.ca.cwds.util.CurrentAuthenticatedUserUtil;
import java.util.ArrayList;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.script.ScriptException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static gov.ca.cwds.idm.service.cognito.CognitoUtils.RACFID_ATTR_NAME_CUSTOM;
import static gov.ca.cwds.idm.service.cognito.CognitoUtils.EMAIL_ATTR_NAME;
import static gov.ca.cwds.service.messages.MessageCode.IDM_MAPPING_SCRIPT_ERROR;
import static gov.ca.cwds.service.messages.MessageCode.NOT_AUTHORIZED_TO_ADD_USER_FOR_OTHER_COUNTY;
import static gov.ca.cwds.service.messages.MessageCode.NO_USER_WITH_RACFID_IN_CWSCMS;
import static gov.ca.cwds.service.messages.MessageCode.USER_WITH_EMAIL_EXISTS_IN_IDM;
import static gov.ca.cwds.util.Utils.toUpperCase;

@Service
@Profile("idm")
public class IdmServiceImpl implements IdmService {

  private static final Logger LOGGER = LoggerFactory.getLogger(IdmServiceImpl.class);

  @Autowired private CognitoServiceFacade cognitoServiceFacade;

  @Autowired private CwsUserInfoService cwsUserInfoService;

  @Autowired private PerryProperties configuration;

  @Autowired private MessagesService messages;

  @Override
  public User findUser(String id) {
    UserType cognitoUser = cognitoServiceFacade.getById(id);
    return enrichCognitoUser(cognitoUser);
  }

  @Override
  @PreAuthorize("@cognitoServiceFacade.getCountyName(#id) == principal.getParameter('county_name')")
  public void updateUser(String id, UserUpdate updateUserDto) {
    cognitoServiceFacade.updateUser(id, updateUserDto);
  }

  @Override
  public String createUser(User user) {
    return cognitoServiceFacade.createUser(user);
  }

  @Override
  public UsersPage getUserPage(String paginationToken) {
    CognitoUserPage userPage = cognitoServiceFacade.search(CognitoUsersSearchCriteriaUtil.composeToGetPage(paginationToken));
    Collection<UserType> cognitoUsers = userPage.getUsers();
    Map<String, String> userNameToRacfId = new HashMap<>(cognitoUsers.size());
    for (UserType user : cognitoUsers) {
      userNameToRacfId.put(user.getUsername(), getRACFId(user));
    }
    Map<String, CwsUserInfo> idToCmsUser = cwsUserInfoService.findUsers(userNameToRacfId.values())
            .stream().collect(
                    Collectors.toMap(CwsUserInfo::getRacfId, e -> e, (user1, user2) -> {
                      LOGGER.warn("UserAuthorization - duplicate UserId for RACFid: {}", user1.getRacfId());
                      return user1;
                    }));
    IdmMappingScript mapping = configuration.getIdentityManager().getIdmMapping();
    List<User> users = cognitoUsers
            .stream()
            .map( e -> { try {
              return mapping.map(e, idToCmsUser.get(userNameToRacfId.get(e.getUsername())));
            } catch (ScriptException ex) {
              LOGGER.error("Error running the IdmMappingScript");
              throw new PerryException(ex.getMessage(), ex);
            }}).collect(Collectors.toList());
    return new UsersPage(users, userPage.getPaginationToken());
  }

  @Override
  public List<User> searchUsers(UsersSearchCriteria usersSearchCriteria) {
    List<User> result = new ArrayList<>();
    List<UserType> cognitoUsers = new ArrayList<>();

    Set<String> racfids = usersSearchCriteria.getRacfids();
    for(String racfid : racfids) {
      CognitoUsersSearchCriteria searchCriteria = CognitoUsersSearchCriteriaUtil.composeToGetByRacfid(racfid);
      cognitoUsers.addAll(cognitoServiceFacade.searchAllPages(searchCriteria));
    }

    return result;
  }

  @Override
  public UserVerificationResult verifyUser(String racfId, String email) {
    CwsUserInfo cwsUser = getCwsUserByRacfId(racfId);
    if (cwsUser == null) {
      return composeNegativeResultWithMessage(NO_USER_WITH_RACFID_IN_CWSCMS, racfId);
    }
    Collection<UserType> cognitoUsers =
        cognitoServiceFacade.search(
            CognitoUsersSearchCriteria.SearchParameterBuilder.aSearchParameters()
                .withAttr(EMAIL_ATTR_NAME, email).build()).getUsers();

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

  static String getRACFId(UserType user) {
    return CognitoUtils.getAttributeValue(user, RACFID_ATTR_NAME_CUSTOM);
  }
}
