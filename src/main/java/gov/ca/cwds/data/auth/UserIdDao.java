package gov.ca.cwds.data.auth;

import gov.ca.cwds.data.persistence.auth.UserId;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * DAO for {@link UserId}.
 *
 * @author CWDS API Team
 */

@Repository
public interface UserIdDao extends ReadOnlyRepository<UserId, String> {

  @Query("SELECT U FROM UserId U WHERE U.logonId in :logonIds AND U.endDate is null")
  List<UserId> findActiveByLogonIdIn(@Param("logonIds") Collection<String> logonIds);

  @Query("SELECT u FROM UserId u "
      + "LEFT OUTER JOIN FETCH u.staffPerson sp "
      + "LEFT OUTER JOIN FETCH sp.unitAuthorities ua "
      + "LEFT OUTER JOIN FETCH ua.assignmentUnit "
      + "LEFT OUTER JOIN FETCH sp.office "
      + "LEFT OUTER JOIN FETCH u.privileges p "
      + "WHERE u.logonId = :logonId "
      + "AND u.endDate IS NULL ")
  Set<UserId> findActiveByLogonId(@Param("logonId") String logonId);


}
