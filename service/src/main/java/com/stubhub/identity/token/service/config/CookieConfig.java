package com.stubhub.identity.token.service.config;

import com.stubhub.identity.token.service.session.MyCookieSerializer;
import com.stubhub.identity.token.service.session.ShapeSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class CookieConfig {

  @Value("${server.servlet.session.cookie.legacy}")
  private String legacyCookieName = "SH_SI";

  @Autowired private ShapeSessionService shapeSessionService;

  @Bean
  public DefaultCookieSerializer cookieSerializer(ServerProperties serverProperties) {
    Session.Cookie cookie = serverProperties.getServlet().getSession().getCookie();
    MyCookieSerializer cookieSerializer =
        new MyCookieSerializer(shapeSessionService, legacyCookieName);
    PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
    map.from(cookie::getName).to(cookieSerializer::setCookieName);
    map.from(cookie::getDomain).to(cookieSerializer::setDomainName);
    map.from(cookie::getPath).to(cookieSerializer::setCookiePath);
    map.from(cookie::getHttpOnly).to(cookieSerializer::setUseHttpOnlyCookie);
    map.from(cookie::getSecure).to(cookieSerializer::setUseSecureCookie);
    map.from(cookie::getMaxAge)
        .to((maxAge) -> cookieSerializer.setCookieMaxAge((int) maxAge.getSeconds()));
    cookieSerializer.setLegacyCookieName(legacyCookieName);
    return cookieSerializer;
  }
}
