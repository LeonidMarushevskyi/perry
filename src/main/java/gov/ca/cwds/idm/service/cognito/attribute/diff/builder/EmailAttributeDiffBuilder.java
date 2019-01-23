package gov.ca.cwds.idm.service.cognito.attribute.diff.builder;

import static gov.ca.cwds.idm.service.cognito.attribute.StandardUserAttribute.EMAIL;

import com.amazonaws.services.cognitoidp.model.UserType;
import gov.ca.cwds.idm.service.cognito.attribute.diff.EmailUserAttributeDiff;

/**
 * Created by Alexander Serbin on 1/15/2019
 */
public class EmailAttributeDiffBuilder extends StringAttributeDiffBuilder {

  public EmailAttributeDiffBuilder(UserType userType, String newValue) {
    super(EMAIL, userType, newValue);
  }

  @Override
  public EmailUserAttributeDiff buildDiff() {
    return new EmailUserAttributeDiff(getUserType(), getNewValue());
  }
  
}
