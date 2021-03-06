package gov.ca.cwds.idm.persistence.ns.repository;

import gov.ca.cwds.idm.dto.UserIdAndOperation;
import gov.ca.cwds.idm.persistence.ns.entity.UserLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Profile("idm")
@Repository
@SuppressWarnings({"squid:S1214"})//implementation details are queries and they are put here by Spring Data design
public interface UserLogRepository extends CrudRepository<UserLog, Long> {

  String LAST_DATE = "lastDate";

  @Override
  UserLog save(UserLog entity);

  @Query(
      "select new gov.ca.cwds.idm.dto.UserIdAndOperation(u.username, u.operationType) from UserLog u "
          + "where u.operationTime > :" + LAST_DATE
          + " group by u.username, u.operationType")
  List<UserIdAndOperation> getUserIdAndOperationTypes(@Param(LAST_DATE) LocalDateTime lastDate);

  @Query("delete from UserLog u where u.operationTime <= :" + LAST_DATE)
  @Modifying
  int deleteLogsBeforeDate(@Param(LAST_DATE) LocalDateTime lastDate);
}
