package gov.ca.cwds.idm.service;

import static gov.ca.cwds.config.TokenServiceConfiguration.TOKEN_TRANSACTION_MANAGER;
import static gov.ca.cwds.config.api.idm.Roles.CWS_WORKER;
import static gov.ca.cwds.idm.persistence.ns.OperationType.CREATE;
import static gov.ca.cwds.idm.persistence.ns.OperationType.UPDATE;
import static gov.ca.cwds.idm.service.ExecutionStatus.FAIL;
import static gov.ca.cwds.idm.service.ExecutionStatus.SUCCESS;
import static gov.ca.cwds.idm.service.ExecutionStatus.WAS_NOT_EXECUTED;
import static gov.ca.cwds.idm.service.cognito.attribute.StandardUserAttribute.EMAIL;
import static gov.ca.cwds.idm.service.cognito.attribute.StandardUserAttribute.RACFID_STANDARD;
import static gov.ca.cwds.service.messages.MessageCode.ERROR_UPDATE_USER_ENABLED_STATUS;
import static gov.ca.cwds.service.messages.MessageCode.UNABLE_TO_PURGE_PROCESSED_USER_LOGS;
import static gov.ca.cwds.service.messages.MessageCode.UNABLE_TO_WRITE_LAST_LOGIN_TIME;
import static gov.ca.cwds.service.messages.MessageCode.USER_CREATE_SAVE_TO_SEARCH_AND_DB_LOG_ERRORS;
import static gov.ca.cwds.service.messages.MessageCode.USER_CREATE_SAVE_TO_SEARCH_ERROR;
import static gov.ca.cwds.service.messages.MessageCode.USER_NOTHING_UPDATED;
import static gov.ca.cwds.service.messages.MessageCode.USER_PARTIAL_UPDATE;
import static gov.ca.cwds.service.messages.MessageCode.USER_PARTIAL_UPDATE_AND_SAVE_TO_SEARCH_AND_DB_LOG_ERRORS;
import static gov.ca.cwds.service.messages.MessageCode.USER_PARTIAL_UPDATE_AND_SAVE_TO_SEARCH_ERRORS;
import static gov.ca.cwds.service.messages.MessageCode.USER_UPDATE_SAVE_TO_SEARCH_AND_DB_LOG_ERRORS;
import static gov.ca.cwds.service.messages.MessageCode.USER_UPDATE_SAVE_TO_SEARCH_ERROR;
import static gov.ca.cwds.util.Utils.URL_DATETIME_FORMATTER;
import static gov.ca.cwds.util.Utils.toLowerCase;
import static gov.ca.cwds.util.Utils.toUpperCase;
import static java.util.stream.Collectors.toSet;

import gov.ca.cwds.idm.dto.RegistrationResubmitResponse;
import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserAndOperation;
import gov.ca.cwds.idm.dto.UserIdAndOperation;
import gov.ca.cwds.idm.dto.UserUpdate;
import gov.ca.cwds.idm.dto.UserVerificationResult;
import gov.ca.cwds.idm.dto.UsersPage;
import gov.ca.cwds.idm.dto.UsersSearchCriteria;
import gov.ca.cwds.idm.event.AuditEvent;
import gov.ca.cwds.idm.event.CellPhoneChangedEvent;
import gov.ca.cwds.idm.event.EmailChangedEvent;
import gov.ca.cwds.idm.event.NotesChangedEvent;
import gov.ca.cwds.idm.event.PermissionsChangedEvent;
import gov.ca.cwds.idm.event.UserCreatedEvent;
import gov.ca.cwds.idm.event.UserEnabledStatusChangedEvent;
import gov.ca.cwds.idm.event.UserRegistrationResentEvent;
import gov.ca.cwds.idm.event.UserRoleChangedEvent;
import gov.ca.cwds.idm.event.WorkerPhoneChangedEvent;
import gov.ca.cwds.idm.exception.AdminAuthorizationException;
import gov.ca.cwds.idm.exception.UserValidationException;
import gov.ca.cwds.idm.persistence.ns.OperationType;
import gov.ca.cwds.idm.persistence.ns.entity.Permission;
import gov.ca.cwds.idm.persistence.ns.entity.UserLog;
import gov.ca.cwds.idm.service.authorization.AuthorizationService;
import gov.ca.cwds.idm.service.cognito.CognitoServiceFacade;
import gov.ca.cwds.idm.service.cognito.attribute.StandardUserAttribute;
import gov.ca.cwds.idm.service.diff.BooleanDiff;
import gov.ca.cwds.idm.service.diff.StringDiff;
import gov.ca.cwds.idm.service.diff.UpdateDifference;
import gov.ca.cwds.idm.service.exception.ExceptionFactory;
import gov.ca.cwds.idm.service.execution.OptionalExecution;
import gov.ca.cwds.idm.service.execution.OptionalExecution.NoUpdateExecution;
import gov.ca.cwds.idm.service.execution.PutInSearchExecution;
import gov.ca.cwds.idm.service.search.UserSearchService;
import gov.ca.cwds.idm.service.validation.ValidationService;
import gov.ca.cwds.service.messages.MessageCode;
import gov.ca.cwds.service.messages.MessagesService;
import gov.ca.cwds.util.Utils;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("idm")
@SuppressWarnings({"fb-contrib:EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS"})
public class IdmServiceImpl implements IdmService {

