package gov.ca.cwds.idm.service.execution;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import gov.ca.cwds.idm.service.OperationResultType;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class OptionalExecutionTest {

  @Test
  public void testSuccess() {
    OptionalExecution<String, String> execution =
        new OptionalExecution<String, String>("abc") {

          @Override
          protected String tryMethod(String str) {
            return str.toUpperCase();
          }

          @Override
          protected void catchMethod(Exception e) {
            System.out.println(e.getMessage());
          }
        };

    assertThat(execution.getResultType(), CoreMatchers.is(OperationResultType.SUCCESS));
    assertThat(execution.getException(), nullValue());
    assertThat(execution.getResponse(), is("ABC"));
  }

  @Test
  public void testFail() {

    OptionalExecution<Integer, Integer> execution =
        new OptionalExecution<Integer, Integer>(0) {

          @Override
          protected Integer tryMethod(Integer i) {
            return 1 / i;
          }

          @Override
          protected void catchMethod(Exception e) {
            System.out.println(e.getMessage());
          }
        };

    assertThat(execution.getResultType(), is(OperationResultType.FAIL));
    assertThat(execution.getException(), notNullValue());
    assertTrue(execution.getException() instanceof ArithmeticException);
    assertThat(execution.getResponse(), nullValue());
  }
}
