package com.stubhub.identity.token.service.token.shape;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stubhub.identity.token.service.exception.ExtendedErrorException;
import com.stubhub.identity.token.service.token.IdentityTokenService;
import com.stubhub.identity.token.service.token.InnerIssueTokenDto;
import com.stubhub.identity.token.service.utils.JwtGenerator;
import io.micrometer.core.instrument.util.StringUtils;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StopWatch;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class ShapeTokenService {

  @Qualifier("shapeTemplate")
  @Autowired
  private RestTemplate template;

  @Value("${remote.api.token.endpoint}")
  private String tokenEndpoint;

  @Value("${remote.api.token.validation.v2.endpoint}")
  private String tokenValidationEndpoint;

  @Value("${security.oauth2.client.clientId}")
  private String clientId;

  @Value("${security.oauth2.client.clientSecret}")
  private String clientSecret;

  @Value("${remote.api.iam.endpoint}")
  private String iamBaseUrl;

  @Autowired private JwtGenerator jwtGenerator;

  @Autowired private IdentityTokenService identityTokenService;

  private String logoutApi;

  @PostConstruct
  public void init() {
    logoutApi = iamBaseUrl + "/logout";
  }

  private ObjectMapper mapper = new ObjectMapper();

  private SHClientUserDto exchangeTokenFromShape(SHCookieDto shCookieDto)
      throws ExtendedErrorException {

    try {
      if (shCookieDto == null
          || (StringUtils.isEmpty(shCookieDto.getCookie())
              && StringUtils.isEmpty(shCookieDto.getToken()))) {
        throw ExtendedErrorException.create(HttpStatus.BAD_REQUEST)
            .reason("request body is empty!")
            .cause(new Throwable("BAD REQUEST"))
            .build();
      }

      StopWatch sw = new StopWatch();
      sw.start();

      log.info(
          "method=exchangeTokenFromShape, uri={} , httpMethod={} , reqBody={} ",
          tokenValidationEndpoint,
          HttpMethod.POST,
          shCookieDto);

      HttpEntity req = new HttpEntity(shCookieDto);
      ResponseEntity<SHClientUserDto> res =
          template.postForEntity(tokenValidationEndpoint, req, SHClientUserDto.class);
      sw.stop();
      log.info(
          "api=tms_remoteapi_extoken, method=exchangeTokenFromShape, statusCode={}, duration={} ms",
          res.getStatusCode(),
          sw.getTotalTimeMillis());
      return res.getBody();
    } catch (RestClientResponseException e) {
      log.error(
          "method=exchangeTokenFromShape, errMsg=\"remote call exchange token {} fail with {}, {}\"",
          tokenValidationEndpoint,
          e.getStatusText(),
          e.getResponseBodyAsString());
      throw ExtendedErrorException.create(((HttpStatusCodeException) e).getStatusCode())
          .putData("path", tokenValidationEndpoint)
          .reason(e.getResponseBodyAsString())
          .cause(new Throwable("Remote Server Error"))
          .build();
    }
  }

  @SneakyThrows(Exception.class)
  public Map exchangeToken(SHCookieDto shCookieDto) {
    log.debug(
        "method=exchangeToken client information clientId={}, clientSecret={}",
        clientId,
        clientSecret);
    String content = mapper.writeValueAsString(exchangeTokenFromShape(shCookieDto));
    Jwt jwt = jwtGenerator.generateJWT(content);
    Map<String, String> jwtMap = new HashMap<>();
    jwtMap.put("jwt", jwt.getEncoded());
    return jwtMap;
  }

  public OAuth2AccessToken refreshToken(String clientId, String clientSecret, String token)
      throws ExtendedErrorException {
    try {
      StopWatch sw = new StopWatch();
      sw.start();

      log.debug(
          "method=refreshToken, client information clientId={}, clientSecret={}, token={}",
          clientId,
          clientSecret,
          token);

      HttpHeaders headers = new HttpHeaders();
      headers.add("Content-Type", "application/x-www-form-urlencoded");

      MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
      map.add("grant_type", "refresh_token");
      map.add("refresh_token", token);
      map.add("client_id", clientId);
      map.add("client_secret", clientSecret);
      map.add("from_cloud", "true");
      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
      log.info("method=refreshToken with shape token api :{}", tokenEndpoint);
      ResponseEntity<OAuth2AccessToken> responseToken =
          template.postForEntity(tokenEndpoint, request, OAuth2AccessToken.class);
      sw.stop();
      log.info(
          "api=tms_remoteapi_refreshtoken, method=refreshToken, statusCode={}, duration={} ms, message= \"refresh token from shape \"",
          responseToken.getStatusCode(),
          sw.getTotalTimeMillis());
      return responseToken.getBody();
    } catch (RestClientResponseException e) {
      log.error(
          "method=refreshToken refresh token api from shape fail with {} {}",
          e.getStatusText(),
          e.getResponseBodyAsString());
      throw ExtendedErrorException.create(((HttpStatusCodeException) e).getStatusCode())
          .putData("path", tokenEndpoint)
          .reason(e.getResponseBodyAsString())
          .cause(new Throwable("Remote Server Error"))
          .build();
    }
  }

  public OAuth2AccessToken refreshJwtToken(
      String clientId, String clientSecret, String refreshToken) throws ExtendedErrorException {

    log.debug("method=refreshJwtToken try to refresh token from shape service");
    // refresh token from shape with client id and secret pass from api
    OAuth2AccessToken token = refreshToken(clientId, clientSecret, refreshToken);
    log.debug("method=refreshJwtToken refresh token from shape service successfully");

    // exchange opaque token to jwt
    SHClientUserDto shapeToken =
        exchangeTokenFromShape(SHCookieDto.builder().token(token.getValue()).build());

    // get user information from jwt
    InnerIssueTokenDto innerIssueTokenDto = new InnerIssueTokenDto();
    innerIssueTokenDto.setClientId(shapeToken.getClientId());
    innerIssueTokenDto.setGuid(shapeToken.getSub());
    innerIssueTokenDto.setEmail(shapeToken.getUserName());

    // issue user token from cloud service
    return identityTokenService.generateUserTokenByUserId(innerIssueTokenDto);
  }

  public void logout(String sessionCookie) {
    try {
      StopWatch sw = new StopWatch();
      sw.start();

      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
      headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
      headers.add("Cookie", sessionCookie);
      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(null, headers);
      log.info("method=logout with shape api {}", logoutApi);

      ResponseEntity<String> res = template.postForEntity(logoutApi, request, String.class);
      sw.stop();
      log.info(
          "api=tms_remoteapi_logout, method=logout, statusCode={}, duration={} ms",
          res.getStatusCode(),
          sw.getTotalTimeMillis());
    } catch (RestClientResponseException e) {
      log.error(
          "method=logout, errMsg=\"remote call logout {} fail with {}, {}\"",
          logoutApi,
          e.getStatusText(),
          e.getResponseBodyAsString());
      throw ExtendedErrorException.create(((HttpStatusCodeException) e).getStatusCode())
          .putData("path", logoutApi)
          .reason(e.getResponseBodyAsString())
          .cause(new Throwable("Remote Server Error"))
          .build();
    }
  }
}
