package gov.ca.cwds.idm.service.validation;

import static gov.ca.cwds.idm.service.PossibleUserPermissionsService.CANS_PERMISSION_NAME;
import static gov.ca.cwds.service.messages.MessageCode.ACTIVE_USER_WITH_RAFCID_EXISTS_IN_IDM;
import static gov.ca.cwds.service.messages.MessageCode.COUNTY_NAME_IS_NOT_PROVIDED;
import static gov.ca.cwds.service.messages.MessageCode.ENABELED_STATUS_CANNOT_BE_NULL;
import static gov.ca.cwds.service.messages.MessageCode.FIRST_NAME_IS_NOT_PROVIDED;
import static gov.ca.cwds.service.messages.MessageCode.INVALID_CELL_PHONE_FORMAT;
import static gov.ca.cwds.service.messages.MessageCode.INVALID_PHONE_EXTENSION_FORMAT;
import static gov.ca.cwds.service.messages.MessageCode.INVALID_PHONE_FORMAT;
import static gov.ca.cwds.service.messages.MessageCode.LAST_NAME_IS_NOT_PROVIDED;
import static gov.ca.cwds.service.messages.MessageCode.NO_USER_WITH_RACFID_IN_CWSCMS;
import static gov.ca.cwds.service.messages.MessageCode.UNABLE_TO_ASSIGN_CANS_PERMISSION_TO_NON_RACFID_USER;
import static gov.ca.cwds.service.messages.MessageCode.UNABLE_TO_CREATE_NON_RACFID_USER_WITH_CANS_PERMISSION;
import static gov.ca.cwds.service.messages.MessageCode.UNABLE_TO_CREATE_USER_WITHOUT_ROLES;
import static gov.ca.cwds.service.messages.MessageCode.UNABLE_TO_CREATE_USER_WITH_UNALLOWED_ROLES;
import static gov.ca.cwds.service.messages.MessageCode.UNABLE_TO_REMOVE_ALL_ROLES;
import static gov.ca.cwds.service.messages.MessageCode.UNABLE_UPDATE_UNALLOWED_ROLES;
import static gov.ca.cwds.service.messages.MessageCode.USER_CANNOT_BE_UNLOCKED;
import static gov.ca.cwds.service.messages.MessageCode.USER_PHONE_IS_NOT_PROVIDED;
import static gov.ca.cwds.service.messages.MessageCode.USER_WITH_EMAIL_EXISTS_IN_IDM;
import static gov.ca.cwds.util.Utils.isRacfidUser;
import static gov.ca.cwds.util.Utils.toCommaDelimitedString;
import static java.lang.Boolean.FALSE;

import gov.ca.cwds.idm.dto.UpdateProperty;
import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserUpdate;
import gov.ca.cwds.idm.service.cognito.CognitoServiceFacade;
import gov.ca.cwds.idm.service.exception.ExceptionFactory;
import gov.ca.cwds.idm.service.role.implementor.AdminRoleImplementorFactory;
import gov.ca.cwds.service.CwsUserInfoService;
import gov.ca.cwds.service.messages.MessageCode;
import java.util.Collection;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("idm")
public class ValidationServiceImpl implements ValidationService {

  static final PatternValidator PHONE_PATTERN_VALIDATOR = new PatternValidator("[1-9]\\d{9}");
  static final PatternValidator PHONE_EXTENSION_PATTERN_VALIDATOR = new PatternValidator("\\d{0,7}");

  private CwsUserInfoService cwsUserInfoService;

  private AdminRoleImplementorFactory adminRoleImplementorFactory;

  private ExceptionFactory exceptionFactory;

  private CognitoServiceFacade cognitoServiceFacade;

  @Override
  public void validateUserCreate(User enrichedUser) {

    validateFirstNameIsProvided(enrichedUser);
    validateLastNameIsProvided(enrichedUser);
    validateCountyNameIsProvided(enrichedUser);
    validateCellPhoneNumber(enrichedUser.getCellPhoneNumber());

    validateUserRolesExistAtCreate(enrichedUser);
    validateUserRolesAreAllowedAtCreate(enrichedUser);

    validateCreateByCansPermission(enrichedUser);

    validateActiveRacfidUserExistsInCws(enrichedUser.getRacfid());
    validateRacfidDoesNotExistInCognito(enrichedUser.getRacfid());
  }

  @Override
  public void validateVerifyIfUserCanBeCreated(User enrichedUser) {
    validateActiveRacfidUserExistsInCws(enrichedUser.getRacfid());
    validateEmailDoesNotExistInCognito(enrichedUser.getEmail());
    validateRacfidDoesNotExistInCognito(enrichedUser.getRacfid());
  }

