package gov.ca.cwds.idm.service.authorization;

import static gov.ca.cwds.service.messages.MessageCode.ADMIN_CANNOT_UPDATE_HIMSELF;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCurrentUserName;

import gov.ca.cwds.idm.dto.UpdateProperty;
import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserUpdate;
import gov.ca.cwds.idm.exception.AdminAuthorizationException;
import gov.ca.cwds.idm.service.exception.ExceptionFactory;
import gov.ca.cwds.idm.service.role.implementor.AbstractAdminActionsAuthorizer;
import gov.ca.cwds.idm.service.role.implementor.AdminRoleImplementorFactory;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service("authorizationService")
@Profile("idm")
public class AuthorizationServiceImpl implements AuthorizationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationServiceImpl.class);

  private static final String ROLES_EDITING = "roles editing";
  private static final String PERMISSIONS_EDITING = "permissions editing";
  private static final String USER_UPDATE = "user update";

  private AdminRoleImplementorFactory adminRoleImplementorFactory;

  private ExceptionFactory exceptionFactory;

  @Override
  public void checkCanViewUser(User user) {
    getAdminActionsAuthorizer(user).checkCanViewUser();
  }

  @Override
  public void checkCanCreateUser(User user) {
    getAdminActionsAuthorizer(user).checkCanCreateUser();
  }

  void checkCanUpdateUser(User user) {
    if (user.getId().equals(getCurrentUserName())) {
      throw exceptionFactory.createAuthorizationException(ADMIN_CANNOT_UPDATE_HIMSELF);
    }
    getAdminActionsAuthorizer(user).checkCanUpdateUser();
  }

  @Override
  public boolean canUpdateUser(User user) {
    return canAuthorizeOperation(user, this::checkCanUpdateUser, USER_UPDATE);
  }

  @Override
  public void checkCanUpdateUser(User user, UserUpdate updateUserDto) {
    if (user.getId().equals(getCurrentUserName())) {
      throw exceptionFactory.createAuthorizationException(ADMIN_CANNOT_UPDATE_HIMSELF);
    }
    checkCanUpdateUser(user);
    authorizeRolesUpdate(user, updateUserDto);
    authorizePermissionsUpdate(user, updateUserDto);
  }

  private void authorizeRolesUpdate(User user, UserUpdate updateUserDto) {
    if (updateUserDto.getRoles() == null) {
      return;
    }
    if (wasRolesActuallyEdited(user, updateUserDto)) {
      checkCanEditRoles(user);
    }
  }

  private void authorizePermissionsUpdate(User user, UserUpdate updateUserDto) {
    if (updateUserDto.getPermissions() == null) {
      return;
    }
    if (wasPermissionsActuallyEdited(user, updateUserDto)) {
      checkCanEditPermissions(user);
    }
  }

  private boolean wasRolesActuallyEdited(User user, UserUpdate updateUserDto) {
    return wasActuallyEdited(user.getRoles(), updateUserDto.getRoles());
  }

  private boolean wasPermissionsActuallyEdited(User user, UserUpdate updateUserDto) {
    return wasActuallyEdited(user.getPermissions(), updateUserDto.getPermissions());
  }

  private boolean wasActuallyEdited(Set<String> oldSet, UpdateProperty<Set<String>> newSet) {
    return newSet.isSet() && !CollectionUtils.isEqualCollection(oldSet, newSet.get());
  }

  private void checkCanEditRoles(User user) {
    getAdminActionsAuthorizer(user).checkCanEditRoles();
  }

  @Override
  public boolean canEditRoles(User user) {
    return canAuthorizeOperation(user, this::checkCanEditRoles, ROLES_EDITING);
  }

  private void checkCanEditPermissions(User user) {
    getAdminActionsAuthorizer(user).checkCanEditPermissions();
  }

  @Override
  public boolean canEditPermissions(User user) {
    return canAuthorizeOperation(user, this::checkCanEditPermissions, PERMISSIONS_EDITING);
  }

  private AdminActionsAuthorizer getAdminActionsAuthorizer(User user) {
    AbstractAdminActionsAuthorizer authorizer = adminRoleImplementorFactory
        .getAdminActionsAuthorizer(user);
    authorizer.setExceptionFactory(exceptionFactory);
    return authorizer;
  }

  @SuppressWarnings({"squid:S1166", "fb-contrib:EXS_EXCEPTION_SOFTENING_RETURN_FALSE"})
  //squid:S1166: exceptions stack trace can be omitted in this context
  //fb-contrib:EXS_EXCEPTION_SOFTENING_RETURN_FALSE: our design needs a boolean result
  private <T> boolean canAuthorizeOperation(T input, Consumer<T> check, String operationName) {
    try {
      check.accept(input);
    } catch (AdminAuthorizationException e) {
      LOGGER.info("{} can not be authorized, since: {}", operationName, e.getUserMessage());
      return false;
    }
    return true;
  }

  @Override
  public void checkCanResendInvitationMessage(User user) {
    getAdminActionsAuthorizer(user).checkCanResendInvitationMessage();
  }

  @Override
  public void checkCanUnlockUser(User user) {
    getAdminActionsAuthorizer(user).checkCanUpdateUser();
  }

  @Autowired
  public void setAdminRoleImplementorFactory(
      AdminRoleImplementorFactory adminRoleImplementorFactory) {
    this.adminRoleImplementorFactory = adminRoleImplementorFactory;
  }

  @Autowired
  public void setExceptionFactory(ExceptionFactory exceptionFactory) {
    this.exceptionFactory = exceptionFactory;
  }
}
