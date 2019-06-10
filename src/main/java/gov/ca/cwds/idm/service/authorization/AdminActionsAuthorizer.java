package gov.ca.cwds.idm.service.authorization;

import java.util.List;

public interface AdminActionsAuthorizer {

  void checkCanViewUser();

  void checkCanCreateUser();

  void checkCanUpdateUser();

  boolean canUpdateUser();

  void checkCanResendInvitationMessage();

  List<String> getPossibleRolesForUpdate();
}
