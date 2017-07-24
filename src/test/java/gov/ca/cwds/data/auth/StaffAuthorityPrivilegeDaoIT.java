package gov.ca.cwds.data.auth;

import gov.ca.cwds.data.persistence.auth.StaffAuthorityPrivilege;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@DataJpaTest(excludeAutoConfiguration = {FlywayAutoConfiguration.class})
@DirtiesContext
@ActiveProfiles("dev")
public class StaffAuthorityPrivilegeDaoIT {
  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private StaffAuthorityPrivilegeDao staffAuthorityPrivilegeDao;

  @Test
  public void testFindByUserId() {
    String userId = "userId";
    entityManager.merge(entity("id", userId, "P"));
    List<StaffAuthorityPrivilege> privileges = staffAuthorityPrivilegeDao.findByFkuseridT(userId);
    assertThat(privileges.size(), is(1));

  }

  @Test
  public void testFindSocialWorkerPrivs() {
    String userId = "userId";
    entityManager.merge(entity("1", userId, "P"));
    entityManager.merge(entity("2", userId, "C"));
    List<StaffAuthorityPrivilege> privileges = staffAuthorityPrivilegeDao.findSocialWorkerPrivileges(userId);
    assertThat(privileges.size(), is(1));
    assertThat(privileges.get(0).getId(), is("1"));

  }

  private StaffAuthorityPrivilege entity(String id, String userId, String code) {
    return new StaffAuthorityPrivilege("01", null, null,
            userId, id, code, (short) 1468, new Date(), new Date());
  }
}
