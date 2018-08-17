package gov.ca.cwds.idm.util;

import static io.dropwizard.testing.FixtureHelpers.fixture;

import java.io.IOException;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

public final class AssertFixtureUtils {

  private AssertFixtureUtils() {
  }

  public static void assertStrict(MvcResult result, String fixturePath) throws IOException, JSONException {
    assertWithFixture(result, fixturePath, JSONCompareMode.STRICT);
  }

  public static void assertNonStrict(MvcResult result, String fixturePath) throws IOException, JSONException {
    assertWithFixture(result, fixturePath, JSONCompareMode.NON_EXTENSIBLE);
  }

  public static void assertExtensible(MvcResult result, String fixturePath) throws IOException, JSONException {
    assertWithFixture(result, fixturePath, JSONCompareMode.LENIENT);
  }

  private static void assertWithFixture(MvcResult result, String fixturePath, JSONCompareMode mode) throws IOException, JSONException {
    MockHttpServletResponse response = result.getResponse();
    String strResponse = response.getContentAsString();
    JSONAssert.assertEquals(fixture(fixturePath), strResponse, mode);
  }
}
