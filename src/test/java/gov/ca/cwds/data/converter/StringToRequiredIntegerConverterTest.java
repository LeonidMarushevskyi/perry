package gov.ca.cwds.data.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class StringToRequiredIntegerConverterTest {
  private StringToRequiredIntegerConverter converter = new StringToRequiredIntegerConverter();

  @Test
  public void testConvertingToEntity() {
    assertNull(converter.convertToEntityAttribute(0));
    assertNull(converter.convertToEntityAttribute(null));
    assertEquals("11", converter.convertToEntityAttribute(11));
  }

  @Test
  public void testConvertingToDbColumn() {
    assertEquals(Integer.valueOf(0), converter.convertToDatabaseColumn(null));
    assertEquals(Integer.valueOf(0), converter.convertToDatabaseColumn(""));
    assertEquals(Integer.valueOf(11), converter.convertToDatabaseColumn("11"));
  }
}
