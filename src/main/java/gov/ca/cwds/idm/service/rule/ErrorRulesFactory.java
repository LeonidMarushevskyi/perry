package gov.ca.cwds.idm.service.rule;

import static gov.ca.cwds.config.api.idm.Roles.CALS_EXTERNAL_WORKER;
import static gov.ca.cwds.config.api.idm.Roles.STATE_ADMIN;
import static gov.ca.cwds.idm.service.authorization.UserRolesService.getStrongestAdminRole;
import static gov.ca.cwds.idm.service.authorization.UserRolesService.isCalsExternalWorker;
import static gov.ca.cwds.idm.service.authorization.UserRolesService.isCountyAdmin;
import static gov.ca.cwds.idm.service.authorization.UserRolesService.isStateAdmin;
import static gov.ca.cwds.idm.service.authorization.UserRolesService.isSuperAdmin;
import static gov.ca.cwds.idm.service.authorization.UserRolesService.isUserWithMainRole;
import static gov.ca.cwds.service.messages.MessageCode.ADMIN_CANNOT_UPDATE_HIMSELF;
import static gov.ca.cwds.service.messages.MessageCode.CANNOT_EDIT_ROLES_OF_CALS_EXTERNAL_WORKER;
import static gov.ca.cwds.service.messages.MessageCode.STATE_ADMIN_ROLES_CANNOT_BE_EDITED;
import static gov.ca.cwds.service.messages.MessageCode.UNABLE_TO_CREATE_USER_WITH_UNALLOWED_ROLES;
import static gov.ca.cwds.service.messages.MessageCode.UNABLE_UPDATE_UNALLOWED_ROLES;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCurrentUser;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCurrentUserCountyName;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCurrentUserName;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCurrentUserOfficeIds;
import static gov.ca.cwds.util.Utils.toCommaDelimitedString;
import static java.util.Arrays.asList;

import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserUpdate;
import gov.ca.cwds.idm.exception.AdminAuthorizationException;
import gov.ca.cwds.idm.exception.UserValidationException;
import gov.ca.cwds.idm.service.exception.ExceptionFactory;
import gov.ca.cwds.service.messages.MessageCode;
import java.util.List;
import java.util.Set;

public class ErrorRulesFactory {

  private ExceptionFactory exceptionFactory;
  private final User user;
  private final UserUpdate userUpdate;

  public ErrorRulesFactory(User user, UserUpdate userUpdate) {
    this.user = user;
    this.userUpdate = userUpdate;
  }

  public final ErrorRule userAndAdminAreNotTheSameUser() {
    return new ErrorRule(
        () -> userAndAdminAreTheSameUser(),
        () -> createAuthorizationException(ADMIN_CANNOT_UPDATE_HIMSELF)
    );
  }

  public final ErrorRule userIsNotSuperAdmin(MessageCode errorCode) {
    return new ErrorRule(
        () -> isUserSuperAdmin(),
        () -> createAuthorizationException(
            errorCode, getStrongestAdminRole(getCurrentUser()), user.getId()));
  }

  public final ErrorRule userIsNotStateAdmin(MessageCode errorCode) {
    return new ErrorRule(
        () -> isUsersStateAdmin(),
        () -> createAuthorizationException(errorCode, user.getId()));
  }

  public final ErrorRule userIsNotCountyAdmin(MessageCode errorCode) {
    return new ErrorRule(
        () -> isUserCountyAdmin(),
        () -> createAuthorizationException(errorCode, user.getId()));
  }

  public final ErrorRule userIsNotCalsExternalWorker(MessageCode errorCode) {
    return new ErrorRule(
        () -> isUserCalsExternalWorker(),
        () -> createAuthorizationException(errorCode, user.getId()));
  }

  public final ErrorRule adminAndUserAreInTheSameCounty(MessageCode errorCode, String... args) {
    return new ErrorRule(
        () -> areAdminAndUserNotInTheSameCounty(),
        () -> createAuthorizationException(errorCode, args));
  }

  public final ErrorRule adminAndUserAreInTheSameOffice(MessageCode errorCode) {
    return new ErrorRule(
        () -> adminAndUserAreInDifferentOffices(),
        () -> createAuthorizationException(errorCode, user.getId()));
  }

  public final ErrorRule stateAdminUserRolesCanNotBeChanged() {
    return rolesAreNotEdited(STATE_ADMIN, STATE_ADMIN_ROLES_CANNOT_BE_EDITED);
  }

  public final ErrorRule calsExternalWorkerRolesCanNotBeChanged() {
    return rolesAreNotEdited(CALS_EXTERNAL_WORKER, CANNOT_EDIT_ROLES_OF_CALS_EXTERNAL_WORKER);
  }

  public final ErrorRule createdUserRolesMayBeOnly(String... allowedRoles) {
    return userNewRolesMayBeOnly(
        UNABLE_TO_CREATE_USER_WITH_UNALLOWED_ROLES, user.getRoles(), allowedRoles);
  }

  public final ErrorRule updatedUserRolesMayBeOnly(List<String> allowedRolesList) {
    return userNewRolesMayBeOnly(
        UNABLE_UPDATE_UNALLOWED_ROLES, userUpdate.getRoles(), allowedRolesList);
  }

  private ErrorRule userNewRolesMayBeOnly(MessageCode messageCode, Set<String> newRoles, List<String> allowedRolesList) {

    return new ErrorRule(
        () -> newRoles != null && (!allowedRolesList.containsAll(newRoles)),
        () -> createValidationException(
                messageCode,
                toCommaDelimitedString(newRoles),
                toCommaDelimitedString(allowedRolesList)));
  }

  private ErrorRule userNewRolesMayBeOnly(MessageCode messageCode, Set<String> newRoles, String... allowedRoles) {
    return userNewRolesMayBeOnly(messageCode, newRoles, asList(allowedRoles));
  }

  private ErrorRule rolesAreNotEdited(String userMainRole,
      MessageCode errorCode) {

    return new ErrorRule(
        () -> userUpdate.getRoles() != null
            && isUserWithMainRole(user, userMainRole)
            && !user.getRoles().equals(userUpdate.getRoles()),
        () -> createAuthorizationException(errorCode, user.getId()));
  }

  private boolean areAdminAndUserNotInTheSameCounty() {
    String userCountyName = user.getCountyName();
    String adminCountyName = getCurrentUserCountyName();
    return userCountyName == null || !userCountyName.equals(adminCountyName);
  }

  private boolean adminAndUserAreInDifferentOffices() {
    String userOfficeId = user.getOfficeId();
    Set<String> adminOfficeIds = getCurrentUserOfficeIds();
    return adminOfficeIds == null || userOfficeId == null || !adminOfficeIds.contains(userOfficeId);
  }

  private boolean isUserCalsExternalWorker() {
    return isCalsExternalWorker(user);
  }

  private boolean isUserCountyAdmin() {
    return isCountyAdmin(user);
  }

  private boolean isUserSuperAdmin() {
    return isSuperAdmin(user);
  }

  private boolean isUsersStateAdmin() {
    return isStateAdmin(user);
  }

  private boolean userAndAdminAreTheSameUser() {
    return user.getId().equals(getCurrentUserName());
  }

  private UserValidationException createValidationException(MessageCode messageCode,
      String... args) {
    return exceptionFactory.createValidationException(messageCode, args);
  }

  private AdminAuthorizationException createAuthorizationException(MessageCode messageCode,
      String... args) {
    return exceptionFactory.createAuthorizationException(messageCode, args);
  }

  public void setExceptionFactory(ExceptionFactory exceptionFactory) {
    this.exceptionFactory = exceptionFactory;
  }
}
