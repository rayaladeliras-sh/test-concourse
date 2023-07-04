package com.stubhub.identity.token.service.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Controller
public class LoginController {

  @Value("${speedy.central.login.url}")
  private String loginUrl;

  @Value("${site.home.page}")
  private String homepage;

  private final RequestCache requestCache = new HttpSessionRequestCache();

  @GetMapping("/oauth/login")
  public void getLoginPage(HttpServletRequest request, HttpServletResponse response) {
    try {
      String redirectUri;
      SavedRequest savedRequest = requestCache.getRequest(request, response);

      if (null != savedRequest) {
        String externalUri = request.getHeader("authorize_uri");

        if (!StringUtils.isEmpty(externalUri)) {
          redirectUri = createRedirectUriFrom(savedRequest, externalUri);
        } else {
          redirectUri = URLEncoder.encode(savedRequest.getRedirectUrl(), "utf-8");
        }
        log.info("method=getLoginPage request redirect uri is {}", redirectUri);
      } else {
        redirectUri = homepage;
        log.info("method=getLoginPage generate default redirect uri is {}", redirectUri);
      }
      response.sendRedirect(loginUrl + "?redirect=" + redirectUri);
    } catch (IOException e) {
      log.error("redirect to login page fail:{}", e.getLocalizedMessage(), e);
      throw new IllegalStateException("redirect to login page fail");
    }
  }

  private String createRedirectUriFrom(SavedRequest savedRequest, String externalUri)
      throws UnsupportedEncodingException {
    String redirectUri;
    UriComponents uriComponents =
        UriComponentsBuilder.fromUriString(savedRequest.getRedirectUrl()).build();
    String query = "?from_login=true";
    query =
        StringUtils.isEmpty(uriComponents.getQuery())
            ? query
            : query + "&" + uriComponents.getQuery();
    redirectUri = URLEncoder.encode(externalUri + query, "utf-8");

    return redirectUri;
  }
}
