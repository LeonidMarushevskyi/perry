package gov.ca.cwds.data.persistence.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gov.ca.cwds.data.persistence.cms.CmsPersistentObject;
import io.dropwizard.jackson.JsonSnakeCase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
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
 * {@link CmsPersistentObject} representing a CWS Office.
 *
 * @author CWDS API Team
 */

@Entity
@Table(name = "CWS_OFFT")
@ApiModel
@JsonSnakeCase
public class CwsOffice extends CmsPersistentObject {

  /**
   * Base serialization version. Increment per version of this class.
   */
  private static final long serialVersionUID = 1L;

  @Id
  @Column(name = "IDENTIFIER")
  @ApiModelProperty(example = "Office")
  @JsonProperty("office_id")
  private String officeId;

  @Type(type = "short")
  @Column(name = "GVR_ENTC")
  @ApiModelProperty(example = "1011")
  @JsonProperty("government_entity_type")
  private Short governmentEntityType;

  @Column(name = "FAX_NO")
  @JsonIgnore
  private Long faxNumber;

  @Column(name = "GEO_RGNTCD")
  @JsonIgnore
  private String geographicRegionTextCode;

  @Column(name = "HDQRTR_IND")
  @JsonIgnore
  private String headquarterIndicator;

  @Column(name = "INACTV_IND")
  @JsonIgnore
  private String inactiveIndicator;

  @Column(name = "MAILST_DSC")
  @JsonIgnore
  private String mailStopDescription;

  @Column(name = "MSG_TEL_NO")
  @JsonIgnore
  private Long messagePhoneNumber;

  @Type(type = "integer")
  @Column(name = "MSG_EXT_NO")
  @JsonIgnore
  private Integer messagePhoneExtensionNumber;

  @Column(name = "CWS_OFF_NO")
  @JsonIgnore
  private String cwsOffNumber;

  @Column(name = "PRM_TEL_NO")
  @JsonIgnore
  private Long primaryPhoneNumber;

  @Type(type = "integer")
  @Column(name = "PRM_EXT_NO")
  @JsonIgnore
  private Integer primaryPhoneExtensionNumber;

  @Column(name = "FKSTFPERST")
  @JsonIgnore
  private String staffPersonId;

  @Column(name = "COMNT_DSC")
  @JsonIgnore
  private String commentDescription;

  @Column(name = "AGENCY_NM")
  @JsonIgnore
  private String agencyName;

  @Column(name = "DPT_DIV_NM")
  @JsonIgnore
  private String departmentDivisionName;

  @Column(name = "CWS_OFF_NM")
  @JsonIgnore
  private String cwsOfficeName;

  @Column(name = "CNTY_SPFCD")
  @JsonIgnore
  private String countySpecificCode;

  @Type(type = "short")
  @Column(name = "AGCY_CD_NO")
  @JsonIgnore
  private Short agencyCodeNumber;

  @Type(type = "short")
  @Column(name = "LOC_CNTY")
  @JsonIgnore
  private Short locationCountyType;

  @Column(name = "DIR_NM_TL")
  @JsonIgnore
  private String directorsNameTitle;

  /**
   * Default constructor <p> Required for Hibernate
   */
  public CwsOffice() {
    super();
  }

