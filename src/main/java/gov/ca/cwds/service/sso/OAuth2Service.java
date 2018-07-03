package gov.ca.cwds.service.sso;

import gov.ca.cwds.rest.api.domain.PerryException;
import gov.ca.cwds.service.sso.custom.OAuth2RequestHttpEntityFactory;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
@Profile("prod")
public class OAuth2Service implements SsoService {

  private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2Service.class);

  private OAuth2ProtectedResourceDetails resourceDetails;
  private OAuth2RestTemplate clientTemplate;
  private ResourceServerProperties resourceServerProperties;
  @Autowired(required = false)
  private OAuth2ClientContext clientContext;
  @Value("${security.oauth2.resource.revokeTokenUri}")
  private String revokeTokenUri;
  @Autowired
  private OAuth2RequestHttpEntityFactory httpEntityFactory;
  private ObjectMapper objectMapper;

  @PostConstruct
  public void init() {
    ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();
    resource.setAccessTokenUri(resourceDetails.getAccessTokenUri());
    resource.setClientId(resourceServerProperties.getClientId());
    resource.setClientSecret(resourceServerProperties.getClientSecret());
    resource.setAuthenticationScheme(resourceDetails.getAuthenticationScheme());
    resource.setClientAuthenticationScheme(resourceDetails.getClientAuthenticationScheme());
    clientTemplate = new OAuth2RestTemplate(resource);
    objectMapper = new ObjectMapper();
  }

  @Autowired
  public void setResourceServerProperties(ResourceServerProperties resourceServerProperties) {
    this.resourceServerProperties = resourceServerProperties;
  }

  @Autowired
  public void setResourceDetails(OAuth2ProtectedResourceDetails resourceDetails) {
    this.resourceDetails = resourceDetails;
  }

  @Override
  public Map getUserInfo(String ssoToken) {
    return doPost(userRestTemplate(ssoToken),
        resourceServerProperties.getUserInfoUri(),
        ssoToken,
        Map.class);
  }

  @Override
  @Retryable(interceptor = "retryInterceptor", value = HttpClientErrorException.class)
  public String validate(Serializable ssoContext) {
      OAuth2ClientContext oAuth2ClientContext = (OAuth2ClientContext) ssoContext;
      OAuth2RestTemplate restTemplate = userRestTemplate(oAuth2ClientContext);
      doPost(restTemplate, resourceServerProperties.getTokenInfoUri(),
          restTemplate.getAccessToken().getValue());
      return restTemplate.getOAuth2ClientContext().getAccessToken().getValue();
  }

  @Override
  public void invalidate(String ssoToken) {
    doPost(clientTemplate, revokeTokenUri, ssoToken);
  }

  @Override
  public String getSsoToken() {
    return clientContext.getAccessToken().getValue();
  }

  @Override
  public Serializable getSecurityContext() {
    Serializable apiSecurityContext = getApiSecurityContext();
    return apiSecurityContext != null ? apiSecurityContext : getWebSecurityContext();
  }

  private Serializable getApiSecurityContext() {
    try {
      Object credential = SecurityContextHolder.getContext().getAuthentication().getCredentials();
      if (credential instanceof OAuth2ClientContext) {
        return (Serializable) credential;
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  private Serializable getWebSecurityContext() {
    //TODO possible serialization issues
    if (clientContext != null) {
      DefaultOAuth2ClientContext context = new DefaultOAuth2ClientContext(
          clientContext.getAccessTokenRequest());
      context.setAccessToken(clientContext.getAccessToken());
      return context;
    }
    return null;
  }

  private OAuth2RestTemplate userRestTemplate(OAuth2ClientContext oAuth2ClientContext) {
    return new OAuth2RestTemplate(resourceDetails, oAuth2ClientContext);
  }

  private OAuth2RestTemplate userRestTemplate(String accessToken) {
    return new OAuth2RestTemplate(resourceDetails,
        new DefaultOAuth2ClientContext(new DefaultOAuth2AccessToken(accessToken)));
  }

  private String doPost(OAuth2RestTemplate restTemplate, String url, String accessToken) {
    return restTemplate.postForObject(url,
        httpEntityFactory.build(url, accessToken),
        String.class);
  }

  private <T> T doPost(OAuth2RestTemplate restTemplate, String url, String accessToken,
      Class<T> clazz) {
    String response = doPost(restTemplate, url, accessToken);
    try {
      return objectMapper.readValue(response, clazz);
    } catch (IOException e) {
      throw new PerryException("url: " + url + ". error parsing response: " + response, e);
    }
  }
}
