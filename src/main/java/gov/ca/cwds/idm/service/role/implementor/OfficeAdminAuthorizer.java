package gov.ca.cwds.idm.service.role.implementor;

import static gov.ca.cwds.config.api.idm.Roles.CALS_EXTERNAL_WORKER;
import static gov.ca.cwds.config.api.idm.Roles.COUNTY_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.CWS_WORKER;
import static gov.ca.cwds.config.api.idm.Roles.OFFICE_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.STATE_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.SUPER_ADMIN;
import static gov.ca.cwds.idm.service.filter.MainRoleFilter.getMainRole;
import static gov.ca.cwds.service.messages.MessageCode.NOT_AUTHORIZED_TO_ADD_USER_FOR_OTHER_OFFICE;
import static gov.ca.cwds.service.messages.MessageCode.NOT_SUPER_ADMIN_CANNOT_UPDATE_USERS_WITH_SUPER_ADMIN_ROLE;
import static gov.ca.cwds.service.messages.MessageCode.NOT_SUPER_ADMIN_CANNOT_VIEW_USERS_WITH_SUPER_ADMIN_ROLE;
import static gov.ca.cwds.service.messages.MessageCode.OFFICE_ADMIN_CANNOT_RESEND_INVITATION_FOR_USER_FROM_OTHER_OFFICE;
import static gov.ca.cwds.service.messages.MessageCode.OFFICE_ADMIN_CANNOT_UPDATE_COUNTY_ADMIN;
import static gov.ca.cwds.service.messages.MessageCode.OFFICE_ADMIN_CANNOT_UPDATE_STATE_ADMIN;
import static gov.ca.cwds.service.messages.MessageCode.OFFICE_ADMIN_CANNOT_UPDATE_USER_FROM_OTHER_OFFICE;
import static gov.ca.cwds.service.messages.MessageCode.OFFICE_ADMIN_CANNOT_VIEW_USERS_WITH_CALS_EXTERNAL_WORKER_ROLE;
import static gov.ca.cwds.service.messages.MessageCode.OFFICE_ADMIN_CANNOT_VIEW_USER_FROM_OTHER_COUNTY;

import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserUpdate;
import gov.ca.cwds.idm.service.rule.ErrorRuleList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class OfficeAdminAuthorizer extends AbstractAdminActionsAuthorizer {

  OfficeAdminAuthorizer(User user, UserUpdate userUpdate) {
    super(user, userUpdate);
  }

  @Override
  public ErrorRuleList getViewUserRules() {
    return new ErrorRuleList()
        .add(rules.adminAndUserAreInTheSameCounty(
            OFFICE_ADMIN_CANNOT_VIEW_USER_FROM_OTHER_COUNTY, getUser().getId()))
        .add(rules.userIsNotCalsExternalWorker(
            OFFICE_ADMIN_CANNOT_VIEW_USERS_WITH_CALS_EXTERNAL_WORKER_ROLE))
        .add(rules.userIsNotSuperAdmin(NOT_SUPER_ADMIN_CANNOT_VIEW_USERS_WITH_SUPER_ADMIN_ROLE));
  }

  @Override
  public ErrorRuleList getCreateUserRules() {
    return new ErrorRuleList()
        .add(rules.adminAndUserAreInTheSameOffice(NOT_AUTHORIZED_TO_ADD_USER_FOR_OTHER_OFFICE))
        .add(rules.createdUserRolesMayBe(CWS_WORKER));
  }

  @Override
  public ErrorRuleList getUpdateUserRules() {
    return new ErrorRuleList()
        .add(rules.userAndAdminAreNotTheSameUser())
        .add(rules.adminAndUserAreInTheSameOffice(OFFICE_ADMIN_CANNOT_UPDATE_USER_FROM_OTHER_OFFICE))
        .add(rules.userIsNotCountyAdmin(OFFICE_ADMIN_CANNOT_UPDATE_COUNTY_ADMIN))
        .add(rules.userIsNotStateAdmin(OFFICE_ADMIN_CANNOT_UPDATE_STATE_ADMIN))
        .add(rules.userIsNotSuperAdmin(NOT_SUPER_ADMIN_CANNOT_UPDATE_USERS_WITH_SUPER_ADMIN_ROLE))
        .add(rules.calsExternalWorkerRolesCanNotBeChanged())
        .add(rules.userChangesRolesOnlyTo(getPossibleRolesForUpdate()));
  }

  @Override
  public List<String> getPossibleRolesForUpdate() {
    switch(getMainRole(getUser())) {
      case SUPER_ADMIN:
      case STATE_ADMIN:
      case COUNTY_ADMIN:
        return Collections.emptyList();
      case CALS_EXTERNAL_WORKER:
        return new ArrayList(getUser().getRoles());
      case OFFICE_ADMIN:
        return Arrays.asList(OFFICE_ADMIN, CWS_WORKER);
      default:
        return Arrays.asList(CWS_WORKER);
    }
  }

  @Override
  public ErrorRuleList getResendInvitationMessageRules() {
    return new ErrorRuleList()
        .add(rules.adminAndUserAreInTheSameOffice(
            OFFICE_ADMIN_CANNOT_RESEND_INVITATION_FOR_USER_FROM_OTHER_OFFICE));
  }
}
