package gov.ca.cwds.idm.service;

import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserAndOperation;
import gov.ca.cwds.idm.dto.UserUpdate;
import gov.ca.cwds.idm.dto.UserVerificationResult;
import gov.ca.cwds.idm.dto.UsersPage;
import gov.ca.cwds.idm.dto.UsersSearchCriteria;
import java.util.Date;
import java.util.List;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;

public interface IdmService {
  @PostAuthorize("returnObject.countyName == principal.getParameter('county_name')")
  User findUser(String id);

  void updateUser(String id, UserUpdate updateUserDto);

  UserVerificationResult verifyUser(String racfId, String email);

  UsersPage getUserPage(String paginationToken);

  @PreAuthorize("#user.countyName == principal.getParameter('county_name')")
  String createUser(@P("user") User user);

  List<User> searchUsers(UsersSearchCriteria usersSearchCriteria);

  List<UserAndOperation> getFailedOperations(Date lastJobTime);
}
