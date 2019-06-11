package gov.ca.cwds.idm.service.role.implementor;

import static gov.ca.cwds.config.api.idm.Roles.CALS_EXTERNAL_WORKER;
import static gov.ca.cwds.config.api.idm.Roles.COUNTY_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.CWS_WORKER;
import static gov.ca.cwds.config.api.idm.Roles.OFFICE_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.STATE_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.SUPER_ADMIN;

import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserUpdate;
import gov.ca.cwds.idm.service.rule.ErrorRuleList;
import java.util.Arrays;
import java.util.List;

class SuperAdminAuthorizer extends AbstractAdminActionsAuthorizer {

  SuperAdminAuthorizer(User user, UserUpdate userUpdate) {
    super(user, userUpdate);
  }

  @Override
  public ErrorRuleList getViewUserRules() {
    return new ErrorRuleList();
  }

  @Override
  public ErrorRuleList getCreateUserRules() {
    return new ErrorRuleList()
        .add(rules.createdUserRolesMayBeOnly(
            SUPER_ADMIN, STATE_ADMIN, COUNTY_ADMIN, OFFICE_ADMIN, CWS_WORKER, CALS_EXTERNAL_WORKER));
  }

  @Override
  public ErrorRuleList getUpdateUserRules() {
    return new ErrorRuleList()
        .add(rules.userAndAdminAreNotTheSameUser())
        .add(rules.updatedUserRolesMayBeOnly(getPossibleRolesForUpdate()));
  }

  @Override
  public ErrorRuleList getResendInvitationMessageRules() {
    return new ErrorRuleList();
  }

  @Override
  public List<String> getPossibleRolesForUpdate() {
    return Arrays.asList(
        SUPER_ADMIN, STATE_ADMIN, COUNTY_ADMIN, OFFICE_ADMIN, CWS_WORKER, CALS_EXTERNAL_WORKER);
  }
}
