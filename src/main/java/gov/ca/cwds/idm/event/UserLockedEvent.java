package gov.ca.cwds.idm.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import gov.ca.cwds.idm.dto.User;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class UserLockedEvent extends SystemCausedChangeLogEvent {

  public static final String EVENT_TYPE_USER_LOCKED = "Account Locked";
  static final String LOCKED = "Locked";
  static final String UNLOCKED = "Unlocked";

  private static final long serialVersionUID = -7798448624678260240L;

  public UserLockedEvent(User user) {
    super(user);
    setEventType(EVENT_TYPE_USER_LOCKED);
    setOldValue(UNLOCKED); // this action is external, we cannot know for sure
    setNewValue(LOCKED);
  }
}