  @Override
  public void validateUserUpdate(User existedUser, UserUpdate updateUserDto) {
    validateUpdatePhoneNumber(updateUserDto.getPhoneNumber());
    validateUpdatePhoneExtension(updateUserDto.getPhoneExtensionNumber());
    validateUpdateCellPhoneNumber(updateUserDto.getCellPhoneNumber());
    validateNotAllRolesAreRemovedAtUpdate(updateUserDto);
    validateNewUserRolesAreAllowedAtUpdate(updateUserDto);
    validateUpdateByCansPermission(existedUser, updateUserDto);
    validateEnabledNotNull(updateUserDto.getEnabled());
    validateActivateUser(existedUser, updateUserDto);
  }

  @Override
  public void validateUnlockUser(User existedUser, boolean newLocked) {
    if (!canChangeLockStatusToFalse(newLocked, existedUser.isLocked())) {
      throwValidationException(USER_CANNOT_BE_UNLOCKED, existedUser.getRacfid());
    }
  }

  private void validateFirstNameIsProvided(User user) {
    validateIsNotBlank(user.getFirstName(), FIRST_NAME_IS_NOT_PROVIDED);
  }

  private void validateLastNameIsProvided(User user) {
    validateIsNotBlank(user.getLastName(), LAST_NAME_IS_NOT_PROVIDED);
  }

  private void validateCountyNameIsProvided(User user) {
    validateIsNotBlank(user.getCountyName(), COUNTY_NAME_IS_NOT_PROVIDED);
  }

  private void validateIsNotBlank(String value, MessageCode errorCode) {
    if (StringUtils.isBlank(value)) {
      throwValidationException(errorCode);
    }
  }

  private void validateActiveRacfidUserExistsInCws(String racfId) {
    if (!isRacfidUser(racfId)) {
      return;
    }

    if (cwsUserInfoService.getCwsUserByRacfId(racfId) == null) {
      throwValidationException(NO_USER_WITH_RACFID_IN_CWSCMS, racfId);
    }
  }

  private void validateUserRolesExistAtCreate(User user) {
    if (CollectionUtils.isEmpty(user.getRoles())) {
      throwValidationException(UNABLE_TO_CREATE_USER_WITHOUT_ROLES);
    }
  }

  private void validateUserRolesAreAllowedAtCreate(User user) {
    validateByAllowedRoles(user.getRoles(), UNABLE_TO_CREATE_USER_WITH_UNALLOWED_ROLES);
  }

  void validateRacfidDoesNotExistInCognito(String racfId) {
    if (!isRacfidUser(racfId)) {
      return;
    }

    if (isActiveRacfIdPresentInCognito(racfId)) {
      throwValidationException(ACTIVE_USER_WITH_RAFCID_EXISTS_IN_IDM, racfId);
    }
  }

  private void validateEmailDoesNotExistInCognito(String email) {
    if (userWithEmailExistsInCognito(email)) {
      throwValidationException(USER_WITH_EMAIL_EXISTS_IN_IDM, email);
    }
  }

  private void validateNotAllRolesAreRemovedAtUpdate(UserUpdate updateUserDto) {
    UpdateProperty<Set<String>> rolesProperty = updateUserDto.getRoles();
    if(!rolesProperty.isSet()){
      return;
    }

    Collection<String> newUserRoles = rolesProperty.get();

    if (newUserRoles == null || newUserRoles.isEmpty()) {
      throwValidationException(UNABLE_TO_REMOVE_ALL_ROLES);
    }
  }

  private void validateNewUserRolesAreAllowedAtUpdate(UserUpdate updateUserDto) {
    UpdateProperty<Set<String>> rolesProperty = updateUserDto.getRoles();
    if(!rolesProperty.isSet()) {
      return;
    }

    validateByAllowedRoles(rolesProperty.get(), UNABLE_UPDATE_UNALLOWED_ROLES);
  }

  private void validateByAllowedRoles(Collection<String> roles, MessageCode errorCode) {

    Collection<String> allowedRoles = adminRoleImplementorFactory.getPossibleUserRoles();
    if (roles == null || !allowedRoles.containsAll(roles)) {
      throwValidationException(
          errorCode,
          toCommaDelimitedString(roles),
          toCommaDelimitedString(allowedRoles));
    }
  }

  private void validateCreateByCansPermission(User user) {
    validateByCansPermission(user.getPermissions(), isRacfidUser(user), user.getId(),
        UNABLE_TO_CREATE_NON_RACFID_USER_WITH_CANS_PERMISSION);
  }

  private void validateUpdateByCansPermission(User existedUser, UserUpdate updateUserDto) {
    UpdateProperty<Set<String>> permissionsProperty = updateUserDto.getPermissions();
    if (!permissionsProperty.isSet()) {
      return;
    }

    validateByCansPermission(permissionsProperty.get(), isRacfidUser(existedUser),
        existedUser.getId(), UNABLE_TO_ASSIGN_CANS_PERMISSION_TO_NON_RACFID_USER);
  }

