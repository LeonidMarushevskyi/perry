package gov.ca.cwds.idm.service.role.implementor;

import static gov.ca.cwds.config.api.idm.Roles.CALS_EXTERNAL_WORKER;
import static gov.ca.cwds.config.api.idm.Roles.COUNTY_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.CWS_WORKER;
import static gov.ca.cwds.config.api.idm.Roles.OFFICE_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.STATE_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.SUPER_ADMIN;
import static gov.ca.cwds.idm.service.filter.MainRoleFilter.getMainRole;
import static gov.ca.cwds.service.messages.MessageCode.NOT_SUPER_ADMIN_CANNOT_UPDATE_USERS_WITH_SUPER_ADMIN_ROLE;
import static gov.ca.cwds.service.messages.MessageCode.NOT_SUPER_ADMIN_CANNOT_VIEW_USERS_WITH_SUPER_ADMIN_ROLE;

import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserUpdate;
import gov.ca.cwds.idm.service.rule.ErrorRuleList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class StateAdminAuthorizer extends AbstractAdminActionsAuthorizer {

  StateAdminAuthorizer(User user, UserUpdate userUpdate) {
    super(user, userUpdate);
  }

  @Override
  public ErrorRuleList getViewUserRules() {
    return new ErrorRuleList()
        .add(rules.userIsNotSuperAdmin(NOT_SUPER_ADMIN_CANNOT_VIEW_USERS_WITH_SUPER_ADMIN_ROLE));
  }

  @Override
  public ErrorRuleList getCreateUserRules() {
    return new ErrorRuleList()
        .add(rules.createdUserRolesMayBe(COUNTY_ADMIN, OFFICE_ADMIN, CWS_WORKER));
  }

  @Override
  public ErrorRuleList getResendInvitationMessageRules() {
    return new ErrorRuleList();
  }

  @Override
  public ErrorRuleList getUpdateUserRules() {
    return new ErrorRuleList()
        .add(rules.userAndAdminAreNotTheSameUser())
        .add(rules.userIsNotSuperAdmin(NOT_SUPER_ADMIN_CANNOT_UPDATE_USERS_WITH_SUPER_ADMIN_ROLE))
        .add(rules.stateAdminUserRolesCanNotBeChanged())
        .add(rules.calsExternalWorkerRolesCanNotBeChanged())
        .add(rules.userChangesRolesOnlyTo(getPossibleRolesForUpdate()));
  }

  @Override
  public List<String> getPossibleRolesForUpdate() {

    switch(getMainRole(getUser())) {
      case SUPER_ADMIN:
        return Collections.emptyList();
      case STATE_ADMIN:
      case CALS_EXTERNAL_WORKER:
        return new ArrayList<>(getUser().getRoles());
      default:
        return Arrays.asList(COUNTY_ADMIN, OFFICE_ADMIN, CWS_WORKER);
    }
  }
}
