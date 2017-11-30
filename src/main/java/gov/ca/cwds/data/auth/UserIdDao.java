package gov.ca.cwds.data.auth;

import gov.ca.cwds.data.persistence.auth.UserId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

/**
 * DAO for {@link UserId}.
 *
 * @author CWDS API Team
 */
@Transactional
@Repository
public interface UserIdDao extends ReadOnlyRepository<UserId, String> {

  @Query("SELECT U FROM UserId U WHERE U.logonId = :logonId AND U.endDate is null")
  List<UserId> findActiveByLogonId(@Param("logonId") String logonId);

}
