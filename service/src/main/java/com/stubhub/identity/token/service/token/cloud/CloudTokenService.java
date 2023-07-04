package com.stubhub.identity.token.service.token.cloud;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import io.micrometer.core.instrument.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
public class CloudTokenService {

    @Autowired
    private OAuth2RestTemplate oAuth2RestTemplate;

    @Value("${remote.api.session.mgt.endpoint}")
    private String sessionMgtBaseUrl;

    private String sessionTokenApi;

    private static final String JWT_CLAIM_USERNAME = "user_name";
    private static final String JWT_CLAIM_AUTHORITY = "authorities";

    @PostConstruct
    public void init() {
        sessionTokenApi = sessionMgtBaseUrl + "/token/v1/";
    }

    private String sessionToken(String sessionId) {
        try {
            log.info("method=sessionToken with cloud api {}", sessionTokenApi);
            StopWatch sw = new StopWatch();
            sw.start();

            ResponseEntity<SessionTokenResp> response = oAuth2RestTemplate.getForEntity(
                    sessionTokenApi + sessionId, SessionTokenResp.class);

            sw.stop();
            log.info("api=tms_remoteapi_sessionToken, method=sessionToken, statusCode={}, duration={} ms",
                    response.getStatusCode(), sw.getTotalTimeMillis());

            return response.getBody() == null ? null : response.getBody().getToken();
        } catch (RestClientResponseException e) {
            log.warn("method=sessionToken, errMsg=\"remote call sessionToken api {} fail with {}, {}\"",
                    sessionTokenApi, e.getStatusText(), e.getResponseBodyAsString());
        }
        return null;
    }

    public Authentication validateSession(String sessionId) {
        String cloudUserToken = sessionToken(sessionId);
        if (!StringUtils.isBlank(cloudUserToken)) {
            try {
                JWT jwt = JWTParser.parse(cloudUserToken);
                JWTClaimsSet claims = jwt.getJWTClaimsSet();
                return new UsernamePasswordAuthenticationToken(
                        claims.getClaim(JWT_CLAIM_USERNAME),
                        null,
                        claims.getClaim(JWT_CLAIM_AUTHORITY) == null
                                ? null
                                : ((List<?>) claims.getClaim(JWT_CLAIM_AUTHORITY))
                                    .stream()
                                    .map(Object::toString)
                                    .map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toList())
                );
            } catch (Exception e) {
                log.error("method=validateSession, validate session fail with {}", e.getLocalizedMessage());
            }
        }
        return null;
    }
}
