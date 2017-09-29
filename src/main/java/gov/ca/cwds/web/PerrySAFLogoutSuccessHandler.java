package gov.ca.cwds.web;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * @author CWDS CALS API Team
 */
@Profile("prod")
@Component
@ConfigurationProperties(prefix = "security.oauth2.resource")
public class PerrySAFLogoutSuccessHandler extends PerryLogoutSuccessHandler implements
    LogoutSuccessHandler {

  private String logoutTokenUri;

  @Override
  protected boolean tryRedirect(HttpServletResponse response, String callback) throws IOException {
    assert(StringUtils.isNotBlank(logoutTokenUri));
    StringBuilder safLogoutUrlBuilder = new StringBuilder(logoutTokenUri);
    safLogoutUrlBuilder.append('?');
    Optional.ofNullable(callback).ifPresent(s -> {
      whiteList.validate("callback", callback);
      safLogoutUrlBuilder.append("redirectToClientLogin=").append(s);
    });

    URI safLogoutURI;
    try {
      safLogoutURI = new URI(safLogoutUrlBuilder.toString());
    } catch (URISyntaxException e) {
      logger.error(e.getMessage());
      return false;
    }
    response.sendRedirect(safLogoutURI.toString());
    return true;
  }

  public String getLogoutTokenUri() {
    return logoutTokenUri;
  }

  public void setLogoutTokenUri(String logoutTokenUri) {
    this.logoutTokenUri = logoutTokenUri;
  }
}
