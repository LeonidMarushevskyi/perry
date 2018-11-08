package gov.ca.cwds.idm.service.cognito.util;

import static gov.ca.cwds.idm.service.cognito.CustomUserAttribute.COUNTY;
import static gov.ca.cwds.idm.service.cognito.CustomUserAttribute.OFFICE;
import static gov.ca.cwds.idm.service.cognito.CustomUserAttribute.PERMISSIONS;
import static gov.ca.cwds.idm.service.cognito.CustomUserAttribute.RACFID_CUSTOM;
import static gov.ca.cwds.idm.service.cognito.CustomUserAttribute.RACFID_CUSTOM_2;
import static gov.ca.cwds.idm.service.cognito.CustomUserAttribute.ROLES;
import static gov.ca.cwds.idm.service.cognito.StandardUserAttribute.EMAIL;
import static gov.ca.cwds.idm.service.cognito.StandardUserAttribute.EMAIL_VERIFIED;
import static gov.ca.cwds.idm.service.cognito.StandardUserAttribute.FIRST_NAME;
import static gov.ca.cwds.idm.service.cognito.StandardUserAttribute.LAST_NAME;
import static gov.ca.cwds.idm.service.cognito.StandardUserAttribute.RACFID_STANDARD;
import static gov.ca.cwds.util.Utils.toSet;
import static gov.ca.cwds.util.Utils.toUpperCase;

import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.cognitoidp.model.UserType;
import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.service.cognito.AttributesBuilder;
import gov.ca.cwds.idm.service.cognito.UserAttribute;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

public class CognitoUtils {

  public static final String EMAIL_DELIVERY = "EMAIL";
  private static final String COGNITO_LIST_DELIMITER = ":";
  private static final String TRUE_VALUE = "True";

  private CognitoUtils() {}

  static Optional<AttributeType> getAttribute(UserType cognitoUser, String attrName) {
    List<AttributeType> attributes = cognitoUser.getAttributes();

    if (CollectionUtils.isEmpty(attributes)) {
      return Optional.empty();
    } else {
      return attributes
          .stream()
          .filter(attr -> attr.getName().equalsIgnoreCase(attrName))
          .findFirst();
    }
  }

  public static String getAttributeValue(UserType cognitoUser, String attributeName) {
    return getAttribute(cognitoUser, attributeName).map(AttributeType::getValue).orElse(null);
  }

  public static String getCountyName(UserType cognitoUser) {
    return getAttributeValue(cognitoUser, COUNTY.getName());
  }

  public static String getEmail(UserType cognitoUser) {
    return getAttributeValue(cognitoUser, EMAIL.getName());
  }

  public static Set<String> getPermissions(UserType cognitoUser) {
    return getDelimitedAttributeValue(cognitoUser, PERMISSIONS);
  }

  public static Set<String> getRoles(UserType cognitoUser) {
    return getDelimitedAttributeValue(cognitoUser, ROLES);
  }

  public static Set<String> getDelimitedAttributeValue(UserType cognitoUser, UserAttribute userAttribute) {
    Optional<AttributeType> attrOpt = getAttribute(cognitoUser, userAttribute.getName());

    if (!attrOpt.isPresent()) {
      return new HashSet<>();
    }

    AttributeType attr = attrOpt.get();
    String attrStrValue = attr.getValue();

    if (StringUtils.isEmpty(attrStrValue)) {
      return new HashSet<>();
    }

    return toSet(attrStrValue.split(COGNITO_LIST_DELIMITER));
  }

  public static AttributeType attribute(String name, String value) {
    AttributeType permissionsAttr = new AttributeType();
    permissionsAttr.setName(name);
    permissionsAttr.setValue(value);
    return permissionsAttr;
  }

  public static String getCustomDelimitedListAttributeValue(Set<String> setOfValues) {
    if (!CollectionUtils.isEmpty(setOfValues)) {
      return String.join(COGNITO_LIST_DELIMITER, setOfValues);
    } else {
      return "";
    }
  }

  public static AttributeType createPermissionsAttribute(Set<String> permissions) {
    return createDelimitedAttribute(PERMISSIONS, permissions);
  }

  public static AttributeType createRolesAttribute(Set<String> roles) {
    return createDelimitedAttribute(ROLES, roles);
  }

  public static AttributeType createDelimitedAttribute(UserAttribute userAttribute,
      Set<String> values) {
    return attribute(userAttribute.getName(), getCustomDelimitedListAttributeValue(values));
  }

  public static List<AttributeType> buildCreateUserAttributes(User user) {

    String racfid = toUpperCase(user.getRacfid());

    AttributesBuilder attributesBuilder =
        new AttributesBuilder()
            .addAttribute(EMAIL, user.getEmail())
            .addAttribute(FIRST_NAME, user.getFirstName())
            .addAttribute(LAST_NAME, user.getLastName())
            .addAttribute(COUNTY, user.getCountyName())
            .addAttribute(OFFICE, user.getOfficeId())
            .addAttribute(RACFID_CUSTOM, racfid)
            .addAttribute(RACFID_CUSTOM_2, racfid)
            .addAttribute(RACFID_STANDARD, racfid)
            .addAttribute(EMAIL_VERIFIED, TRUE_VALUE)
            .addAttribute(createPermissionsAttribute(user.getPermissions()))
            .addAttribute(createRolesAttribute(user.getRoles()));
    return attributesBuilder.build();
  }

  public static String getRACFId(UserType user) {
    return CognitoUtils.getAttributeValue(user, RACFID_CUSTOM.getName());
  }
}
