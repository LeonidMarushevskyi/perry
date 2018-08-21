package gov.ca.cwds.idm.service;

import static gov.ca.cwds.idm.persistence.model.OperationType.CREATE;
import static gov.ca.cwds.idm.persistence.model.OperationType.UPDATE;
import static gov.ca.cwds.service.messages.MessageCode.UNABLE_LOG_IDM_USER;
import static gov.ca.cwds.service.messages.MessageCode.UNABLE_LOG_IDM_USER_CREATE;
import static gov.ca.cwds.service.messages.MessageCode.UNABLE_LOG_IDM_USER_UPDATE;

import gov.ca.cwds.idm.dto.UserIdAndOperation;
import gov.ca.cwds.idm.persistence.UserLogRepository;
import gov.ca.cwds.idm.persistence.model.OperationType;
import gov.ca.cwds.idm.persistence.model.UserLog;
import gov.ca.cwds.idm.service.execution.OptionalExecution;
import gov.ca.cwds.service.messages.MessagesService;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("idm")
public class UserLogService {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserLogService.class);

  private UserLogRepository userLogRepository;
  private MessagesService messages;

  public OptionalExecution<String, UserLog> logCreate(String username) {
    return log(username, CREATE);
  }

  public OptionalExecution<String, UserLog> logUpdate(String username) {
    return log(username, UPDATE);
  }

  @SuppressWarnings({"fb-contrib:CLI_CONSTANT_LIST_INDEX"})
  public List<UserIdAndOperation> getUserIdAndOperations(Date lastJobTime) {
    if (lastJobTime == null) {
      throw new IllegalArgumentException("Last Job Time cannot be null");
    }

    List<Object[]> iDAndOperationPairs = userLogRepository.getUserIdAndOperationTypes(lastJobTime);

    return iDAndOperationPairs
        .stream()
        .map(e -> new UserIdAndOperation((String) e[0], (OperationType) e[1]))
        .collect(Collectors.toList());
  }

  private OptionalExecution<String, UserLog> log(String username, OperationType operationType) {

    return new OptionalExecution<String, UserLog>(username){
      @Override
      protected UserLog tryMethod(String username) {
        UserLog userLog = new UserLog();
        userLog.setUsername(username);
        userLog.setOperationType(operationType);
        userLog.setOperationTime(new Date());

        return userLogRepository.save(userLog);
      }
      @Override
      protected void catchMethod(Exception e) {
        String msg = getErrorMessage(username, operationType);
        LOGGER.error(msg, e);
      }
    };
  }

  private String getErrorMessage(String username, OperationType operationType) {
    String msg;
    if (operationType == OperationType.CREATE) {
      msg = messages.get(UNABLE_LOG_IDM_USER_CREATE, username);
    } else if (operationType == OperationType.UPDATE) {
      msg = messages.get(UNABLE_LOG_IDM_USER_UPDATE, username);
    } else {
      msg = messages.get(UNABLE_LOG_IDM_USER, operationType.toString(), username);
    }
    return msg;
  }

  @Autowired
  public void setUserLogRepository(UserLogRepository userLogRepository) {
    this.userLogRepository = userLogRepository;
  }

  @Autowired
  public void setMessages(MessagesService messages) {
    this.messages = messages;
  }
}
