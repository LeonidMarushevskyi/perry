package gov.ca.cwds.idm;

import gov.ca.cwds.idm.dto.UpdateUserDto;
import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.service.DictionaryProvider;
import gov.ca.cwds.idm.service.IdmService;
import gov.ca.cwds.rest.api.domain.UserExistsException;
import gov.ca.cwds.rest.api.domain.UserNotFoundPerryException;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
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

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
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
      responseContainer = "List")
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
      })
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
      consumes = "application/json")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ApiResponses(
      value = {
        @ApiResponse(code = 204, message = "No Content"),
        @ApiResponse(code = 401, message = "Not Authorized"),
        @ApiResponse(code = 404, message = "Not found")
      })
  @ApiOperation(value = "Update User")
  public ResponseEntity updateUser(
      @ApiParam(required = true, value = "The unique user ID", example = "userId1")
          @PathVariable
          @NotNull
          String id,
      @ApiParam(required = true, name = "userUpdateData", value = "The User update data")
          @NotNull
          @RequestBody
          UpdateUserDto updateUserDto) {
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
        @ApiResponse(code = 201, message = "Created"),
        @ApiResponse(code = 401, message = "Not Authorized"),
        @ApiResponse(code = 409, message = "Conflict")
      })
  @ApiOperation(value = "Create new Cognito User")
  public ResponseEntity createUser(
      @ApiParam(required = true, name = "createUserDto", value = "The User create data")
          @NotNull
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
    } catch (UserExistsException e) {
      return new ResponseEntity(HttpStatus.CONFLICT);
    }
  }

  @RequestMapping(method = RequestMethod.GET, value = "/permissions", produces = "application/json")
  @ApiResponses(
      value = {
        @ApiResponse(code = 401, message = "Not Authorized"),
        @ApiResponse(code = 404, message = "Not found")
      })
  @ApiOperation(
      value = "Get List of possible permissions",
      response = String.class,
      responseContainer = "List")
  public ResponseEntity<List<String>> getPermissions() {
    return Optional.ofNullable(dictionaryProvider.getPermissions())
        .map(permissions -> ResponseEntity.ok().body(permissions))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @RequestMapping(method = RequestMethod.PUT, value = "/permissions", consumes = "application/json")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ApiResponses(
      value = {
        @ApiResponse(code = 204, message = "No Content"),
        @ApiResponse(code = 401, message = "Not Authorized")
      })
  @ApiOperation(value = "Overwrite the List of possible permissions")
  @PreAuthorize("hasAuthority('CARES-admin')")
  public ResponseEntity overwritePermissions(
      @ApiParam(required = true, name = "List of Permissions", value = "List new Permissions here")
          @NotNull
          @RequestBody
          List<String> permissions) {
    dictionaryProvider.overwritePermissions(permissions);
    return ResponseEntity.noContent().build();
  }
}
