package gov.ca.cwds.idm.service.authorization;

import gov.ca.cwds.config.api.idm.Roles;
import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserUpdate;
import gov.ca.cwds.idm.service.role.implementor.AdminActionsAuthorizerFactory;
import gov.ca.cwds.util.Utils;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service("authorizationService")
@Profile("idm")
public class AuthorizationServiceImpl implements AuthorizationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationServiceImpl.class);

  private AdminActionsAuthorizerFactory adminRoleImplementorFactory;

  @Override
  public void checkCanViewUser(User user) {
    getAdminActionsAuthorizer(user).checkCanViewUser();
  }

  @Override
  public void checkCanCreateUser(User user) {
    getAdminActionsAuthorizer(user).checkCanCreateUser();
  }

  @Override
  public void checkCanUpdateUser(User user, UserUpdate updateUserDto) {
    getAdminActionsAuthorizer(user, updateUserDto).checkCanUpdateUser();
  }

  @Override
  public boolean canUpdateUser(User user, UserUpdate updateUser) {
    return getAdminActionsAuthorizer(user, updateUser).canUpdateUser();
  }

  @Override
  public boolean canUpdateUser(User user) {
    return canUpdateUser(user, new UserUpdate());
  }

  @Override
  public List<String> getAllowedUiRolesForUpdate(User existedUser) {
    AdminActionsAuthorizer authorizer = getAdminActionsAuthorizer(existedUser, new UserUpdate());
    boolean userCanBeUpdated = authorizer.canUpdateUser();
    if (userCanBeUpdated) {
      return authorizer.getPossibleRolesForUpdate();
    } else {
      return Collections.emptyList();
    }
  }

  private AdminActionsAuthorizer getAdminActionsAuthorizer(User user) {
    return adminRoleImplementorFactory.getAdminActionsAuthorizer(user, new UserUpdate());
  }

  private AdminActionsAuthorizer getAdminActionsAuthorizer(User user, UserUpdate userUpdate) {
    return adminRoleImplementorFactory.getAdminActionsAuthorizer(user, userUpdate);
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
      AdminActionsAuthorizerFactory adminRoleImplementorFactory) {
    this.adminRoleImplementorFactory = adminRoleImplementorFactory;
  }
}
