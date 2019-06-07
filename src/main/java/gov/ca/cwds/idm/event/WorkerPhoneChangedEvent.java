package gov.ca.cwds.idm.event;

import static gov.ca.cwds.util.PhoneFormatter.formatPhone;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.service.diff.StringDiff;
import java.util.Objects;
import java.util.Optional;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class WorkerPhoneChangedEvent extends AdminCausedChangeLogEvent {

  public static final String EVENT_TYPE_WORKER_PHONE_CHANGED = "Worker Phone Change";
  public static final String EXTENSION_SIGN = "Ext ";

  public WorkerPhoneChangedEvent(User existedUser,
      Optional<StringDiff> optPhoneNumberDiff, Optional<StringDiff> optExtNumberDiff) {
    super(existedUser);
    setEventType(EVENT_TYPE_WORKER_PHONE_CHANGED);
    setOldValue(getOldValue(existedUser));
    setNewValue(getNewValue(existedUser, optPhoneNumberDiff, optExtNumberDiff));
  }

  private String getOldValue(User existedUser) {
    return getPhoneWithExtension(
        existedUser.getPhoneNumber(), existedUser.getPhoneExtensionNumber());
  }

  private String getNewValue(User existedUser, Optional<StringDiff> optPhoneNumberDiff,
      Optional<StringDiff> optExtNumberDiff) {

    String phoneNumber = getNewPhoneNumber(existedUser, optPhoneNumberDiff);
    String extensionNumber = getNewPhoneExtensionNumber(existedUser, optExtNumberDiff);

    return getPhoneWithExtension(phoneNumber, extensionNumber);
  }

  private String getNewPhoneNumber(User existedUser, Optional<StringDiff> optPhoneNumberDiff) {
    if(optPhoneNumberDiff.isPresent()) {
      return optPhoneNumberDiff.get().getNewValue();
    } else {
      return existedUser.getPhoneNumber();
    }
  }

  private String getNewPhoneExtensionNumber(User existedUser, Optional<StringDiff> optExtNumberDiff) {
    if(optExtNumberDiff.isPresent()) {
      return optExtNumberDiff.get().getNewValue();
    } else {
      return existedUser.getPhoneExtensionNumber();
    }
  }

  private String getPhoneWithExtension(String phone, String extension){
    String phoneStr = Objects.toString(phone, "");
    StringBuilder sb = new StringBuilder(formatPhone(phoneStr));

    if(extension != null) {
      if(!phoneStr.isEmpty()) {
        sb.append(' ');
      }
      sb.append(EXTENSION_SIGN).append(extension);
    }
    return sb.toString();
  }
}