  /**
   * @param officeId the primary key
   * @param faxNumber the faxNumber
   * @param geographicRegionTextCode geographicRegionTextCode
   * @param governmentEntityType governmentEntityType
   * @param headquarterIndicator headquarterIndicator
   * @param inactiveIndicator inactiveIndicator
   * @param mailStopDescription mailStopDescription
   * @param messagePhoneNumber messagePhoneNumber
   * @param messagePhoneExtensionNumber the messagePhoneExtensionNumber
   * @param cwsOffNumber the cwsOffNumber
   * @param primaryPhoneNumber the primaryPhoneNumber
   * @param primaryPhoneExtensionNumber the primaryPhoneExtensionNumber
   * @param staffPersonId the staffPersonId
   * @param commentDescription the commentDescription
   * @param agencyName the agencyName
   * @param departmentDivisionName the departmentDivisionName
   * @param cwsOfficeName the cwsOfficeName
   * @param countySpecificCode the countySpecificCode
   * @param agencyCodeNumber the agencyCodeNumber
   * @param locationCountyType the locationCountyType
   * @param directorsNameTitle the directorsNameTitle
   */
  public CwsOffice(String officeId, Long faxNumber, String geographicRegionTextCode,
      Short governmentEntityType, String headquarterIndicator, String inactiveIndicator,
      String mailStopDescription, Long messagePhoneNumber,
      Integer messagePhoneExtensionNumber, String cwsOffNumber, Long primaryPhoneNumber,
      Integer primaryPhoneExtensionNumber, String staffPersonId, String commentDescription,
      String agencyName, String departmentDivisionName, String cwsOfficeName,
      String countySpecificCode, Short agencyCodeNumber, Short locationCountyType,
      String directorsNameTitle) {
    super();
    this.officeId = officeId;
    this.faxNumber = faxNumber;
    this.geographicRegionTextCode = geographicRegionTextCode;
    this.governmentEntityType = governmentEntityType;
    this.headquarterIndicator = headquarterIndicator;
    this.inactiveIndicator = inactiveIndicator;
    this.mailStopDescription = mailStopDescription;
    this.messagePhoneNumber = messagePhoneNumber;
    this.messagePhoneExtensionNumber = messagePhoneExtensionNumber;
    this.cwsOffNumber = cwsOffNumber;
    this.primaryPhoneNumber = primaryPhoneNumber;
    this.primaryPhoneExtensionNumber = primaryPhoneExtensionNumber;
    this.staffPersonId = staffPersonId;
    this.commentDescription = commentDescription;
    this.agencyName = agencyName;
    this.departmentDivisionName = departmentDivisionName;
    this.cwsOfficeName = cwsOfficeName;
    this.countySpecificCode = countySpecificCode;
    this.agencyCodeNumber = agencyCodeNumber;
    this.locationCountyType = locationCountyType;
    this.directorsNameTitle = directorsNameTitle;
  }


  @Override
  public Serializable getPrimaryKey() {
    return getOfficeId();
  }

  /**
   * @return the officeId
   */
  public String getOfficeId() {
    return officeId;
  }

  /**
   * @return the faxNumber
   */
  public Long getFaxNumber() {
    return faxNumber;
  }

  /**
   * @return the geographicRegionTextCode
   */
  public String getGeographicRegionTextCode() {
    return geographicRegionTextCode;
  }

  /**
   * @return the governmentEntityType
   */
  public Short getGovernmentEntityType() {
    return governmentEntityType;
  }

  /**
   * @return the headquarterIndicator
   */
  public String getHeadquarterIndicator() {
    return headquarterIndicator;
  }

  /**
   * @return the inactiveIndicator
   */
  public String getInactiveIndicator() {
    return inactiveIndicator;
  }

  /**
   * @return the mailStopDescription
   */
  public String getMailStopDescription() {
    return mailStopDescription;
  }

  /**
   * @return the messagePhoneNumber
   */
  public Long getMessagePhoneNumber() {
    return messagePhoneNumber;
  }

  /**
   * @return the messagePhoneExtensionNumber
   */
  public Integer getMessagePhoneExtensionNumber() {
    return messagePhoneExtensionNumber;
  }

  /**
   * @return the cwsOffNumber
   */
  public String getCwsOffNumber() {
    return cwsOffNumber;
  }

  /**
   * @return the primaryPhoneNumber
   */
  public Long getPrimaryPhoneNumber() {
    return primaryPhoneNumber;
  }

  /**
   * @return the primaryPhoneExtensionNumber
   */
  public Integer getPrimaryPhoneExtensionNumber() {
    return primaryPhoneExtensionNumber;
  }

