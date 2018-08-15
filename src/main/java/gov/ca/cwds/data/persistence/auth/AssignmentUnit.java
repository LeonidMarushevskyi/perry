package gov.ca.cwds.data.persistence.auth;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gov.ca.cwds.data.persistence.cms.CmsPersistentObject;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Type;

/**
 * {@link CmsPersistentObject} representing an Assignment Unit
 *
 * @author CWDS API Team
 */
@Entity
@Table(name = "ASG_UNIT")
public class AssignmentUnit extends CmsPersistentObject {

  /**
   * Base serialization version. Increment per version of this class.
   */
  private static final long serialVersionUID = 1L;
  @Column(name = "PHONE_NO")
  private Long phoneNo;

  @Column(name = "TEL_EXT_NO")
  private int telExtNo;

  @Column(name = "ASGDSK_IND")
  private String assignmentUnitDeskIndicator;

  @Column(name = "CNTY_SPFCD")
  private String countySpecificCode;

  @Type(type = "date")
  @Column(name = "END_DT")
  private Date endDate;

  @Column(name = "FKCWS_OFFT")
  private String cwsOfficeId;

  @Column(name = "ASGMUNT_NM")
  private String assignmentUnitName;

  @Type(type = "date")
  @Column(name = "START_DT")
  private Date startDate;

  @Id
  @Column(name = "IDENTIFIER")
  private String id;

  /**
   * Default constructor <p> Required for Hibernate
   */
  public AssignmentUnit() {
    super();
  }


  /**
   * @param phoneNo the phone number
   * @param telExtNo the tel ext number
   * @param assignmentUnitDeskIndicator the assignmentUnitDesk Indicator
   * @param countySpecificCode the countySpecificCode
   * @param endDate the end date
   * @param cwsOfficeId the cwsOfficeId
   * @param assignmentUnitName the assignmentUnitName
   * @param startDate the startDate
   * @param id the id
   */
  public AssignmentUnit(Long phoneNo, int telExtNo, String assignmentUnitDeskIndicator,
      String countySpecificCode, Date endDate, String cwsOfficeId, String assignmentUnitName,
      Date startDate, String id) {
    super();
    this.phoneNo = phoneNo;
    this.telExtNo = telExtNo;
    this.assignmentUnitDeskIndicator = assignmentUnitDeskIndicator;
    this.countySpecificCode = countySpecificCode;
    this.endDate = endDate;
    this.cwsOfficeId = cwsOfficeId;
    this.assignmentUnitName = assignmentUnitName;
    this.startDate = startDate;
    this.id = id;
  }


  /**
   * @return the phoneNo
   */
  public Long getPhoneNo() {
    return phoneNo;
  }


  /**
   * @return the telExtNo
   */
  public int getTelExtNo() {
    return telExtNo;
  }


  /**
   * @return the asgdskInd
   */
  public String getAssignmentUnitDeskIndicator() {
    return assignmentUnitDeskIndicator;
  }


  /**
   * @return the cwsOfficeId
   */
  public String getCwsOfficeId() {
    return cwsOfficeId;
  }


  /**
   * @return the assignmentNumber
   */
  public String getAssignmentUnitName() {
    return assignmentUnitName;
  }


  /**
   * @return the id
   */
  public String getId() {
    return id;
  }


  /**
   * {@inheritDoc}
   *
   * @see gov.ca.cwds.data.persistence.PersistentObject#getPrimaryKey()
   */
  @Override
  public String getPrimaryKey() {
    return getId();
  }

  /**
   * @return the countySpecificCode
   */
  public String getCountySpecificCode() {
    return StringUtils.trimToEmpty(countySpecificCode);
  }

  /**
   * @return the endDate
   */
  public Date getEndDate() {
    return endDate;
  }


  /**
   * @return the startDate
   */
  public Date getStartDate() {
    return startDate;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof AssignmentUnit)) {
      return false;
    }

    AssignmentUnit that = (AssignmentUnit) o;

    return new EqualsBuilder()
        .append(telExtNo, that.telExtNo)
        .append(phoneNo, that.phoneNo)
        .append(assignmentUnitDeskIndicator, that.assignmentUnitDeskIndicator)
        .append(countySpecificCode, that.countySpecificCode)
        .append(endDate, that.endDate)
        .append(cwsOfficeId, that.cwsOfficeId)
        .append(assignmentUnitName, that.assignmentUnitName)
        .append(startDate, that.startDate)
        .append(id, that.id)
        .append(getLastUpdatedId(), that.getLastUpdatedId())
        .append(getLastUpdatedTime(), that.getLastUpdatedTime())
        .isEquals();
  }

  @Override
  public final int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(phoneNo)
        .append(telExtNo)
        .append(assignmentUnitDeskIndicator)
        .append(countySpecificCode)
        .append(endDate)
        .append(cwsOfficeId)
        .append(assignmentUnitName)
        .append(startDate)
        .append(id)
        .append(getLastUpdatedId())
        .append(getLastUpdatedTime())
        .toHashCode();
  }
}
