package com.stubhub.identity.token.service.dye;

import java.io.IOException;
import java.util.UUID;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class DyeFilter implements Filter {

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {

    String dyePath = getDyePath(req);
    if (!StringUtils.isEmpty(dyePath)) {
      ((HttpServletResponse) res).setHeader(DyeConstants.DYE_PATH, dyePath);
      MDC.put(DyeConstants.DYE_KEY, dyePath);
    }

    chain.doFilter(req, res);
  }

  private String getDyePath(ServletRequest req) {
    StringBuilder builder = new StringBuilder();
    String dyePath = ((HttpServletRequest) req).getHeader(DyeConstants.DYE_PATH);
    if (!StringUtils.isEmpty(dyePath)) {
      builder.append(dyePath);
    }
    String requestId = ((HttpServletRequest) req).getHeader("X-Cloud-Trace-Context");
    if (!StringUtils.isEmpty(requestId)) {
      if (builder.length() != 0) {
        builder.append("_");
      }
      if (requestId.contains("/")) {
        builder.append(requestId.split("/")[0]);
      } else {
        builder.append(requestId);
      }
    }

    String dye = builder.toString();
    if (!StringUtils.isEmpty(dye)) {
      return dye;
    } else {
      return "[" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "]";
    }
  }
}
