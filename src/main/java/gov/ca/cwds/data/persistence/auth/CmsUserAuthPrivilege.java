package gov.ca.cwds.data.persistence.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enumerated type container for CMS authorization privileges.
 *
 * @author CWDS API Team
 */
public final class CmsUserAuthPrivilege {

  private static final CmsUserAuthPrivilege singleton = new CmsUserAuthPrivilege();

  private static final Logger LOGGER = LoggerFactory.getLogger(CmsUserAuthPrivilege.class);

  private final Map<String, UnitAuthority> unitAuthorityByCode = buildUnitAuthorityRegistry();
  private final Map<Long, UserAuthPriv> privBySysId = buildUserPrivRegistry();
  private final Map<Long, AuthorizationLevel> authLvlBySysId = buildAuthLevelRegistry();

  private static final Map<String, UnitAuthority> buildUnitAuthorityRegistry() {
    Map<String, UnitAuthority> ret = new ConcurrentHashMap<>();
    for (UnitAuthority unitAuth : UnitAuthority.values()) {
      ret.put(unitAuth.getUnitAuthCode(), unitAuth);
    }

    return ret;
  }

  private static final Map<Long, UserAuthPriv> buildUserPrivRegistry() {
    Map<Long, UserAuthPriv> ret = new ConcurrentHashMap<>();
    for (UserAuthPriv priv : UserAuthPriv.values()) {
      ret.put(priv.getSysId(), priv);
    }

    return ret;
  }

  private static final Map<Long, AuthorizationLevel> buildAuthLevelRegistry() {
    Map<Long, AuthorizationLevel> ret = new ConcurrentHashMap<>();
    for (AuthorizationLevel authLvl : AuthorizationLevel.values()) {
      ret.put(authLvl.getSysId(), authLvl);
    }

    return ret;
  }

  /**
   * An evil singleton. <p> <p> Immutable enum service. Probably fine. </p>
   *
   * @return singleton instance
   */
  public static final CmsUserAuthPrivilege getInstance() {
    return singleton;
  }

  /**
   * Look up unit authority code.
   *
   * @param code single character unit authority code
   * @return matching unit authority
   */
  public final UnitAuthority findUnitAuthorityByCode(final String code) {
    return unitAuthorityByCode.get(code);
  }

  /**
   * Look up user level privileges by SYS_ID.
   *
   * @param sysId system code id, per table SYS_CD_C
   * @return matching user privilege
   */
  public final UserAuthPriv findUserPrivBySysId(long sysId) {
    return privBySysId.get(sysId);
  }

  /**
   * Look up authorization level by SYS_ID.
   *
   * @param sysId system code id, per table SYS_CD_C
   * @return matching user privilege
   */
  public final AuthorizationLevel findAuthLvlBySysId(long sysId) {
    return authLvlBySysId.get(sysId);
  }

  /**
   * Look up description of authorization privilege type
   *
   * @param type the sysId String value
   * @return the description
   */
  public String getUserAuthPrivDescription(String type) {
    String description = "";
    try {
      Long sysId = Long.valueOf(type);
      description = findSysIdDescription(sysId);
    } catch (Exception e) {
      LOGGER.error("ERROR - Description couldn't parse: {}", e.getMessage());
    }
    return description;
  }

  /**
   * Find a system code description by SYS_ID either user authorization privilege, {@link
   * UserAuthPriv}, or authorization level (state, county, global, etc.), {@link
   * AuthorizationLevel}.
   *
   * @param sysId code to look up
   * @return the code's description
   */
  public final String findSysIdDescription(long sysId) {
    String ret = null;
    final UserAuthPriv priv = findUserPrivBySysId(sysId);
    if (priv != null) {
      ret = priv.getDescription();
    } else {
      final AuthorizationLevel authLvl = findAuthLvlBySysId(sysId);
      if (authLvl != null) {
        ret = authLvl.getDescription();
      }
    }

    return ret;
  }

  /**
   * AUTHORITY_CODE - This represents the type of authority a designated STAFF PERSON has for a
   * particular ASSIGNMENT UNIT. The valid values are (S = Supervisor, A = Approval Authority, T =
   * Assignment Transfer Authority, B = Both the Approval and Assignment <p> <p> Taken from table
   * STAFF_PERSON_UNIT_AUTHORITY. </p>
   *
   * @author CWDS API Team
   */
  public enum UnitAuthority {

    /**
     * Unit supervisor.
     */
    UNIT_AUTH_SUPERVISOR("S", "Supervisor"),

    /**
     * Approve case changes within unit.
     */
    UNIT_AUTH_APPROVAL("A", "Approval Authority"),

    /**
     * Transfer assignments within unit.
     */
    UNIT_AUTH_ASSIGNMENT_TRANSFER("T", "Assignment Transfer Authority"),

