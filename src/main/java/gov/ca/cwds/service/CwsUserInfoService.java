package gov.ca.cwds.service;

import gov.ca.cwds.data.auth.UserIdDao;
import gov.ca.cwds.data.persistence.auth.CwsOffice;
import gov.ca.cwds.data.persistence.auth.StaffPerson;
import gov.ca.cwds.data.persistence.auth.UserId;
import gov.ca.cwds.rest.api.domain.DomainChef;
import gov.ca.cwds.rest.api.domain.auth.StaffAuthorityPrivilege;
import gov.ca.cwds.rest.api.domain.auth.StaffUnitAuthority;
import gov.ca.cwds.rest.services.CrudsService;
import gov.ca.cwds.service.dto.CwsUserInfo;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional("transactionManager")
public class CwsUserInfoService {

  private static final int RACFID_MAX_LENGTH = 8;

  private static final Logger LOGGER = LoggerFactory.getLogger(CwsUserInfoService.class);

  @Autowired
  private UserIdDao userIdDao;

  /**
   * {@inheritDoc}
   *
   * @see CrudsService#find(Serializable)
   */
  public CwsUserInfo composeForUserAuthorization(Serializable primaryKey) {
    Optional<UserId> userId = findUserId(primaryKey);
    if (!userId.isPresent()) {
      LOGGER.warn("No RACFID found for {}", primaryKey);
      return null;
    }
    UserId user = userId.get();
    return getCwsUserInfo(user);
  }

  //just because of codeclimate 25 lines of code allowed rule
  private CwsUserInfo getCwsUserInfo(UserId user) {
    boolean socialWorker = isSocialWorker(user);
    Set<StaffAuthorityPrivilege> userAuthPrivs = getStaffAuthorityPriveleges(user);
    Set<StaffUnitAuthority> setStaffUnitAuths = getStaffUnitAuthorities(user);
    StaffPerson staffPerson = user.getStaffPerson();
    if (staffPerson == null) {
      LOGGER.warn("No staff person found for UserId {}", user.getLogonId());
      return null;
    }

    CwsOffice cwsOffice = staffPerson.getOffice();
    if (cwsOffice == null) {
      LOGGER.warn("No cws office for staffPerson {}", staffPerson.getId());
      return null;
    }

    return CwsUserInfo.CwsUserInfoBuilder.aCwsUserInfo()
        .withRacfId(user.getLogonId())
        .withCwsOffice(cwsOffice)
        .withStaffUnitAuths(setStaffUnitAuths)
        .withSocialWorker(socialWorker)
        .withUserAuthPrivs(userAuthPrivs)
        .withStaffPerson(staffPerson)
        .build();
  }

  private boolean isSocialWorker(UserId userId) {
    return Optional.ofNullable(userId.getPrivileges())
        .orElse(Collections.emptySet()).stream()
        .anyMatch(p -> p.getLevelOfAuthPrivilegeType() == 1468 &&
            "P".equals(p.getLevelOfAuthPrivilegeCode()));
  }

  public List<CwsUserInfo> findUsers(Collection<String> racfIds) {
    List<String> filtered =
        racfIds
            .stream()
            .filter(Objects::nonNull)
            .filter(e -> e.length() <= RACFID_MAX_LENGTH)
            .collect(Collectors.toList());
    if (CollectionUtils.isEmpty(filtered)) {
      return Collections.emptyList();
    }
    Set<UserId> userIdList = userIdDao.findActiveByLogonIdIn(filtered);
    return userIdList.stream().map(this::composeCwsUserInfo).collect(Collectors.toList());
  }

  private CwsUserInfo composeCwsUserInfo(UserId userId) {
    CwsOffice office =
        Optional.ofNullable(userId.getStaffPerson()).map(StaffPerson::getOffice).orElse(null);
    return CwsUserInfo.CwsUserInfoBuilder.aCwsUserInfo()
        .withRacfId(userId.getLogonId())
        .withCwsOffice(office)
        .withStaffPerson(userId.getStaffPerson())
        .build();
  }

  private Optional<UserId> findUserId(Serializable primaryKey) {
    LOGGER.info("Trying to find RACFID for user id {}", primaryKey);
    final String logonId = ((String) primaryKey).trim();
    if (logonId.length() > RACFID_MAX_LENGTH) {
      return Optional.empty();
    }
    Set<UserId> users = userIdDao.findActiveByLogonId(logonId);
    if (CollectionUtils.isEmpty(users)) {
      return Optional.empty();
    }
    return Optional.of(users.iterator().next());
  }

  /**
   * Gets the {@link StaffUnitAuthority} for a StaffPerson
   *
   * @param user The User Id
   * @return Set of StaffUnitAuthority for the Staff Person
   */
  private Set<StaffUnitAuthority> getStaffUnitAuthorities(UserId user) {
    return user.getStaffPerson().getUnitAuthorities()
        .stream()
        .map(
            staffUnitAuth -> {
              String endDate = DomainChef.cookDate(staffUnitAuth.getEndDate());
              String assignedUnitKey = staffUnitAuth.getFkasgUnit().trim();
              String assignedUnitEndDate = "";
              if (StringUtils.isNotBlank(assignedUnitKey)) {
                assignedUnitEndDate = DomainChef
                    .cookDate(staffUnitAuth.getAssignmentUnit().getEndDate());
              }
              return new StaffUnitAuthority(
                  staffUnitAuth.getAuthorityCode(),
                  assignedUnitKey,
                  assignedUnitEndDate,
                  staffUnitAuth.getCountySpecificCode(),
                  endDate);
            })
        .collect(Collectors.toSet());
  }

  /**
   * Gets the {@link StaffAuthorityPrivilege} for a User
   *
   * @param userId the User Identifier
   * @return Set of StaffAuthorityPrivilege for the User
   */
  private Set<StaffAuthorityPrivilege> getStaffAuthorityPriveleges(UserId userId) {
    return userId.getPrivileges()
        .stream()
        .map(
            priv ->
                new StaffAuthorityPrivilege(
                    priv.getLevelOfAuthPrivilegeType().toString(),
                    priv.getLevelOfAuthPrivilegeCode(),
                    priv.getCountySpecificCode(),
                    DomainChef.cookDate(priv.getEndDate())))
        .collect(Collectors.toSet());
  }

  public void setUserIdDao(UserIdDao userIdDao) {
    this.userIdDao = userIdDao;
  }

}
