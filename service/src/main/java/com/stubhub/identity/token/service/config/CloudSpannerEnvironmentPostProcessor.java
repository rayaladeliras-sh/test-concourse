package com.stubhub.identity.token.service.config;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.util.StringUtils;

/**
 * Inject user provided service as spanner service when it contains tag 'gcp' and 'spanner', eg:
 * <br>
 * {@code cf cups <service-name> -p <credentials> -t "gcp,spanner"}
 *
 * @see
 *     org.springframework.cloud.gcp.autoconfigure.core.cloudfoundry.GcpCloudFoundryEnvironmentPostProcessor
 */
@Slf4j
public class CloudSpannerEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

  private static final Logger logger =
      LoggerFactory.getLogger(CloudSpannerEnvironmentPostProcessor.class);

  private final JsonParser parser = JsonParserFactory.getJsonParser();

  /** Environment variable for VCAP services. */
  public static final String VCAP_SERVICES_ENVVAR = "VCAP_SERVICES";

  private static final String SPRING_CLOUD_GCP_PROPERTY_PREFIX = "spring.cloud.gcp.";

  @SuppressWarnings("unchecked")
  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    if (StringUtils.isEmpty(environment.getProperty(VCAP_SERVICES_ENVVAR))) {
      logger.info("VCAP_SERVICES environment property not set");
      return;
    }

    Map<String, Object> vcapMap =
        this.parser.parseMap(environment.getProperty(VCAP_SERVICES_ENVVAR));
    if (!vcapMap.containsKey("user-provided")) {
      logger.info("VCAP_SERVICES does not contain \"user-provided\" key");
      return;
    }
    List<Object> bindings = (List<Object>) vcapMap.get("user-provided");
    if (bindings == null) {
      logger.info("VCAP_SERVICES's \"user-provided\" key does not contain any value");
      return;
    }

    for (Object obj : bindings) {
      Map<String, Object> binding = (Map<String, Object>) obj;
      String name = (String) binding.get("name");
      // TODO add to property
      if (name.equals("identity-spanner")) {
        environment
            .getPropertySources()
            .addFirst(new PropertiesPropertySource("upsSpannerCf", loadSpannerProperties(binding)));
        return;
      }
    }
  }

  @SuppressWarnings("unchecked")
  private Properties loadSpannerProperties(Map<String, Object> binding) {
    Map<String, String> credentials = (Map<String, String>) binding.get("credentials");
    String prefix = SPRING_CLOUD_GCP_PROPERTY_PREFIX + "spanner.";
    Properties props = new Properties();
    props.put(prefix + "project-id", credentials.get("ProjectId"));
    props.put(prefix + "credentials.encoded-key", credentials.get("PrivateKeyData"));
    props.put(prefix + "instance-id", credentials.get("instance_id"));
    credentials.forEach((s, s2) -> log.info("SpannerProperties:" + s + "=" + s2));
    return props;
  }

  @Override
  public int getOrder() {
    return ConfigFileApplicationListener.DEFAULT_ORDER;
  }
}