    /**
     * Both approve and assign.
     */
    UNIT_AUTH_BOTH_APPROVE_AND_ASSIGN("B", "Both the Approval and Assignment");

    private final String unitAuthCode;
    private final String description;

    private UnitAuthority(String unitAuthCode, String description) {
      this.unitAuthCode = unitAuthCode;
      this.description = description;
    }

    /**
     * Get the single char unit authority code.
     *
     * @return unit authority code
     */
    public String getUnitAuthCode() {
      return unitAuthCode;
    }

    /**
     * Get description of the unit authority code.
     *
     * @return the unit auth code description.
     */
    public String getDescription() {
      return description;
    }
  }

  /**
   * Enumerated types for user authorization privileges, for FKS_META_T = LVL_ATHC.
   *
   * @author CWDS API Team
   */
  public enum AuthorizationLevel {

    /**
     * County Administration
     */
    AUTH_LVL_COUNTY(1298, "County Administration", "0001"),

    /**
     * Office Administration
     */
    AUTH_LVL_OFFICE(1299, "Office Administration", "0002"),

    /**
     * State Administration
     */
    AUTH_LVL_STATE(1300, "State Administration", "0003"),

    /**
     * User
     */
    AUTH_LVL_USER(1301, "User", "0004"),

    /**
     * Global Administration
     */
    AUTH_LVL_GLOBAL(2201, "Global Administration", "0005");

    private final long sysId;
    private final String description;
    private final String orderInCategory;

    private AuthorizationLevel(long sysId, String shortDsc, String lgcId) {
      this.sysId = sysId;
      this.description = shortDsc;
      this.orderInCategory = lgcId;
    }

    /**
     * Primary key of CMS system code table.
     *
     * @return system code PK
     */
    public long getSysId() {
      return this.sysId;
    }

    /**
     * Description of this system code.
     *
     * @return system code description
     */
    public String getDescription() {
      return description;
    }

    /**
     * Relative order of system code in category. Authorization level only has one category.
     *
     * @return system code order
     */
    public String getOrderInCategory() {
      return orderInCategory;
    }

  }

  /**
   * Enumerated types for user authorization privileges, for FKS_META_T = PRIVLG_C.
   *
   * @author CWDS API Team
   */
  public enum UserAuthPriv {

    /**
     * Access Authority
     */
    USR_PRV_ACCESS_AUTHORITY(1460, "Access Authority", 0, "0001"),

    /**
     * Interface Authority
     */
    USR_PRV_INTERFACE_AUTHORITY(1461, "Interface Authority", 0, "0002"),

    /**
     * Limited Access Authority
     */
    USR_PRV_LIMITED_ACCESS_AUTHORITY(1462, "Limited Access Authority", 0, "0003"),

    /**
     * Override Authority
     */
    USR_PRV_OVERRIDE_AUTHORITY(1463, "Override Authority", 0, "0004"),

    /**
     * Assignment Match
     */
    USR_PRV_ASSIGNMENT_MATCH(1464, "Assignment Match", 1460, "0005", false),

    /**
     * Bulletin Administrator
     */
    USR_PRV_BULLETIN_ADMINISTRATOR(1465, "Bulletin Administrator", 1460, "0006"),

    /**
     * Closed Case/Referral Update
     */
    USR_PRV_CLOSED_CASE_REFERRAL_UPDATE(1466, "Closed Case/Referral Update", 1460, "0007"),

    /**
     * Code Table Maintenance
     */
    USR_PRV_CODE_TABLE_MAINTENANCE(1467, "Code Table Maintenance", 1460, "0008", false),

    /**
     * County License Case Management
     */
    USR_PRV_COUNTY_LICENSE_CASE_MANAGEMENT(3295, "County License Case Management", 1460, "0009"),

    /**
     * CWS Case Management System
     */
    USR_PRV_CWS_CASE_MANAGEMENT_SYSTEM(1468, "CWS Case Management System", 1460, "0010"),

    /**
     * Fingerprint Management Reports
     */
    USR_PRV_FINGERPRINT_MANAGEMENT_REPORTS(1469, "Fingerprint Management Reports", 1460, "0011"),

    /**
     * Fingerprint Services
     */
    USR_PRV_FINGERPRINT_SERVICES(1470, "Fingerprint Services", 1460, "0012"),

    /**
     * Program Management Reports
     */
    USR_PRV_PROGRAM_MANAGEMENT_REPORTS(1471, "Program Management Reports", 1460, "0013"),

    /**
     * Resource Management
     */
    USR_PRV_RESOURCE_MANAGEMENT(1472, "Resource Management", 1460, "0014"),

