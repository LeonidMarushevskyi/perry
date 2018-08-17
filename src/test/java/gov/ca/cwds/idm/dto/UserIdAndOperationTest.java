package gov.ca.cwds.idm.dto;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import gov.ca.cwds.idm.persistence.model.OperationType;
import org.junit.Test;

public class UserIdAndOperationTest {

  @Test
  public void testEqualsAndHashCode() {
    UserIdAndOperation idOp0 = new UserIdAndOperation("a", OperationType.CREATE);
    UserIdAndOperation idOp1 = new UserIdAndOperation("a", OperationType.CREATE);
    UserIdAndOperation idOp2 = new UserIdAndOperation("b", OperationType.CREATE);
    UserIdAndOperation idOp3 = new UserIdAndOperation("a", OperationType.UPDATE);
    UserIdAndOperation idOp4 = idOp0;

    assertTrue(idOp0.equals(idOp4));

    assertFalse(idOp0.equals("abc"));

    assertTrue(idOp0 != idOp1);
    assertTrue(idOp0.equals(idOp1));
    assertTrue(idOp0.hashCode() == idOp1.hashCode() );

    assertTrue(idOp0 != idOp2);
    assertFalse(idOp0.equals(idOp2));
    assertTrue(idOp0.hashCode() != idOp2.hashCode() );

    assertTrue(idOp0 != idOp3);
    assertFalse(idOp0.equals(idOp3));
    assertTrue(idOp0.hashCode() != idOp3.hashCode() );
  }
}
