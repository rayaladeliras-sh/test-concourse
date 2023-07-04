package com.stubhub.identity.token.service.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stubhub.identity.token.service.auditlog.Audit;
import com.stubhub.identity.token.service.auditlog.enums.AuditedMethodEnum;
import com.stubhub.identity.token.service.client.ClientBasicAuthCheckService;
import com.stubhub.identity.token.service.client.IdentityClientDetailsService;
import com.stubhub.identity.token.service.client.OAuthClientDetails;
import com.stubhub.identity.token.service.token.IdentityTokenService;
import com.stubhub.identity.token.service.token.InnerIssueTokenDto;
import com.stubhub.identity.token.service.token.act.ActUserTokenDto;
import com.stubhub.identity.token.service.token.refresh.OAuthRefreshTokenService;
import com.stubhub.identity.token.service.token.shape.SHCookieDto;
import com.stubhub.identity.token.service.token.shape.ShapeTokenService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.view.RedirectView;

@Api
@Lazy
@RestController
@RequestMapping(path = {"/oauth/v1/inner", "/oauth/v1/ext"})
@Slf4j
public class IdentityTokenController {

  @Autowired private IdentityTokenService identityTokenService;

  @Autowired private ShapeTokenService shapeTokenService;

  @Autowired private OAuthRefreshTokenService oAuthRefreshTokenService;

  @Autowired private ClientBasicAuthCheckService clientBasicAuthCheckService;

  @Autowired private IdentityClientDetailsService clientDetailsService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @ApiOperation(
      value = "generateUserTokenByUserId",
      notes = "issue user token by guid and client id",
      httpMethod = "POST")
  @RequestMapping(
      value = "/token",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("#oauth2.hasScope('identity:tmgt:ext:token')")
  public ResponseEntity generateUserTokenByUserId(
      Authentication authentication, @RequestBody InnerIssueTokenDto innerIssueTokenDto) {
    try {
      StopWatch sw = new StopWatch();
      sw.start();
      log.info(
          "method=generateUserTokenByUserId, message=\"generate user token with {} by client {}\"",
          innerIssueTokenDto,
          authentication.getPrincipal());
      OAuth2AccessToken oAuth2AccessToken =
          identityTokenService.generateUserTokenByUserId(innerIssueTokenDto);
      sw.stop();
      log.debug(
          "--PERF--generate user token successfully with duration: {} ms.",
          sw.getTotalTimeMillis());
      return ResponseEntity.ok(oAuth2AccessToken);
    } catch (Exception e) {
      log.error(
          "method=generateUserTokenByUserId, errMsg=\"issue user token fail {}\"",
          e.getLocalizedMessage());
      if (e instanceof RestClientResponseException) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(((RestClientResponseException) e).getResponseBodyAsString());
      }
      throw e;
    }
  }

  @ApiOperation(
      value = "actUserToken",
      notes = "issue user token with act information",
      httpMethod = "POST")
  @RequestMapping(
      value = "/act",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("#oauth2.hasScope('identity:tmgt:ext:act')")
  @Audit(method = AuditedMethodEnum.ACT_USER_TOKEN)
  public ResponseEntity actUserToken(
      Authentication authentication, @RequestBody ActUserTokenDto actUserTokenDto)
      throws JsonProcessingException {
    try {
      StopWatch sw = new StopWatch();
      sw.start();
      log.info(
          "method=actUserToken, message=\"act user token with {} by client {}\"",
          actUserTokenDto,
          authentication.getPrincipal());
      HashMap params = new HashMap<String, String>();
      if (null != actUserTokenDto.getAct()) {
        if (StringUtils.isEmpty(actUserTokenDto.getAct().getSub())) {
          throw new IllegalArgumentException(
              "The value of Sub value for act claim cant not be null or empty.");
        }
        if (StringUtils.isEmpty(actUserTokenDto.getAct().getClientId())) {
          throw new IllegalArgumentException(
              "The value of client id value for act claim cant not be null or empty.");
        }
        actUserTokenDto
            .getAct()
            .setClientName(
                clientDetailsService
                    .loadClientByClientId(actUserTokenDto.getAct().getClientId())
                    .getClientName());
        params.put("act", objectMapper.writeValueAsString(actUserTokenDto.getAct()));
      }
      OAuth2AccessToken oAuth2AccessToken =
          identityTokenService.generateUserTokenByUserId(actUserTokenDto, params);
      sw.stop();
      log.debug(
          "--PERF--act user token successfully with duration: {} ms.", sw.getTotalTimeMillis());
      return ResponseEntity.ok(oAuth2AccessToken);
    } catch (Exception e) {
      log.error("method=actUserToken, errMsg=\"act user token fail {}\"", e.getLocalizedMessage());
      if (e instanceof RestClientResponseException) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(((RestClientResponseException) e).getResponseBodyAsString());
      }
      throw e;
    }
  }