  private static final Logger LOGGER = LoggerFactory.getLogger(IdmServiceImpl.class);

  @Autowired
  private UserService userService;

  @Autowired
  private CognitoServiceFacade cognitoServiceFacade;

  @Autowired
  private MessagesService messages;

  @Autowired
  private UserLogService userLogService;

  @Autowired
  private AuthorizationService authorizeService;

  @Autowired
  private ValidationService validationService;

  @Autowired
  private ExceptionFactory exceptionFactory;

  @Autowired
  private AuditEventService auditService;

  @Autowired
  private TransactionalUserService transactionalUserService;

  @Autowired
  private DictionaryProvider dictionaryProvider;

  @Autowired
  private MessagesService messagesService;

  @Autowired
  private UserSearchService userSearchService;

  @Override
  public User findUser(String id) {
    User user = userService.getUser(id);
    authorizeService.checkCanViewUser(user);
    return user;
  }

  @Override
  public void updateUser(String userId, UserUpdate updateUserDto) {
    User existedUser = userService.getUser(userId);

    authorizeService.checkCanUpdateUser(existedUser, updateUserDto);
    validationService.validateUserUpdate(existedUser, updateUserDto);

    UserUpdateRequest userUpdateRequest =
        prepareUserUpdateRequest(existedUser, updateUserDto);

    ExecutionStatus updateAttributesStatus = updateUserAttributes(userUpdateRequest);

    OptionalExecution<BooleanDiff, Void> updateUserEnabledExecution =
        updateUserEnabledStatus(userUpdateRequest);
    if (updateAttributesStatus == WAS_NOT_EXECUTED
        && updateUserEnabledExecution.getExecutionStatus() == FAIL) {
      throw (RuntimeException) updateUserEnabledExecution.getException();
    }

    PutInSearchExecution<User> doraExecution = null;
    if (doesElasticSearchNeedUpdate(updateAttributesStatus, updateUserEnabledExecution)) {
      User updatedUser = userService.getUser(userId);
      doraExecution = userSearchService.updateUserInSearch(updatedUser);
    } else {
      LOGGER.info(messages.getTechMessage(USER_NOTHING_UPDATED, userId));
    }

    handleUpdatePartialSuccess(
        userId, updateAttributesStatus, updateUserEnabledExecution, doraExecution);
  }

