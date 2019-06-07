package gov.ca.cwds.util;

import static gov.ca.cwds.util.PhoneFormatter.formatPhone;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Test;

public class PhoneFormatterTest {

  @Test
  public void testFormat() {
    assertThat(formatPhone("1234567890"), is("(123) 456-7890"));
    assertThat(formatPhone(null), nullValue());
    assertThat(formatPhone(""), is(""));
    assertThat(formatPhone("abc"), is("abc"));
    assertThat(formatPhone("123456789"), is("(123) 456-789"));
    assertThat(formatPhone("12345678"), is("(123) 456-78"));
    assertThat(formatPhone("1234567"), is("(123) 456-7"));
    assertThat(formatPhone("123456"), is("123456"));
    assertThat(formatPhone("12345678901"), is("(123) 456-78901"));
  }
}
