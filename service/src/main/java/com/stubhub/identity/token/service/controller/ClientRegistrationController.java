package com.stubhub.identity.token.service.controller;

import com.stubhub.identity.token.service.client.OAuthClientDetails;
import com.stubhub.identity.token.service.client.OAuthClientDetailsDto;
import com.stubhub.identity.token.service.client.OAuthClientRegistrationService;
import com.stubhub.identity.token.service.client.OAuthClientSecretDto;
import com.stubhub.identity.token.service.exception.ExtendedErrorException;
import com.stubhub.identity.token.service.utils.ClientUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.Arrays;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Api
@RestController
@RequestMapping("/oauth/v1/clients")
@Slf4j
public class ClientRegistrationController {

  @Autowired private OAuthClientRegistrationService service;

  @Value("${stubhub.team.email}")
  private String DEFAULT_LAST_UPDATED_BY;

  private static final OAuthClientDetails.Status DEFAULT_CLIENT_STATUS =
      OAuthClientDetails.Status.ACTIVE;
  private static final Integer DEFAULT_ACCESS_TOKEN_VALIDITY_SECONDS = 60 * 60; // 1 hour
  private static final List<String> DEFAULT_GRANT_TYPES =
      Arrays.asList("authorization_code", "refresh_token", "client_credentials");
  private static final List<String> DEFAULT_SCOPES = Arrays.asList("default", "openid");
  private static final List<String> DEFAULT_AUTO_APPROVE = Arrays.asList("true");
  private static final String DEFAULT_CREATED_BY = "default_api:DL-SH-Identity@stubhub.com";