  @Override
  public String createUser(User userDto) {
    userDto = userService.enrichWithCwsData(userDto);
    String email = toLowerCase(userDto.getEmail());
    userDto.setEmail(email);
    String racfId = toUpperCase(userDto.getRacfid());
    userDto.setRacfid(racfId);
    validationService.validateUserCreate(userDto);
    authorizeService.checkCanCreateUser(userDto);
    User createdUser = userService.createUser(userDto);
    LOGGER.info("New user with username:{} was successfully created in Cognito", createdUser.getId());
    transactionalUserService.createUserInDbWithInvitationEmail(createdUser);
    LOGGER.info("New user with username:{} was successfully created in database", createdUser.getId());
    auditService.saveAuditEvent(new UserCreatedEvent(createdUser));
    PutInSearchExecution doraExecution = userSearchService.createUserInSearch(createdUser);
    handleCreatePartialSuccess(createdUser, doraExecution);
    return createdUser.getId();
  }

  @Override
  public UsersPage getUserPage(String paginationToken) {
    return userService.getUserPage(paginationToken);
  }

  @Override
  public List<User> searchUsersInCognito(UsersSearchCriteria criteria) {
    return userService.searchUsers(criteria);
  }

  @Override
  public List<User> searchUsersByRacfids(Set<String> racfids) {
    return userService.searchUsersByRacfids(racfids);
  }