    /**
     * Resource Management Placement Facility Maintenance
     */
    USR_PRV_RESOURCE_MGMT_PLACEMENT_FACILITY_MAINT(1473, "Resource Mgmt Placement Facility Maint",
        1460, "0015"),

    /**
     * System Administration
     */
    USR_PRV_SYSTEM_ADMINISTRATION(1474, "System Administration", 1460, "0016"),

    /**
     * CDS Client Index
     */
    USR_PRV_CDS_CLIENT_INDEX(1475, "CDS Client Index", 1461, "0017"),

    /**
     * MEDS
     */
    USR_PRV_MEDS(1476, "MEDS", 1461, "0018"),

    /**
     * LIS
     */
    USR_PRV_LIS(2182, "LIS", 1461, "0020"),

    /**
     * Adoptions
     */
    USR_PRV_ADOPTIONS(1479, "Adoptions", 1460, "0021"),

    /**
     * Authority
     */
    USR_PRV_AUTHORITY(2672, "Authority", 1462, "0022", false),

    /**
     * Probation
     */
    USR_PRV_PROBATION(1480, "Probation", 1462, "0023", false),

    /**
     * Sealed
     */
    USR_PRV_SEALED(1481, "Sealed", 1462, "0024"),

    /**
     * Sensitive Persons
     */
    USR_PRV_SENSITIVE_PERSONS(1482, "Sensitive Persons", 1462, "0025"),

    /**
     * Countywide Read
     */
    USR_PRV_COUNTYWIDE_READ(1483, "Countywide Read", 1463, "0026"),

    /**
     * Countywide Read/Write
     */
    USR_PRV_COUNTYWIDE_READ_WRITE(1484, "Countywide Read/Write", 1463, "0027"),

    /**
     * State Read Assignment
     */
    USR_PRV_STATE_READ_ASSIGNMENT(1485, "State Read Assignment", 1463, "0028", false),

    /**
     * Statewide Read
     */
    USR_PRV_STATEWIDE_READ(1486, "Statewide Read", 1463, "0029"),

    /**
     * Office-wide Read
     */
    USR_PRV_OFFICEWIDE_READ(5390, "Officewide Read", 1463, "0030"),

    /**
     * Office-wide Read/Write
     */
    USR_PRV_OFFICEWIDE_READ_WRITE(5391, "Officewide Read/Write", 1463, "0031"),

    /**
     * SOC158 Application
     */
    USR_PRV_SOC158_APPLICATION(5589, "SOC158 Application", 1460, "0031"),

    /**
     * Non-CWD
     */
    USR_PRV_NON_CWD(5609, "Non-CWD", 1460, "0032"),

    /**
     * Cell Phone
     */
    USR_PRV_CELL_PHONE(6072, "Cell Phone", 1460, "0033"),

    /**
     * Non-CWD Kin-GAP
     */
    USR_PRV_NON_CWD_KIN_GAP(6131, "Non-CWD Kin-GAP", 1460, "0034"),

    /**
     * Non-CWD Mental Health
     */
    USR_PRV_NON_CWD_MENTAL_HEALTH(6132, "Non-CWD Mental Health", 1460, "0035"),

    /**
     * Merge Client
     */
    USR_PRV_MERGE_CLIENT(6795, "Merge Client", 1460, "0036"),

    /**
     * Create Service Provider
     */
    USR_PRV_CREATE_SERVICE_PROVIDER(6904, "Create Service Provider", 1460, "0037");

    private final long sysId;
    private final long categoryId;
    private final String description;
    private final String orderInCategory;
    private final boolean active;

    private UserAuthPriv(long sysId, String shortDsc, long categoryId, String orderInCategory) {
      this.sysId = sysId;
      this.description = shortDsc;
      this.categoryId = categoryId;
      this.orderInCategory = orderInCategory;
      this.active = true;
    }

    private UserAuthPriv(long sysId, String shortDsc, long categoryId, String orderInCategory,
        boolean active) {
      this.sysId = sysId;
      this.description = shortDsc;
      this.categoryId = categoryId;
      this.orderInCategory = orderInCategory;
      this.active = active;
    }

    /**
     * Primary key of CMS system code table.
     *
     * @return system code PK
     */
    public long getSysId() {
      return this.sysId;
    }

    /**
     * Get category of this system code or zero if none.
     *
     * @return system code category
     */
    public long getCategoryId() {
      return this.categoryId;
    }

    /**
     * Description of this system code.
     *
     * @return system code description
     */
    public String getDescription() {
      return description;
    }

    /**
     * Relative order of system code in category
     *
     * @return system code order
     */
    public String getOrderInCategory() {
      return orderInCategory;
    }

    /**
     * A handful of privileges are marked as inactive.
     *
     * @return whether the privilege is active
     */
    public boolean isActive() {
      return active;
    }

  }

}
