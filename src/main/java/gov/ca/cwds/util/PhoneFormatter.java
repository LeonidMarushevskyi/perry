package gov.ca.cwds.util;

import java.util.regex.Pattern;

public class PhoneFormatter {

  private static final Pattern REPLACE_PATTERN = Pattern.compile("(\\d{3})(\\d{3})(\\d+)");

  private PhoneFormatter() {}

  public static String formatPhone(String digitsStr) {
    if(digitsStr == null) {
      return null;
    }
    return REPLACE_PATTERN.matcher(digitsStr).replaceFirst("($1) $2-$3");
  }
}
