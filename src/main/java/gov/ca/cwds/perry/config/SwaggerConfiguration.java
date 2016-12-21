package gov.ca.cwds.perry.config;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SwaggerConfiguration {

  @JsonProperty
  @NotEmpty
  private String templateName;

  @JsonProperty
  @NotEmpty
  private String assetsPath;

  @JsonProperty
  @NotEmpty
  private String resourcePackage;

  @JsonProperty
  @NotEmpty
  private String title;

  @JsonProperty
  @NotEmpty
  private String description;

  @JsonProperty
  @NotEmpty
  private String jsonUrl;

  @JsonProperty
  @NotEmpty
  private String logo;

  /**
   * @return the templateName
   */
  public String getTemplateName() {
    return templateName;
  }

  /**
   * @return the assetsPath
   */
  public String getAssetsPath() {
    return assetsPath;
  }

  /**
   * @return the resourcePackage
   */
  public String getResourcePackage() {
    return resourcePackage;
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * @return the jsonUrl
   */
  public String getJsonUrl() {
    return jsonUrl;
  }

  /**
   * @return the logo
   */
  public String getLogo() {
    return logo;
  }
}
