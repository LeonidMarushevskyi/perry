package gov.ca.cwds.idm.event;

import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCurrentUser;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCurrentUserFullName;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCurrentUserName;

import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserChangeLogRecord;
import gov.ca.cwds.idm.service.authorization.UserRolesService;
import java.time.LocalDateTime;

/**
 * Created by Alexander Serbin on 1/11/2019
 */

abstract class UserChangeLogEvent extends AuditEvent<UserChangeLogRecord> {

  private static final long serialVersionUID = -2341018571605446028L;

  static final String CAP_EVENT_SOURCE = "CAP";

  UserChangeLogEvent(User user) {
    setTimestamp(LocalDateTime.now());
    setEventSource(CAP_EVENT_SOURCE);
    setUserLogin(getCurrentUserName());
    UserChangeLogRecord userChangeLogRecord = new UserChangeLogRecord();
    userChangeLogRecord.setAdminName(getCurrentUserFullName());
    String adminRole = UserRolesService.getStrongestAdminRole(getCurrentUser());
    userChangeLogRecord.setAdminRole(adminRole);
    setEvent(userChangeLogRecord);
    getEvent().setCountyName(user.getCountyName());
    getEvent().setOfficeId(user.getOfficeId());
    getEvent().setUserId(user.getId());
    getEvent().setUserName(user.getFirstName() + " " + user.getLastName());
  }

  protected void setAdminRole(String adminRole) {
    getEvent().setAdminRole(adminRole);
  }

  protected void setAdminName(String adminName) {
    getEvent().setAdminName(adminName);
  }

  protected void setUserRoles(String userRoles) {
    getEvent().setUserRoles(userRoles);
  }

  protected void setUserId(String userId) {
    getEvent().setUserId(userId);
  }

  protected void setUserName(String userName) {
    getEvent().setUserName(userName);
  }

  protected void setOldValue(String oldValue) {
    getEvent().setOldValue(oldValue);
  }

  protected void setNewValue(String newValue) {
    getEvent().setNewValue(newValue);
  }

  protected void setOfficeId(String officeId) {
    getEvent().setOfficeId(officeId);
  }

  protected void setCountyName(String countyName) {
    getEvent().setCountyName(countyName);
  }

}
