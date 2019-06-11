package gov.ca.cwds.idm.service.role.implementor;

import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserUpdate;
import gov.ca.cwds.idm.service.authorization.AdminActionsAuthorizer;
import gov.ca.cwds.idm.service.rule.ErrorRuleList;
import gov.ca.cwds.idm.service.rule.ErrorRulesFactory;

public abstract class AbstractAdminActionsAuthorizer implements AdminActionsAuthorizer {

  protected ErrorRulesFactory rules;

  private final User user;
  private final UserUpdate userUpdate;

  AbstractAdminActionsAuthorizer(User user, UserUpdate userUpdate) {
    this.user = user;
    this.userUpdate = userUpdate;
  }

  protected User getUser() {
    return user;
  }

  protected UserUpdate getUserUpdate() {
    return userUpdate;
  }

  public abstract ErrorRuleList getViewUserRules();

  @Override
  public final void checkCanViewUser() {
    getViewUserRules().check();
  }

  public abstract ErrorRuleList getCreateUserRules();

  @Override
  public final void checkCanCreateUser() {
    getCreateUserRules().check();
  }

  public abstract ErrorRuleList getResendInvitationMessageRules();

  @Override
  public final void checkCanResendInvitationMessage() {
    getResendInvitationMessageRules().check();
  }

  public final ErrorRuleList getUpdateUserRules() {
    ErrorRuleList updateRuleList = new ErrorRuleList()
    .add(rules.userAndAdminAreNotTheSameUser());
    addCustomUpdateUserRules(updateRuleList);
    updateRuleList.add(rules.updatedUserRolesMayBeOnly(getPossibleRolesForUpdate()));
    return updateRuleList;
  }

  public abstract void addCustomUpdateUserRules(ErrorRuleList updateRuleList);

  @Override
  public final void checkCanUpdateUser() {
    getUpdateUserRules().check();
  }

  @Override
  public final boolean canUpdateUser() {
    return !getUpdateUserRules().hasError();
  }

  public void setRules(ErrorRulesFactory rules) {
    this.rules = rules;
  }
}