  @Override
  public List<UserAndOperation> getFailedOperations(LocalDateTime lastJobTime) {

    deleteProcessedLogs(lastJobTime);

    List<UserIdAndOperation> dbList = userLogService.getUserIdAndOperations(lastJobTime);

    return filterIdAndOperationList(dbList)
        .stream()
        .map(e -> new UserAndOperation(userService.getUser(e.getId()), e.getOperation()))
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(TOKEN_TRANSACTION_MANAGER)
  public RegistrationResubmitResponse resendInvitationMessage(User user) {
    authorizeService.checkCanResendInvitationMessage(user);
    cognitoServiceFacade.resendInvitationMessage(user.getId());
    auditService.saveAuditEvent(new UserRegistrationResentEvent(user));
    return new RegistrationResubmitResponse(user.getId());
  }

  @Override
  public void saveLastLoginTime(String userId, LocalDateTime loginTime) {
    try {
      if (userId == null) {
        LOGGER.warn("userToken doesn't contain the userId, no following actions expected}");
        return;
      }
      LOGGER.debug("Handling \"user logged in\" event for user {}", userId);
      userService.saveLastLoginTime(userId, loginTime);
      User user = userService.getUser(userId);
      userSearchService.updateUserInSearch(user);

    } catch (Exception e) {
      String msg = messagesService.getTechMessage(UNABLE_TO_WRITE_LAST_LOGIN_TIME, userId);
      LOGGER.error(msg, e);
    }
  }

  @Override
  @SuppressWarnings({"squid:S1166"})//Validation exceptions are already logged by ValidationService
  public UserVerificationResult verifyIfUserCanBeCreated(String racfId, String email) {
    User userDto = new User();
    userDto.setEmail(toLowerCase(email));
    userDto.setRacfid(toUpperCase(racfId));
    userDto.setRoles(Utils.toSet(CWS_WORKER));

    userDto = userService.enrichWithCwsData(userDto);

    try {
      validationService.validateVerifyIfUserCanBeCreated(userDto);
      authorizeService.checkCanCreateUser(userDto);
    } catch (UserValidationException | AdminAuthorizationException e) {
      return buildUserVerificationErrorResult(e.getErrorCode(), e.getUserMessage());
    }

    return UserVerificationResult.Builder.anUserVerificationResult().withUser(userDto)
        .withVerificationPassed().build();
  }

  private UserVerificationResult buildUserVerificationErrorResult(MessageCode code, String msg) {
    return UserVerificationResult.Builder.anUserVerificationResult()
        .withVerificationFailed(code.getValue(), msg).build();
  }

  private List<AuditEvent> createUserUpdateEvents(UserUpdateRequest userUpdateRequest) {
    User existedUser = userUpdateRequest.getExistedUser();
    UpdateDifference updateDifference = userUpdateRequest.getUpdateDifference();
    List<AuditEvent> auditEvents = new LinkedList<>();

    addRoleChangedEvent(existedUser, updateDifference, auditEvents);
    addNotesChangedEvent(existedUser, updateDifference, auditEvents);
    addPermissionsChangedEvent(existedUser, updateDifference, auditEvents);
    addEmailChangedEvent(existedUser, updateDifference, auditEvents);
    addCellPhoneChangedEvent(existedUser, updateDifference, auditEvents);
    addWorkerPhoneChangedEvent(existedUser, updateDifference, auditEvents);

    return auditEvents;
  }

  private void addWorkerPhoneChangedEvent(User existedUser, UpdateDifference updateDifference,
      List<AuditEvent> auditEvents) {
    Optional<StringDiff> optPhoneNumberDiff = updateDifference.getPhoneNumberDiff();
    Optional<StringDiff> optExtNumberDiff = updateDifference.getPhoneExtensionNumberDiff();
    if(optPhoneNumberDiff.isPresent() || optExtNumberDiff.isPresent()) {
      auditEvents.add(new WorkerPhoneChangedEvent(existedUser, optPhoneNumberDiff, optExtNumberDiff));
    }
  }

  private void addCellPhoneChangedEvent(User existedUser, UpdateDifference updateDifference,
      List<AuditEvent> auditEvents) {
    updateDifference.getCellPhoneNumberDiff().ifPresent(cellPhoneNumberDiff ->
        auditEvents.add(new CellPhoneChangedEvent(existedUser, cellPhoneNumberDiff))
    );
  }

  private void addEmailChangedEvent(User existedUser, UpdateDifference updateDifference,
      List<AuditEvent> auditEvents) {
    updateDifference.getEmailDiff().ifPresent(emailDiff ->
        auditEvents.add(new EmailChangedEvent(existedUser, emailDiff))
    );
  }

  private void addPermissionsChangedEvent(User existedUser, UpdateDifference updateDifference,
      List<AuditEvent> auditEvents) {
    updateDifference.getPermissionsDiff().ifPresent(permissionsDiff -> {
          List<Permission> permissions = dictionaryProvider.getPermissions();
          auditEvents.add(
              new PermissionsChangedEvent(existedUser, permissionsDiff, permissions));
        }
    );
  }

  private void addNotesChangedEvent(User existedUser, UpdateDifference updateDifference,
      List<AuditEvent> auditEvents) {
    updateDifference.getNotesDiff().ifPresent(notesDiff ->
        auditEvents.add(new NotesChangedEvent(existedUser, notesDiff)));
  }

  private void addRoleChangedEvent(User existedUser, UpdateDifference updateDifference,
      List<AuditEvent> auditEvents) {
    updateDifference.getRolesDiff().ifPresent(rolesDiff ->
        auditEvents.add(
            new UserRoleChangedEvent(existedUser, rolesDiff)));
  }

  private ExecutionStatus updateUserAttributes(UserUpdateRequest userUpdateRequest) {
    ExecutionStatus updateAttributesStatus = WAS_NOT_EXECUTED;

    if (transactionalUserService.updateUserAttributes(userUpdateRequest)) {
      updateAttributesStatus = SUCCESS;
      auditService.saveAuditEvents(createUserUpdateEvents(userUpdateRequest));
    }
    return updateAttributesStatus;
  }

  private void handleUpdatePartialSuccess(
      String userId,
      ExecutionStatus updateAttributesStatus,
      OptionalExecution<BooleanDiff, Void> updateUserEnabledExecution,
      PutInSearchExecution<User> doraExecution) {

    ExecutionStatus updateEnableStatus = updateUserEnabledExecution.getExecutionStatus();
    Exception updateEnableException = updateUserEnabledExecution.getException();

    ExecutionStatus doraStatus = WAS_NOT_EXECUTED;
    Exception doraException = null;
    ExecutionStatus logDbStatus = WAS_NOT_EXECUTED;
    Exception logDbException = null;

    if (doraExecution != null) {
      doraStatus = doraExecution.getExecutionStatus();
      doraException = doraExecution.getException();

      OptionalExecution<String, UserLog> userLogExecution = doraExecution.getUserLogExecution();
      if (userLogExecution != null) {
        logDbStatus = userLogExecution.getExecutionStatus();
        logDbException = userLogExecution.getException();
      }
    }

    if (updateAttributesStatus == SUCCESS && updateEnableStatus == FAIL) { // partial Cognito update
      handleUpdatePartialSuccessWithCognitoFail(
          userId, updateEnableException, doraStatus, doraException, logDbStatus, logDbException);
    } else { // no Cognito partial update
      handleUpdatePartialSuccessNoCognitoFail(
          userId, doraStatus, doraException, logDbStatus, logDbException);
    }
  }

  private void handleUpdatePartialSuccessWithCognitoFail(
      String userId,
      Exception updateEnableException,
      ExecutionStatus doraStatus,
      Exception doraException,
      ExecutionStatus logDbStatus,
      Exception logDbException) {

    if (doraStatus == SUCCESS) {
      throwPartialSuccessException(userId, UPDATE, USER_PARTIAL_UPDATE, updateEnableException);

    } else if (doraStatus == FAIL && logDbStatus == SUCCESS) {
      throwPartialSuccessException(
          userId,
          UPDATE,
          USER_PARTIAL_UPDATE_AND_SAVE_TO_SEARCH_ERRORS,
          updateEnableException,
          doraException);

    } else if (doraStatus == FAIL && logDbStatus == FAIL) {
      throwPartialSuccessException(
          userId,
          UPDATE,
          USER_PARTIAL_UPDATE_AND_SAVE_TO_SEARCH_AND_DB_LOG_ERRORS,
          updateEnableException,
          doraException,
          logDbException);
    }
  }

  private void handleUpdatePartialSuccessNoCognitoFail(
      String userId,
      ExecutionStatus doraStatus,
      Exception doraException,
      ExecutionStatus logDbStatus,
      Exception logDbException) {

    if (doraStatus == FAIL && logDbStatus == SUCCESS) {
      throwPartialSuccessException(userId, UPDATE, USER_UPDATE_SAVE_TO_SEARCH_ERROR, doraException);

    } else if (doraStatus == FAIL && logDbStatus == FAIL) {
      throwPartialSuccessException(
          userId, UPDATE, USER_UPDATE_SAVE_TO_SEARCH_AND_DB_LOG_ERRORS, doraException,
          logDbException);
    }
  }

  private OptionalExecution<BooleanDiff, Void> executeUpdateEnableStatusOptionally(
      User existedUser, BooleanDiff enabledDiff) {
    return new OptionalExecution<BooleanDiff, Void>(enabledDiff) {

      @Override
      public Void tryMethod(BooleanDiff enabledDiff) {
        cognitoServiceFacade
            .changeUserEnabledStatus(existedUser.getId(), enabledDiff.getNewValue());
        auditService.saveAuditEvent(
            new UserEnabledStatusChangedEvent(existedUser, enabledDiff));
        return null;
      }

      @Override
      protected void catchMethod(Exception e) {
        LOGGER.error(
            messages.getTechMessage(ERROR_UPDATE_USER_ENABLED_STATUS, existedUser.getId()), e);
      }
    };
  }

  private void handleCreatePartialSuccess(User user, PutInSearchExecution doraExecution) {
    if (doraExecution.getExecutionStatus() == FAIL) {
      OptionalExecution dbLogExecution = doraExecution.getUserLogExecution();

      if (dbLogExecution.getExecutionStatus() == SUCCESS) {
        throwPartialSuccessException(user.getId(), CREATE, USER_CREATE_SAVE_TO_SEARCH_ERROR,
            doraExecution.getException());
      } else { // logging in db failed
        throwPartialSuccessException(user.getId(), CREATE,
            USER_CREATE_SAVE_TO_SEARCH_AND_DB_LOG_ERRORS,
            doraExecution.getException(), dbLogExecution.getException());
      }
    }
  }

  private void deleteProcessedLogs(LocalDateTime lastJobTime) {
    int deletedCount = 0;
    try {
      deletedCount = userLogService.deleteProcessedLogs(lastJobTime);
    } catch (Exception e) {
      LOGGER.error(messages.getTechMessage(UNABLE_TO_PURGE_PROCESSED_USER_LOGS,
          lastJobTime.format(URL_DATETIME_FORMATTER)), e);
    }
    if (deletedCount > 0) {
      LOGGER.info("{} processed user log records are deleted", deletedCount);
    }
  }

  private void throwPartialSuccessException(
      String userId, OperationType operationType, MessageCode errorCode, Exception... causes) {
    throw exceptionFactory.createPartialSuccessException(userId, operationType, errorCode,
        causes);
  }

  private static List<UserIdAndOperation> filterIdAndOperationList(
      List<UserIdAndOperation> inputList) {
    Map<String, OperationType> idAndOperationMap = new HashMap<>();

    for (UserIdAndOperation userIdAndOperation : inputList) {
      String userId = userIdAndOperation.getId();
      OperationType operation = userIdAndOperation.getOperation();
      OperationType existedOperation = idAndOperationMap.get(userId);

      if (existedOperation == null || (existedOperation == CREATE && operation == UPDATE)) {
        idAndOperationMap.put(userId, operation);
      }
    }

    return idAndOperationMap
        .entrySet()
        .stream()
        .map(e -> new UserIdAndOperation(e.getKey(), e.getValue()))
        .collect(Collectors.toList());
  }

  static Set<String> transformSearchValues(Set<String> values, StandardUserAttribute searchAttr) {
    if (searchAttr == RACFID_STANDARD) {
      values = applyFunctionToValues(values, Utils::toUpperCase);
    } else if (searchAttr == EMAIL) {
      values = applyFunctionToValues(values, Utils::toLowerCase);
    }
    return values;
  }

  private static Set<String> applyFunctionToValues(Set<String> values,
      Function<String, String> function) {
    return values.stream().map(function).collect(toSet());
  }

  private UserUpdateRequest prepareUserUpdateRequest(User existedUser, UserUpdate updateUserDto) {
    UserUpdateRequest userUpdateRequest = new UserUpdateRequest();
    userUpdateRequest.setUserId(existedUser.getId());
    userUpdateRequest.setExistedUser(existedUser);
    UpdateDifference updateDifference = new UpdateDifference(existedUser, updateUserDto);
    userUpdateRequest.setUpdateDifference(updateDifference);
    return userUpdateRequest;
  }

  private OptionalExecution<BooleanDiff, Void> updateUserEnabledStatus(
      UserUpdateRequest userUpdateRequest) {
    OptionalExecution<BooleanDiff, Void> updateUserEnabledExecution;

    Optional<BooleanDiff> optEnabledDiff = userUpdateRequest.getUpdateDifference().getEnabledDiff();
    User existedUser = userUpdateRequest.getExistedUser();

    if (optEnabledDiff.isPresent()) {
      updateUserEnabledExecution = executeUpdateEnableStatusOptionally(existedUser,
          optEnabledDiff.get());
    } else {
      updateUserEnabledExecution = NoUpdateExecution.INSTANCE;
    }
    return updateUserEnabledExecution;
  }

  private boolean doesElasticSearchNeedUpdate(ExecutionStatus updateAttributesStatus,
      OptionalExecution<BooleanDiff, Void> updateUserEnabledExecution) {
    return updateAttributesStatus == SUCCESS
        || updateUserEnabledExecution.getExecutionStatus() == SUCCESS;
  }

  public void setCognitoServiceFacade(
      CognitoServiceFacade cognitoServiceFacade) {
    this.cognitoServiceFacade = cognitoServiceFacade;
  }

  public void setUserLogService(UserLogService userLogService) {
    this.userLogService = userLogService;
  }

  public void setMessages(MessagesService messages) {
    this.messages = messages;
  }

  public void setAuthorizeService(AuthorizationService authorizeService) {
    this.authorizeService = authorizeService;
  }

  public void setValidationService(ValidationService validationService) {
    this.validationService = validationService;
  }

  public void setTransactionalUserService(TransactionalUserService transactionalUserService) {
    this.transactionalUserService = transactionalUserService;
  }

  public void setAuditService(AuditEventService auditService) {
    this.auditService = auditService;
  }

  public void setUserService(UserService userService) {
    this.userService = userService;
  }
}
