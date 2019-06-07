package gov.ca.cwds.idm.event;

import static gov.ca.cwds.util.PhoneFormatter.formatPhone;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.service.diff.StringDiff;


@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class CellPhoneChangedEvent extends AdminCausedChangeLogEvent {

  private static final long serialVersionUID = -6356499019298731693L;

  public static final String EVENT_TYPE_CELL_PHONE_CHANGED = "Cell Phone Change";

  public CellPhoneChangedEvent(User existedUser, StringDiff cellPhoneDiff) {
    super(existedUser);
    setEventType(EVENT_TYPE_CELL_PHONE_CHANGED);
    setOldValue(formatPhone(cellPhoneDiff.getOldValue()));
    setNewValue(formatPhone(cellPhoneDiff.getNewValue()));
  }
}