  @ApiOperation(
      value = "exchangeToken",
      notes = "exchange token by legacy opaque token",
      httpMethod = "POST")
  @RequestMapping(
      value = "/extoken",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity exchangeToken(@RequestBody(required = false) SHCookieDto shCookieDto) {
    try {
      log.info(
          "method=exchangeToken, message=\"Exchange token by token or cookie {}\"", shCookieDto);
      StopWatch sw = new StopWatch();
      sw.start();
      Map jwt = shapeTokenService.exchangeToken(shCookieDto);
      sw.stop();
      log.debug(
          "--PERF-- exchange token successfully with duration: {} ms.", sw.getTotalTimeMillis());
      return new ResponseEntity<>(jwt, HttpStatus.OK);
    } catch (RestClientResponseException e) {
      log.error(
          "method=exchangeToken, errMsg=\"exchange token fail {}, {}\"",
          e.getStatusText(),
          e.getResponseBodyAsString());
      return ResponseEntity.status(e.getRawStatusCode()).body(e.getResponseBodyAsString());
    }
  }

  @Deprecated
  @ApiOperation(
      value = "deprecatedKeys",
      notes =
          "this api will be deprecated soon, please use \"/oauth/v1/.well-known/jwks.json\" instead",
      httpMethod = "GET")
  @GetMapping("/jwks.json")
  public RedirectView deprecatedKeys() {
    return new RedirectView("/identity/oauth/v1/.well-known/jwks.json");
  }

  @ApiOperation(
      value = "revokeRefreshToken",
      notes = "revoke refresh token of this client with valid client credentials",
      httpMethod = "POST")
  @PostMapping(value = "/token/revoke", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public void revokeRefreshToken(
      HttpServletRequest request,
      @RequestParam String token,
      @RequestParam(required = false) String token_type_hint) {

    if (!StringUtils.isEmpty(token_type_hint)
        && !token_type_hint.equalsIgnoreCase("refresh_token")) {
      throw new InvalidRequestException("Token type " + token_type_hint + " is not support");
    }

    OAuthClientDetails clientDetails = clientBasicAuthCheckService.checkClientCredentials(request);

    if (!StringUtils.isEmpty(token)) {
      OAuth2Authentication authentication =
          oAuthRefreshTokenService.readAuthenticationForRefreshToken(token);
      // authentication is null when no refresh token was found
      // in this case return 200 avoid detecting valid refresh token by malicious client
      if (null == authentication) {
        log.info("method=revokeRefreshToken {} is invalid", token);
      } else if (clientDetails
          .getClientId()
          .equalsIgnoreCase(authentication.getOAuth2Request().getClientId())) {
        log.info("method=revokeRefreshToken {} was revoked", token);
        oAuthRefreshTokenService.deleteById(token);
      } else {
        throw new InsufficientAuthenticationException("The client or token is invalid.");
      }
    }
  }
}
