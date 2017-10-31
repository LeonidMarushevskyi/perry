package gov.ca.cwds.data.reissue.model;

import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.SerializationUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by TPT2 on 10/24/2017.
 */
@Entity
@Table(name = "perry_token")
public class PerryTokenEntity implements Serializable {
  @Id
  @Column(name = "token")
  private String token;
  @Column(name = "access_code")
  private String accessCode;
  @Column(name = "user_id")
  private String user;
  //@Lob

  @Column(name = "access_token", length = 20000)
  private byte[] accessToken;
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "created_date")
  private Date createdDate = new Date();

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public byte[] getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(byte[] accessToken) {
    this.accessToken = accessToken;
  }

  @Transient
  public OAuth2AccessToken readAccessToken() {
    return (OAuth2AccessToken) SerializationUtils.deserialize(accessToken);
  }

  @Transient
  public void writeAccessToken(OAuth2AccessToken oAuth2AccessToken) {
    accessToken = SerializationUtils.serialize(oAuth2AccessToken);
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public String getAccessCode() {
    return accessCode;
  }

  public void setAccessCode(String accessCode) {
    this.accessCode = accessCode;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

}