  /**
   * @return the staffPersonId
   */
  public String getStaffPersonId() {
    return staffPersonId;
  }

  /**
   * @return the commentDescription
   */
  public String getCommentDescription() {
    return commentDescription;
  }

  /**
   * @return the agencyName
   */
  public String getAgencyName() {
    return agencyName;
  }

  /**
   * @return the departmentDivisionName
   */
  public String getDepartmentDivisionName() {
    return departmentDivisionName;
  }

  /**
   * @return the cwsOfficeName
   */
  public String getCwsOfficeName() {
    return StringUtils.trimToEmpty(cwsOfficeName);
  }

  /**
   * @return the countySpecificCode
   */
  public String getCountySpecificCode() {
    return countySpecificCode;
  }

  /**
   * @return the agencyCodeNumber
   */
  public Short getAgencyCodeNumber() {
    return agencyCodeNumber;
  }

  /**
   * @return the locationCountyType
   */
  public Short getLocationCountyType() {
    return locationCountyType;
  }

  /**
   * @return the directorsNameTitle
   */
  public String getDirectorsNameTitle() {
    return directorsNameTitle;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof CwsOffice)) {
      return false;
    }

    CwsOffice cwsOffice = (CwsOffice) o;

    return new EqualsBuilder()
        .append(officeId, cwsOffice.officeId)
        .append(governmentEntityType, cwsOffice.governmentEntityType)
        .append(faxNumber, cwsOffice.faxNumber)
        .append(geographicRegionTextCode, cwsOffice.geographicRegionTextCode)
        .append(headquarterIndicator, cwsOffice.headquarterIndicator)
        .append(inactiveIndicator, cwsOffice.inactiveIndicator)
        .append(mailStopDescription, cwsOffice.mailStopDescription)
        .append(messagePhoneNumber, cwsOffice.messagePhoneNumber)
        .append(messagePhoneExtensionNumber, cwsOffice.messagePhoneExtensionNumber)
        .append(cwsOffNumber, cwsOffice.cwsOffNumber)
        .append(primaryPhoneNumber, cwsOffice.primaryPhoneNumber)
        .append(primaryPhoneExtensionNumber, cwsOffice.primaryPhoneExtensionNumber)
        .append(staffPersonId, cwsOffice.staffPersonId)
        .append(commentDescription, cwsOffice.commentDescription)
        .append(agencyName, cwsOffice.agencyName)
        .append(departmentDivisionName, cwsOffice.departmentDivisionName)
        .append(cwsOfficeName, cwsOffice.cwsOfficeName)
        .append(countySpecificCode, cwsOffice.countySpecificCode)
        .append(agencyCodeNumber, cwsOffice.agencyCodeNumber)
        .append(locationCountyType, cwsOffice.locationCountyType)
        .append(directorsNameTitle, cwsOffice.directorsNameTitle)
        .append(getLastUpdatedId(), cwsOffice.getLastUpdatedId())
        .append(getLastUpdatedTime(), cwsOffice.getLastUpdatedTime())
        .isEquals();
  }

  @Override
  public final int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(officeId)
        .append(governmentEntityType)
        .append(faxNumber)
        .append(geographicRegionTextCode)
        .append(headquarterIndicator)
        .append(inactiveIndicator)
        .append(mailStopDescription)
        .append(messagePhoneNumber)
        .append(messagePhoneExtensionNumber)
        .append(cwsOffNumber)
        .append(primaryPhoneNumber)
        .append(primaryPhoneExtensionNumber)
        .append(staffPersonId)
        .append(commentDescription)
        .append(agencyName)
        .append(departmentDivisionName)
        .append(cwsOfficeName)
        .append(countySpecificCode)
        .append(agencyCodeNumber)
        .append(locationCountyType)
        .append(directorsNameTitle)
        .append(getLastUpdatedId())
        .append(getLastUpdatedTime())
        .toHashCode();
  }
}
