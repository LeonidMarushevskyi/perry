package gov.ca.cwds.data.persistence.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import gov.ca.cwds.data.persistence.PersistentObject;
import gov.ca.cwds.data.persistence.cms.CmsPersistentObject;
import java.util.Date;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.Where;

/**
 * {@link PersistentObject} representing a UserId
 *
 * @author CWDS API Team
 */

@Entity
@Table(name = "USERID_T")
public class UserId extends CmsPersistentObject {

  /**
   * Base serialization version. Increment per version of this class.
   */
  private static final long serialVersionUID = 1L;

  @Type(type = "date")
  @Column(name = "END_DT")
  private Date endDate;

  @JsonFormat(pattern = "HH:mm:ss")
  @Type(type = "time")
  @Column(name = "END_TM")
  private Date endTime;

  @Column(name = "FKFPSTFPRT", length = CMS_ID_LEN)
  private String fkfpstfprt;

  @Column(name = "FKSTFPERST", length = CMS_ID_LEN)
  private String staffPersonId;

  @Id
  @Column(name = "IDENTIFIER", length = CMS_ID_LEN)
  private String id;

  @Column(name = "LOGON_ID")
  private String logonId;

  @Type(type = "short")
  @Column(name = "SYS_DMC")
  private Short systemDomainType;

  @OneToMany
  @JoinColumn(name = "FKUSERID_T")
  @LazyCollection(LazyCollectionOption.FALSE)
  @Fetch(FetchMode.JOIN)
  @Where(clause = "END_DT IS NULL")
  private Set<StaffAuthorityPrivilege> privileges;

  @ManyToOne
  @JoinColumn(name = "FKSTFPERST", insertable = false, updatable = false)
  @LazyCollection(LazyCollectionOption.FALSE)
  @Fetch(FetchMode.JOIN)
  private StaffPerson staffPerson;

  /**
   * Default constructor <p> Required for Hibernate
   */
  public UserId() {
    super();
  }

  /**
   * Constructor
   *
   * @param endDate The endDate
   * @param endTime The endTime
   * @param fkfpstfprt The fkfpstfprt
   * @param staffPersonId The staffPersonId
   * @param id The id
   * @param logonId The logonId
   * @param systemDomainType The system domain type
   */
  public UserId(Date endDate, Date endTime, String fkfpstfprt, String staffPersonId, String id,
      String logonId, Short systemDomainType) {
    super();
    this.endDate = endDate;
    this.endTime = endTime;
    this.fkfpstfprt = fkfpstfprt;
    this.staffPersonId = staffPersonId;
    this.id = id;
    this.logonId = logonId;
    this.systemDomainType = systemDomainType;
  }

  public StaffPerson getStaffPerson() {
    return staffPerson;
  }

  public void setStaffPerson(StaffPerson staffPerson) {
    this.staffPerson = staffPerson;
  }

  public Set<StaffAuthorityPrivilege> getPrivileges() {
    return privileges;
  }

  public void setPrivileges(
      Set<StaffAuthorityPrivilege> privileges) {
    this.privileges = privileges;
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
   * @return the endDate
   */
  public Date getEndDate() {
    return endDate;
  }

  /**
   * @return the endTime
   */
  public Date getEndTime() {
    return endTime;
  }

  /**
   * @return the fkfpstfprt
   */
  public String getFkfpstfprt() {
    return StringUtils.trimToEmpty(fkfpstfprt);
  }

  /**
   * @return the staffPersonId
   */
  public String getStaffPersonId() {
    return StringUtils.trimToEmpty(staffPersonId);
  }

  /**
   * @return the id
   */
  public String getId() {
    return StringUtils.trimToEmpty(id);
  }

  /**
   * @return the logonId
   */
  public String getLogonId() {
    return StringUtils.trimToEmpty(logonId);
  }

  /**
   * @return the systemDomainType
   */
  public Short getSystemDomainType() {
    return systemDomainType;
  }


  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof UserId)) {
      return false;
    }

    UserId userId = (UserId) o;

    return new EqualsBuilder()
        .append(endDate, userId.endDate)
        .append(endTime, userId.endTime)
        .append(fkfpstfprt, userId.fkfpstfprt)
        .append(staffPersonId, userId.staffPersonId)
        .append(id, userId.id)
        .append(logonId, userId.logonId)
        .append(systemDomainType, userId.systemDomainType)
        .append(privileges, userId.privileges)
        .append(staffPerson, userId.staffPerson)
        .append(getLastUpdatedId(), userId.getLastUpdatedId())
        .append(getLastUpdatedTime(), userId.getLastUpdatedTime())
        .isEquals();
  }

  @Override
  public final int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(endDate)
        .append(endTime)
        .append(fkfpstfprt)
        .append(staffPersonId)
        .append(id)
        .append(logonId)
        .append(systemDomainType)
        .append(privileges)
        .append(staffPerson)
        .append(getLastUpdatedId())
        .append(getLastUpdatedTime())
        .toHashCode();
  }
}
