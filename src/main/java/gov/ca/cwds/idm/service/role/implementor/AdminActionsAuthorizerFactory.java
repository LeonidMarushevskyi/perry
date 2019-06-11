package gov.ca.cwds.idm.service.role.implementor;

import static gov.ca.cwds.config.api.idm.Roles.COUNTY_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.OFFICE_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.STATE_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.SUPER_ADMIN;
import static gov.ca.cwds.idm.service.filter.MainRoleFilter.getMainRole;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCurrentUser;

import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserUpdate;
import gov.ca.cwds.idm.service.authorization.UserRolesService;
import gov.ca.cwds.idm.service.exception.ExceptionFactory;
import gov.ca.cwds.idm.service.rule.ErrorRulesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("idm")
public class AdminActionsAuthorizerFactory {

  private ExceptionFactory exceptionFactory;

  public AbstractAdminActionsAuthorizer getAdminActionsAuthorizer(User user, UserUpdate userUpdate) {

    AbstractAdminActionsAuthorizer authorizer = createAuthorizer(user, userUpdate);

    ErrorRulesFactory errorRulesFactory = new ErrorRulesFactory(user, userUpdate);
    errorRulesFactory.setExceptionFactory(exceptionFactory);
    authorizer.setRules(errorRulesFactory);

    return authorizer;
  }

  private AbstractAdminActionsAuthorizer createAuthorizer(User user, UserUpdate userUpdate){
    switch (getMainRole(getCurrentUser())) {
      case SUPER_ADMIN:
        return new SuperAdminAuthorizer(user, userUpdate);
      case STATE_ADMIN:
        return new StateAdminAuthorizer(user, userUpdate);
      case COUNTY_ADMIN:
        return new CountyAdminAuthorizer(user, userUpdate);
      case OFFICE_ADMIN:
        return new OfficeAdminAuthorizer(user, userUpdate);
      default:
        throw new IllegalStateException();
    }
  }

  @Autowired
  public void setExceptionFactory(ExceptionFactory exceptionFactory) {
    this.exceptionFactory = exceptionFactory;
  }
}
