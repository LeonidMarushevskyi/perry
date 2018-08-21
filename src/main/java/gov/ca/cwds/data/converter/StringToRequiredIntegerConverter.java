package gov.ca.cwds.data.converter;

import javax.persistence.AttributeConverter;
import org.apache.commons.lang3.StringUtils;

/**
 * @author CWDS TPT-3 Team
 *     <p>This converter converts string value to long and use zero as default value
 */
public class StringToRequiredIntegerConverter implements AttributeConverter<String, Integer> {

  @Override
  public Integer convertToDatabaseColumn(String string) {
    if (StringUtils.isBlank(string)) {
      return 0;
    }
    return Integer.valueOf(string);
  }

  @Override
  public String convertToEntityAttribute(Integer dbData) {
    if (dbData == null || dbData == 0) {
      return null;
    }
    return String.valueOf(dbData);
  }

}
