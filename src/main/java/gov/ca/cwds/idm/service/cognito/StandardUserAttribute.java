package gov.ca.cwds.idm.service.cognito;

public enum StandardUserAttribute implements UserAttribute {
  EMAIL("email"),
  FIRST_NAME("given_name"),
  LAST_NAME("family_name"),
  PHONE_NUMBER("phone_number"),
  RACFID_STANDARD("preferred_username"),
  EMAIL_VERIFIED("email_verified");

  private String name;

  StandardUserAttribute(String name){
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }
}
