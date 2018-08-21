package gov.ca.cwds.idm.service.execution;

import gov.ca.cwds.idm.service.OperationResultType;

/**
 * Class to encapsulate optional execution of some logic i.e. execution which in case of some
 * exception does not fail entire process but logs this exception in internal variable.
 * Logic is executed in constructor so you need to implement abstract tryMethod and catchMethod methods
 * and create new OptionalExecution instance passing tryMethod input as a constructor parameter.
 * @param <T> input type
 * @param <R> response type
 */
public abstract class OptionalExecution<T, R> {

  private OperationResultType resultType;
  private Exception exception;
  private R response;

  @SuppressWarnings({"fb-contrib:PCOA_PARTIALLY_CONSTRUCTED_OBJECT_ACCESS"})
  //implementations of tryMethod() should not use resultType field by design
  public OptionalExecution(T input){
    try {
      response = tryMethod(input);
      resultType = OperationResultType.SUCCESS;

    } catch (Exception e) {
      resultType = OperationResultType.FAIL;
      exception = e;
      catchMethod(e);
    }
  }

  /**
   * Put main logic here. This method is executed internally it try/cath block
   * which catches all exceptions.
   * If this logic does not produce any response use null as return value
   * and 'Void" as response generic type.
   */
  protected abstract R tryMethod(T input);

  /**
   * Put some logic which should be executed in case of exception in tryMethod.
   * It should be a code which is not supposed to throw an exception by itself.
   * Usually it's a some sort of error logging.
   * Other optional execution can be started from these method.
   * @param e, Exception thrown by main logic in tryMethod method
   */
  protected abstract void catchMethod(Exception e);

  /**
   * @return SUCCESS if tryMethod was executed successfully or FAIL otherwise.
   */
  public OperationResultType getResultType() {
    return resultType;
  }

  /**
   *  @return Exception thrown in tryMethod or null if execution was successful.
   */
  public Exception getException() {
    return exception;
  }

  /**
   * @return result of tryMethod if it was executed successfully or null otherwise.
   */
  public R getResponse() {
    return response;
  }
}
