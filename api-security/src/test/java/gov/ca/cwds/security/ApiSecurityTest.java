package gov.ca.cwds.security;

import com.google.inject.Inject;
import gov.ca.cwds.testapp.domain.Case;
import gov.ca.cwds.testapp.domain.CaseDTO;
import gov.ca.cwds.testapp.service.TestService;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Created by dmitry.rudenko on 10/6/2017.
 */
public class ApiSecurityTest extends AbstractApiSecurityTest {

  @Inject
  TestService testService;


  @Before
  public void before() throws Exception {
    initInjector();
  }

  @Test(expected = UnauthorizedException.class)
  public void testUnauthorized() throws Exception {
    testService.testArg("2");
  }

  @Test
  public void testAuthorized() throws Exception {
    testService.testArg("1");
  }

  @Test
  public void testArgAuthorizedCompositeObject() throws Exception {
    CaseDTO caseDTO = new CaseDTO();
    Case caseObject = new Case(1L, "name");
    caseDTO.setCaseObject(caseObject);
    testService.testCompositeObject(caseDTO);
  }

  @Test(expected = UnauthorizedException.class)
  public void testArgUnauthorizedCompositeObject() throws Exception {
    CaseDTO caseDTO = new CaseDTO();
    Case caseObject = new Case(2L, "name");
    caseDTO.setCaseObject(caseObject);
    testService.testCompositeObject(caseDTO);
  }

  @Test
  public void testArgAuthorizedCompositeObjectList() throws Exception {
    CaseDTO caseDTO = new CaseDTO();
    Case caseObject = new Case(1L, "name");
    caseDTO.getCases().add(caseObject);
    caseDTO.getCases().add(new Case(1L, "name"));
    testService.testCompositeObjectList(caseDTO);
  }

  @Test(expected = UnauthorizedException.class)
  public void testArgUnauthorizedCompositeObjectList() throws Exception {
    CaseDTO caseDTO = new CaseDTO();
    Case caseObject = new Case(1L, "name");
    caseDTO.getCases().add(caseObject);
    caseDTO.getCases().add(new Case(2L, "name"));
    testService.testCompositeObjectList(caseDTO);
  }

  @Test
  public void testRetAuthorizedCompositeObject() throws Exception {
    CaseDTO caseDTO = testService.testReturnInstance();
    assert  caseDTO != null;
  }

  @Test(expected = UnauthorizedException.class)
  public void testRetUnauthorizedCompositeObject() throws Exception {
    testService.testReturnProtectedInstance();
  }

  @Test
  public void testFilter() {
    List<CaseDTO> result = testService.testFilter();
    assert result.size() == 1;
    assert result.iterator().next().getCaseObject().getName().equals("valid");

  }
}
