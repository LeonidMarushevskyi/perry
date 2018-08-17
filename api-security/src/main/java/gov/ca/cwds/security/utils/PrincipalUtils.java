package gov.ca.cwds.security.utils;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import gov.ca.cwds.security.realm.PerryAccount;

/**
 * Created by Alexander Serbin on 12/21/2017.
 */
public final class PrincipalUtils {

  public static final String DEFAULT_USER_ID = "0X5";

  private PrincipalUtils() {}

  public static PerryAccount getPrincipal() {
    Optional<PerryAccount> perryAccount = Optional.empty();
    final Subject currentUser = SecurityUtils.getSubject();

    if (currentUser.getPrincipals() != null) {
      perryAccount = currentUser.getPrincipals().asList().stream()
          .filter(principal -> principal instanceof PerryAccount).findAny();
    }

    return perryAccount.orElseThrow(IllegalStateException::new);
  }

  public static String getStaffPersonId() {
    final PerryAccount perryAccount = getPrincipal();
    if (StringUtils.isEmpty(perryAccount.getStaffId())) {
      return DEFAULT_USER_ID;
    }
    return perryAccount.getStaffId();
  }

}
