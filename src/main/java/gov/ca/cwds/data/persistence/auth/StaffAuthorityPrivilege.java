package gov.ca.cwds.data.persistence.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import gov.ca.cwds.data.persistence.PersistentObject;
import gov.ca.cwds.data.persistence.cms.CmsPersistentObject;
import java.util.Date;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Type;

/**
 * {@link PersistentObject} representing a StaffPersonPrivilege
 *
 * @author CWDS API Team
 */

@Entity
@Table(name = "STF_PVLT")
public class StaffAuthorityPrivilege extends CmsPersistentObject {

  /**
   * Base serialization version. Increment per version of this class.
   */
  private static final long serialVersionUID = 1L;

  @Column(name = "CNTY_SPFCD")
  private String countySpecificCode;

  @Type(type = "date")
  @Column(name = "END_DT")
  private Date endDate;

  @JsonFormat(pattern = "HH:mm:ss")
  @Type(type = "time")
  @Column(name = "END_TIME")
  private Date endTime;

  @Column(name = "FKUSERID_T", length = CMS_ID_LEN)
  private String fkuseridT;

  @Id
  @Column(name = "IDENTIFIER", length = CMS_ID_LEN)
  private String id;

  @Column(name = "ATH_PRVC")
  private String levelOfAuthPrivilegeCode;

  @Type(type = "short")
  @Column(name = "LVL_PRVC")
  private Short levelOfAuthPrivilegeType;

  @Type(type = "date")
  @Column(name = "START_DT")
  private Date startDate;

  @JsonFormat(pattern = "HH:mm:ss")
  @Type(type = "time")
  @Column(name = "START_TIME")
  private Date startTime;

  /**
   * Default constructor. <p> Required for Hibernate
   */
  public StaffAuthorityPrivilege() {
    super();
  }

  /**
   * Constructor. Build from fields.
   *
   * @param countySpecificCode county code
   * @param endDate end date. null = active
   * @param endTime end time
   * @param fkuseridT FK to user id table
   * @param id The id
   * @param levelOfAuthPrivilegeCode The levelOfAuthPrivilegeCode
   * @param levelOfAuthPrivilegeType The levelOfAuthPrivilegeType
   * @param startDate The startDate
   * @param startTime The startTime
   */
  public StaffAuthorityPrivilege(String countySpecificCode, Date endDate, Date endTime,
      String fkuseridT, String id, String levelOfAuthPrivilegeCode, Short levelOfAuthPrivilegeType,
      Date startDate, Date startTime) {
    super();
    this.countySpecificCode = countySpecificCode;
    this.endDate = endDate;
    this.endTime = endTime;
    this.fkuseridT = fkuseridT;
    this.id = id;
    this.levelOfAuthPrivilegeCode = levelOfAuthPrivilegeCode;
    this.levelOfAuthPrivilegeType = levelOfAuthPrivilegeType;
    this.startDate = startDate;
    this.startTime = startTime;
  }

  /**
   * {@inheritDoc}
   *
   * @see PersistentObject#getPrimaryKey()
   */
  @Override
  public String getPrimaryKey() {
    return getId();
  }

  /**
   * @return county code
   */
  public String getCountySpecificCode() {
    return StringUtils.trimToEmpty(countySpecificCode);
  }

  /**
   * @return the end date. Null = currently active.
   */
  public Date getEndDate() {
    return endDate;
  }

  /**
   * @return the end time
   */
  public Date getEndTime() {
    return endTime;
  }

  /**
   * @return foreign key to the user table
   */
  public String getFkuseridT() {
    return StringUtils.trimToEmpty(fkuseridT);
  }

  /**
   * @return the primary key
   */
  public String getId() {
    return StringUtils.trimToEmpty(id);
  }

  /**
   * @return the levelOfAuthPrivilegeCode
   */
  public String getLevelOfAuthPrivilegeCode() {
    return StringUtils.trimToEmpty(levelOfAuthPrivilegeCode);
  }

  /**
   * @return the levelOfAuthPrivilegeType
   */
  public Short getLevelOfAuthPrivilegeType() {
    return levelOfAuthPrivilegeType;
  }

  /**
   * @return the startDate
   */
  public Date getStartDate() {
    return startDate;
  }

  /**
   * @return the startTime
   */
  public Date getStartTime() {
    return startTime;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof StaffAuthorityPrivilege)) {
      return false;
    }

    StaffAuthorityPrivilege that = (StaffAuthorityPrivilege) o;

    return new EqualsBuilder()
        .append(countySpecificCode, that.countySpecificCode)
        .append(endDate, that.endDate)
        .append(endTime, that.endTime)
        .append(fkuseridT, that.fkuseridT)
        .append(id, that.id)
        .append(levelOfAuthPrivilegeCode, that.levelOfAuthPrivilegeCode)
        .append(levelOfAuthPrivilegeType, that.levelOfAuthPrivilegeType)
        .append(startDate, that.startDate)
        .append(startTime, that.startTime)
        .append(getLastUpdatedId(), that.getLastUpdatedId())
        .append(getLastUpdatedTime(), that.getLastUpdatedTime())
        .isEquals();
  }

  @Override
  public final int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(countySpecificCode)
        .append(endDate)
        .append(endTime)
        .append(fkuseridT)
        .append(id)
        .append(levelOfAuthPrivilegeCode)
        .append(levelOfAuthPrivilegeType)
        .append(startDate)
        .append(startTime)
        .append(getLastUpdatedId())
        .append(getLastUpdatedTime())
        .toHashCode();
  }
}