  /**
   * register client app
   *
   * @param
   * @return
   */
  @ApiOperation(
      value = "add client",
      notes = "register client which can issue token later",
      httpMethod = "POST")
  @RequestMapping(
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("#oauth2.hasScope('identity:tmgt:clients:write') || hasRole('ADMIN')")
  public ResponseEntity<OAuthClientDetailsDto> addClient(
      @RequestBody OAuthClientDetailsDto clientDetailsDto) {
    log.info("method=addClient, request={}", clientDetailsDto.toString());
    service.addClientDetails(clientDetailsDto);
    log.info("statusCode={}", HttpStatus.OK.value());
    return new ResponseEntity<>(clientDetailsDto, HttpStatus.OK);
  }

  /**
   * create default client app
   *
   * @param clientDetails
   * @return
   */
  @ApiOperation(
      value = "create default client information",
      notes =
          "create default client with 1 hour access token validity and 'default' scope only require clientName, clientOwner",
      httpMethod = "POST")
  @RequestMapping(
      value = "/default",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<OAuthClientDetailsDto> addDefaultClient(
      @RequestBody OAuthClientDetailsDto clientDetails) {
    log.info(
        "method=createDefaultClient,clientName={},clientOwner={}，redirectUris={}",
        clientDetails.getClientName(),
        clientDetails.getClientOwner(),
        clientDetails.getRegisteredRedirectUri());

    final boolean isNameOrOwnerEmpty =
        StringUtils.isEmpty(clientDetails.getClientName())
            || StringUtils.isEmpty(clientDetails.getClientOwner());
    if (isNameOrOwnerEmpty) {
      throw ExtendedErrorException.create(HttpStatus.BAD_REQUEST)
          .reason("Need clientName and clientOwner in request !")
          .build();
    }

    String clientId =
        StringUtils.isEmpty(clientDetails.getClientId())
            ? ClientUtils.getRandomNumber()
            : clientDetails.getClientId();
    String clientSecret =
        StringUtils.isEmpty(clientDetails.getClientSecret())
            ? ClientUtils.getRandomNumber()
            : clientDetails.getClientSecret();
    OAuthClientDetailsDto realUpdateClientDetails =
        OAuthClientDetailsDto.builder()
            .clientName(clientDetails.getClientName())
            .clientId(clientId)
            .clientSecret(clientSecret)
            .clientOwner(clientDetails.getClientOwner())
            .registeredRedirectUri(clientDetails.getRegisteredRedirectUri())
            .authorizedGrantTypes(DEFAULT_GRANT_TYPES)
            .scope(DEFAULT_SCOPES)
            .lastUpdateBy(DEFAULT_LAST_UPDATED_BY)
            .status(DEFAULT_CLIENT_STATUS)
            .accessTokenValiditySeconds(DEFAULT_ACCESS_TOKEN_VALIDITY_SECONDS)
            .autoApprove(DEFAULT_AUTO_APPROVE)
            .createdBy(
                StringUtils.isEmpty(clientDetails.getCreatedBy())
                    ? DEFAULT_CREATED_BY
                    : clientDetails.getCreatedBy())
            .build();
    this.addClient(realUpdateClientDetails);

    return new ResponseEntity<>(realUpdateClientDetails, HttpStatus.OK);
  }

  /**
   * update exist ACTIVE client app
   *
   * @param clientDetails
   * @return
   */
  @ApiOperation(
      value = "update client",
      notes = "update client detail information with exist client id",
      httpMethod = "PUT")
  @RequestMapping(
      method = RequestMethod.PUT,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("#oauth2.hasScope('identity:tmgt:clients:write') || hasRole('ADMIN')")
  public ResponseEntity<OAuthClientDetailsDto> updateClient(
      @RequestBody OAuthClientDetailsDto clientDetails) {
    log.info("method=updateClient, request={}", clientDetails.toString());
    OAuthClientDetailsDto oAuthClientDetailsDto = service.updateClientDetails(clientDetails);
    log.info("statusCode= {}", HttpStatus.OK.value());
    return new ResponseEntity<>(oAuthClientDetailsDto, HttpStatus.OK);
  }

  /**
   * update exist ACTIVE client app
   *
   * @param clientDetails
   * @return
   */
  @ApiOperation(
      value = "update self client information",
      notes = "update self client information by the client id in jwt authentication",
      httpMethod = "PUT")
  @RequestMapping(
      value = "/self",
      method = RequestMethod.PUT,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<OAuthClientDetailsDto> updateSelf(
      Authentication auth, @RequestBody OAuthClientDetailsDto clientDetails) {
    log.info("method=updateSelf, authentication={}", auth);
    if (!StringUtils.isEmpty(clientDetails.getClientId())
        && auth instanceof OAuth2Authentication
        && clientDetails
            .getClientId()
            .equals(((OAuth2Authentication) auth).getOAuth2Request().getClientId())) {
      OAuthClientDetailsDto realUpdateClientDetails =
          OAuthClientDetailsDto.builder()
              .clientId(clientDetails.getClientId())
              .clientName(clientDetails.getClientName())
              .lastUpdateBy(clientDetails.getLastUpdateBy())
              .registeredRedirectUri(clientDetails.getRegisteredRedirectUri())
              .status(clientDetails.getStatus())
              .resourceIds(clientDetails.getResourceIds())
              .accessTokenValiditySeconds(clientDetails.getAccessTokenValiditySeconds())
              .build();
      return updateClient(realUpdateClientDetails);
    } else {
      throw ExtendedErrorException.create(HttpStatus.FORBIDDEN)
          .reason(
              "Only clientName, lastUpdateBy, RegisteredRedirectUri, status, resourceIds can be updated for the authentication client itself!")
          .build();
    }
  }

  /**
   * Only Reset ACTIVE client secret
   *
   * @param clientId
   * @param clientSecret
   * @return
   */
  @ApiOperation(
      value = "reset client secret",
      notes = "reset client secret for specified client",
      httpMethod = "PUT")
  @RequestMapping(
      value = "/{clientId}/secret",
      method = RequestMethod.PUT,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("#oauth2.hasScope('identity:tmgt:clients:write') || hasRole('ADMIN')")
  public ResponseEntity<Object> resetClientSecret(
      @NonNull @PathVariable("clientId") String clientId,
      @RequestBody OAuthClientSecretDto clientSecret) {
    log.info("method=resetClientSecret, clientId={}", clientId);
    service.updateClientSecret(clientId, clientSecret);
    log.info("statusCode= {}", HttpStatus.OK.value());
    return new ResponseEntity<>(HttpStatus.OK);
  }

  /**
   * GET registered client information
   *
   * @param clientId
   * @return
   */
  @ApiOperation(
      value = "get client by id",
      notes = "get client detail information by client id",
      httpMethod = "GET")
  @RequestMapping(value = "/{clientId}", method = RequestMethod.GET)
  @PreAuthorize("#oauth2.hasScope('identity:tmgt:clients:read') || hasRole('ADMIN')")
  public ResponseEntity<OAuthClientDetailsDto> findClient(
      @PathVariable("clientId") String clientId) {
    log.info("method=findClient, clientId={}", clientId);
    return new ResponseEntity<>(service.getClientById(clientId), HttpStatus.OK);
  }

  /**
   * GET registered client information
   *
   * @return
   */
  @ApiOperation(
      value = "get self client information",
      notes = "get self client information by the client id in jwt authentication",
      httpMethod = "GET")
  @RequestMapping(value = "/self", method = RequestMethod.GET)
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<OAuthClientDetailsDto> getSelf(Authentication auth) {
    log.info("method=getSelf, authentication={}", auth);
    if (auth instanceof OAuth2Authentication) {
      return findClient(((OAuth2Authentication) auth).getOAuth2Request().getClientId());
    } else {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
    }
  }

  /**
   * GET all registered client information
   *
   * @return
   */
  @ApiOperation(value = "get clients", notes = "return all client list", httpMethod = "GET")
  @GetMapping
  @PreAuthorize("#oauth2.hasScope('identity:tmgt:clients:read') || hasRole('ADMIN')")
  public ResponseEntity<List<OAuthClientDetailsDto>> listAll() {
    return new ResponseEntity<>(service.getAllClients(), HttpStatus.OK);
  }

  /**
   * DELETE delete client
   *
   * @param clientId
   * @return
   */
  @ApiOperation(
      value = "delete client",
      notes = "delete client by client id",
      httpMethod = "DELETE")
  @RequestMapping(value = "/{clientId}", method = RequestMethod.DELETE)
  @PreAuthorize("#oauth2.hasScope('identity:tmgt:clients:write') || hasRole('ADMIN')")
  public ResponseEntity deleteById(@PathVariable("clientId") String clientId) {
    log.info("method=deleteClientById, clientId={}", clientId);
    service.delete(clientId);
    log.info("statusCode= {}", HttpStatus.OK.value());

    return ResponseEntity.ok().build();
  }
}
