package gov.ca.cwds.idm;

import static gov.ca.cwds.service.messages.MessageCode.IDM_USER_VALIDATION_FAILED;
import static gov.ca.cwds.service.messages.MessageCode.USER_WITH_EMAIL_EXISTS_IN_IDM;

import gov.ca.cwds.idm.dto.IdmApiCustomError;
import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserUpdate;
import gov.ca.cwds.idm.dto.UserVerificationResult;
import gov.ca.cwds.idm.persistence.model.Permission;
import gov.ca.cwds.idm.service.DictionaryProvider;
import gov.ca.cwds.idm.service.IdmService;
import gov.ca.cwds.rest.api.domain.UserAlreadyExistsException;
import gov.ca.cwds.rest.api.domain.UserIdmValidationException;
import gov.ca.cwds.rest.api.domain.UserNotFoundPerryException;
import gov.ca.cwds.service.messages.MessageCode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Profile("idm")
@RequestMapping(value = "/idm")
public class IdmResource {

  @Autowired private IdmService idmService;

  @Autowired private DictionaryProvider dictionaryProvider;

  @RequestMapping(method = RequestMethod.GET, value = "/users", produces = "application/json")
  @ApiOperation(
    value = "Users to manage by current logged-in admin",
    response = User.class,
    responseContainer = "List"
  )
  @ApiResponses(value = {@ApiResponse(code = 401, message = "Not Authorized")})
  public List<User> getUsers(
      @ApiParam(name = "lastName", value = "lastName to search for")
          @RequestParam(name = "lastName", required = false)
          String lastName) {
    return idmService.getUsers(lastName);
  }

  @RequestMapping(method = RequestMethod.GET, value = "/users/{id}", produces = "application/json")
  @ApiOperation(value = "Find User by ID", response = User.class)
  @ApiResponses(
    value = {
      @ApiResponse(code = 401, message = "Not Authorized"),
      @ApiResponse(code = 404, message = "Not found")
    }
  )
  public ResponseEntity<User> getUser(
      @ApiParam(required = true, value = "The unique user ID", example = "userId1")
          @PathVariable
          @NotNull
          String id) {

    try {
      User user = idmService.findUser(id);
      return ResponseEntity.ok().body(user);
    } catch (UserNotFoundPerryException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @RequestMapping(
    method = RequestMethod.PATCH,
    value = "/users/{id}",
    consumes = "application/json"
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ApiResponses(
    value = {
      @ApiResponse(code = 204, message = "No Content"),
      @ApiResponse(code = 401, message = "Not Authorized"),
      @ApiResponse(code = 404, message = "Not found")
    }
  )
  @ApiOperation(value = "Update User")
  public ResponseEntity updateUser(
      @ApiParam(required = true, value = "The unique user ID", example = "userId1")
          @PathVariable
          @NotNull
          String id,
      @ApiParam(required = true, name = "userUpdateData", value = "The User update data")
          @NotNull
          @RequestBody
          UserUpdate updateUserDto) {
    try {
      idmService.updateUser(id, updateUserDto);
      return ResponseEntity.noContent().build();
    } catch (UserNotFoundPerryException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @RequestMapping(method = RequestMethod.POST, value = "/users", consumes = "application/json")
  @ResponseStatus(HttpStatus.CREATED)
  @ApiResponses(
      value = {
        @ApiResponse(code = 201, message = "New User is created successfully"),
        @ApiResponse(code = 400, message = "Bad Request. Provided JSON has invalid data"),
        @ApiResponse(
            code = 401,
            message =
                "Not Authorized. For example county_name differs from the county admin belongs to."),
        @ApiResponse(code = 409, message = "Conflict. User with the same email already exists")
      })
  @ApiOperation(
      value = "Create new User",
      notes =
          "Only the following properties of the input User JSON will be used at new User creation:\n "
              + "email, first_name, last_name, county_name, RACFID, permissions, office, phone_number.\n "
              + "Other properties values will be set by the system automatically.\n"
              + "Required properties are: email, first_name, last_name, county_name.")
  public ResponseEntity createUser(
      @ApiParam(required = true, name = "User", value = "The User create data")
          @NotNull
          @Valid
          @RequestBody
          User user) {
    try {
      String newUserId = idmService.createUser(user);
      URI uri =
          ServletUriComponentsBuilder.fromCurrentRequest()
              .path("/{id}")
              .buildAndExpand(newUserId)
              .toUri();
      return ResponseEntity.created(uri).build();

    } catch (UserAlreadyExistsException e) {
      return createCustomResponseEntity(
          HttpStatus.CONFLICT, USER_WITH_EMAIL_EXISTS_IN_IDM, e.getMessage());
    } catch (UserIdmValidationException e) {
      return createCustomResponseEntity(
          HttpStatus.BAD_REQUEST, IDM_USER_VALIDATION_FAILED, e.getMessage(), e.getCause().getMessage());
    }
  }

  @RequestMapping(method = RequestMethod.GET, value = "/permissions", produces = "application/json")
  @ApiResponses(
    value = {
      @ApiResponse(code = 401, message = "Not Authorized"),
      @ApiResponse(code = 404, message = "Not found")
    }
  )
  @ApiOperation(
    value = "Get List of possible permissions",
    response = Permission.class,
    responseContainer = "List"
  )
  public List<Permission> getPermissions() {
    return  dictionaryProvider.getPermissions();
  }

  @RequestMapping(method = RequestMethod.PUT, value = "/permissions", consumes = "application/json")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ApiResponses(
    value = {
      @ApiResponse(code = 204, message = "No Content"),
      @ApiResponse(code = 401, message = "Not Authorized")
    }
  )
  @ApiOperation(value = "Overwrite the List of possible permissions")
  @PreAuthorize("hasAuthority('CARES-admin')")
  public ResponseEntity overwritePermissions(
      @ApiParam(required = true, name = "List of Permissions", value = "List new Permissions here")
          @NotNull
          @RequestBody
          List<Permission> permissions) {
    dictionaryProvider.overwritePermissions(permissions);
    return ResponseEntity.noContent().build();
  }

  @RequestMapping(method = RequestMethod.GET, value = "users/verify", produces = "application/json")
  @ApiOperation(value = "Check if user can be created by racfId and email", response = UserVerificationResult.class)
  @ApiResponses(value = {@ApiResponse(code = 401, message = "Not Authorized")})
  public ResponseEntity<UserVerificationResult> verifyUser(
      @ApiParam(required = true, name = "racfid", value = "The RACFID to verify user by in CWS/CMS")
          @NotNull
          @RequestParam("racfid")
          String racfId,
      @ApiParam(required = true, name = "email", value = "The email to verify user by in Cognito")
          @NotNull
          @RequestParam("email")
          String email) {
    return ResponseEntity.ok().body(idmService.verifyUser(racfId, email));
  }

  private static ResponseEntity<IdmApiCustomError> createCustomResponseEntity(
      HttpStatus httpStatus, MessageCode errorCode, String msg, String cause) {
    return new ResponseEntity<>(
        new IdmApiCustomError(httpStatus, errorCode, msg, cause), httpStatus);
  }

  private static ResponseEntity<IdmApiCustomError> createCustomResponseEntity(
      HttpStatus httpStatus, MessageCode errorCode, String msg) {
    return new ResponseEntity<>(
        new IdmApiCustomError(httpStatus, errorCode, msg), httpStatus);
  }
}