  private void validateByCansPermission(Collection<String> newUserPermissions, boolean isRacfidUser,
      String userId, MessageCode errorCode) {
    if (newUserPermissions == null) {
      return;
    }

    if (!isRacfidUser && newUserPermissions.contains(CANS_PERMISSION_NAME)) {
      throwValidationException(errorCode, userId);
    }
  }

  private boolean isActiveRacfIdPresentInCognito(String racfId) {
    return cognitoServiceFacade.isActiveRacfIdPresentInCognito(racfId);
  }

  private boolean userWithEmailExistsInCognito(String email) {
    return cognitoServiceFacade.doesUserWithEmailExistInCognito(email);

  }

  private void throwValidationException(MessageCode messageCode, String... args) {
    throw exceptionFactory.createValidationException(messageCode, args);
  }

  private void validateEnabledNotNull(UpdateProperty<Boolean> enabledProperty){
    if (!enabledProperty.isSet()) {
      return;
    }
    if(enabledProperty.get() == null){
      throwValidationException(ENABELED_STATUS_CANNOT_BE_NULL);
    }
  }

  private void validateActivateUser(User existedUser, UserUpdate updateUserDto) {
    UpdateProperty<Boolean> enabledProperty = updateUserDto.getEnabled();
    if (!enabledProperty.isSet()) {
      return;
    }
    if (changeToEnableActiveStatus(enabledProperty.get(), existedUser.getEnabled())) {
      String racfId = existedUser.getRacfid();
      if (StringUtils.isNotBlank(racfId)) {
        validateActivateUser(racfId);
      }
    }
  }

  private static boolean changeToEnableActiveStatus(Boolean newEnabled, Boolean currentEnabled) {
    return newEnabled != null && !newEnabled.equals(currentEnabled) && newEnabled;
  }

  private static boolean canChangeLockStatusToFalse(Boolean newLocked, Boolean currentLocked) {
    return FALSE.equals(newLocked) && !newLocked.equals(currentLocked);
  }

  private void validateActivateUser(String racfId) {
    validateActiveRacfidUserExistsInCws(racfId);
    validateRacfidDoesNotExistInCognito(racfId);
  }

  void validatePhoneNumber(String newPhoneNumber) {
    validateRequiredStringProperty(
        newPhoneNumber,
        PHONE_PATTERN_VALIDATOR,
        USER_PHONE_IS_NOT_PROVIDED,
        INVALID_PHONE_FORMAT
    );
  }

  private void validateUpdatePhoneNumber(UpdateProperty<String> newPhoneNumber) {
    if(newPhoneNumber.isSet()) {
      validatePhoneNumber(newPhoneNumber.get());
    }
  }

  void validatePhoneExtension(String newPhoneExtension) {
    validateOptionalStringProperty(
        newPhoneExtension,
        PHONE_EXTENSION_PATTERN_VALIDATOR,
        INVALID_PHONE_EXTENSION_FORMAT);
  }

  private void validateUpdatePhoneExtension(UpdateProperty<String> newPhoneExtension) {
    if(newPhoneExtension.isSet()) {
      validatePhoneExtension(newPhoneExtension.get());
    }
  }

  void validateCellPhoneNumber(String newPhoneNumber) {
    validateOptionalStringProperty(
        newPhoneNumber,
        PHONE_PATTERN_VALIDATOR,
        INVALID_CELL_PHONE_FORMAT
    );
  }

  private void validateUpdateCellPhoneNumber(UpdateProperty<String> newPhoneNumber) {
    if(newPhoneNumber.isSet()){
      validateCellPhoneNumber(newPhoneNumber.get());
    }
  }

  private void validateRequiredStringProperty(
      String newValue,
      PatternValidator patternValidator,
      MessageCode absenceErrCode,
      MessageCode formatErrCode) {

    if(StringUtils.isBlank(newValue)){
      throwValidationException(absenceErrCode);
    }

    if (!patternValidator.isValid(newValue)) {
      throwValidationException(formatErrCode, newValue);
    }
  }

  private void validateOptionalStringProperty(
      String newValue,
      PatternValidator patternValidator,
      MessageCode errCode) {

    if (StringUtils.isBlank(newValue)) {
      return;
    }
    if (!patternValidator.isValid(newValue)) {
      throwValidationException(errCode, newValue);
    }
  }

  @Autowired
  public void setCwsUserInfoService(CwsUserInfoService cwsUserInfoService) {
    this.cwsUserInfoService = cwsUserInfoService;
  }

  @Autowired
  public void setCognitoServiceFacade(
      CognitoServiceFacade cognitoServiceFacade) {
    this.cognitoServiceFacade = cognitoServiceFacade;
  }

  @Autowired
  public void setAdminRoleImplementorFactory(
      AdminRoleImplementorFactory adminRoleImplementorFactory) {
    this.adminRoleImplementorFactory = adminRoleImplementorFactory;
  }

  @Autowired
  public void setExceptionFactory(ExceptionFactory exceptionFactory) {
    this.exceptionFactory = exceptionFactory;
  }
}
