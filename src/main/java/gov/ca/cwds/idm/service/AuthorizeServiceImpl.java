package gov.ca.cwds.idm.service;

import static gov.ca.cwds.config.api.idm.Roles.isCalsAdmin;
import static gov.ca.cwds.config.api.idm.Roles.isCalsExternalWorker;
import static gov.ca.cwds.config.api.idm.Roles.isCountyAdmin;
import static gov.ca.cwds.config.api.idm.Roles.isMostlyCountyAdmin;
import static gov.ca.cwds.config.api.idm.Roles.isMostlyOfficeAdmin;
import static gov.ca.cwds.config.api.idm.Roles.isMostlyStateAdmin;
import static gov.ca.cwds.config.api.idm.Roles.isStateAdmin;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getAdminOfficeIds;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCountyName;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCurrentUser;

import com.amazonaws.services.cognitoidp.model.UserType;
import gov.ca.cwds.UniversalUserToken;
import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.service.cognito.AuthorizeService;
import gov.ca.cwds.idm.service.cognito.CognitoServiceFacade;
import java.util.Set;
import java.util.function.BiPredicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service(value = "authorize")
@Profile("idm")
public class AuthorizeServiceImpl implements AuthorizeService {
  private CognitoServiceFacade cognitoServiceFacade;

  private MappingService mappingService;

  @Override
  public boolean canViewUser(User user) {
    return canViewUser(user, getCurrentUser());
  }

  boolean canViewUser(User user, UniversalUserToken admin) {
    return authorizeByUserAndAdmin(user, admin,
        AuthorizeServiceImpl::authorizeFindUserByOfficeAdmin);
  }

  private static boolean authorizeFindUserByOfficeAdmin(User user, UniversalUserToken admin) {
    return areInSameCounty(user, admin)
        && !userIsStateAdminFromOtherOffice(user, admin)
        && !userIsCountyAdminFromOtherOffice(user, admin);
  }

  private static boolean userIsStateAdminFromOtherOffice(User user, UniversalUserToken admin) {
    return isStateAdmin(user) && !areInSameOffice(user, admin);
  }

  private static boolean userIsCountyAdminFromOtherOffice(User user, UniversalUserToken admin) {
    return isCountyAdmin(user) && !areInSameOffice(user, admin);
  }

  @Override
  public boolean canCreateUser(User user) {
    return authorizeByUser(user);
  }

  @Override
  public boolean canUpdateUser(String userId) {
    return authorizeByUserId(userId);
  }

  @Override
  public boolean canResendInvitationMessage(String userId) {
    return authorizeByUserId(userId);
  }

  private boolean authorizeByUserId(String userId) {
    return authorizeByUser(getUserFromUserId(userId));
  }

  private User getUserFromUserId(String userId) {
    UserType cognitoUser = cognitoServiceFacade.getCognitoUserById(userId);
    UniversalUserToken admin = getCurrentUser();
    User user;

    if (isMostlyOfficeAdmin(admin)) {
      user = mappingService.toUser(cognitoUser);
    } else {
      user = mappingService.toUserWithoutCwsData(cognitoUser);
    }
    return user;
  }

  private boolean authorizeByUser(User user) {
    UniversalUserToken admin = getCurrentUser();
    return authorizeByUserAndAdmin(user, admin);
  }

  private boolean authorizeByUserAndAdmin(User user, UniversalUserToken admin,
      BiPredicate<User, UniversalUserToken> officeAdminStrategy) {

    if (isMostlyStateAdmin(admin) || isAuthorizedAsCalsAdmin(user, admin)) {
      return true;

    } else if (isMostlyCountyAdmin(admin)) {
      return areInSameCounty(user, admin);

    } else if (isMostlyOfficeAdmin(admin)) {
      return officeAdminStrategy.test(user, admin);
    }
    return false;
  }

  boolean authorizeByUserAndAdmin(User user, UniversalUserToken admin) {
    return authorizeByUserAndAdmin(user, admin, AuthorizeServiceImpl::authorizeByOfficeAdmin);
  }

  private static boolean authorizeByOfficeAdmin(User user, UniversalUserToken admin) {
    return areInSameOffice(user, admin);
  }

  private static boolean areInSameCounty(User user, UniversalUserToken admin) {
    String userCountyName = user.getCountyName();
    String adminCountyName = getCountyName(admin);
    return areNotNullAndEquals(userCountyName, adminCountyName);
  }

  private static boolean areInSameOffice(User user, UniversalUserToken admin) {
    String userOfficeId = user.getOfficeId();
    Set<String> adminOfficeIds = getAdminOfficeIds(admin);
    return areNotNullAndContains(adminOfficeIds, userOfficeId);
  }

  private boolean isAuthorizedAsCalsAdmin(User user, UniversalUserToken admin) {
    return isCalsAdmin(admin) && isCalsExternalWorker(user);
  }

  static boolean areNotNullAndEquals(String str1, String str2) {
    return str1 != null && str1.equals(str2);
  }

  static boolean areNotNullAndContains(Set<String> set, String str) {
    return str != null && set != null && set.contains(str);
  }

  @Autowired
  public void setCognitoServiceFacade(CognitoServiceFacade cognitoServiceFacade) {
    this.cognitoServiceFacade = cognitoServiceFacade;
  }

  @Autowired
  public void setMappingService(MappingService mappingService) {
    this.mappingService = mappingService;
  }
}
