package gov.ca.cwds;

import gov.ca.cwds.rest.api.domain.auth.UserAuthorization;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by dmitry.rudenko on 7/28/2017.
 */
public class UniversalUserToken implements Serializable {
  private String userId;
  private Set<String> roles = new LinkedHashSet<>();
  private String token;
  private Map<String, Object> parameters = new HashMap<>();
  private UserAuthorization authorization;

  public Object getParameter(String parameterName) {
    return parameters.get(parameterName);
  }

  public Object setParameter(String parameterName, Object parameter) {
    return parameters.put(parameterName, parameter);
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public Set<String> getRoles() {
    return roles;
  }

  public void setRoles(Set<String> roles) {
    this.roles = roles;
  }

  public String toString() {
    return userId;
  }

  public UserAuthorization getAuthorization() {
    return authorization;
  }

  public void setAuthorization(UserAuthorization authorization) {
    this.authorization = authorization;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }
}
