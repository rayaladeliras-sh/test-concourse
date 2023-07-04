package com.stubhub.identity.token.test.util;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.*;
import lombok.SneakyThrows;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

public class ShapeAPI {
  private static String appToken;

  private static String base = String.format("https://api-shape.%s.com", SHEnv.getEnv());

  private static RestTemplate restTemplate = createInsecureRestTemplate();

  public static String getBase() {
    return base;
  }

  @SneakyThrows
  private static RestTemplate createInsecureRestTemplate() {
    // Create a trust manager that does not validate certificate chains
    TrustManager[] trustAllCerts =
        new TrustManager[] {
          new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
              return null;
            }

            public void checkClientTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {}

            public void checkServerTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {}
          }
        };

    // Install the all-trusting trust manager
    SSLContext sc = SSLContext.getInstance("SSL");
    sc.init(null, trustAllCerts, new java.security.SecureRandom());
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

    HttpsURLConnection.setDefaultHostnameVerifier(
        new HostnameVerifier() {
          public boolean verify(String hostname, SSLSession session) {
            return true;
          }
        });
    SimpleClientHttpRequestFactory factory =
        new SimpleClientHttpRequestFactory() {
          protected void prepareConnection(HttpURLConnection connection, String httpMethod)
              throws IOException {
            super.prepareConnection(connection, httpMethod);
            connection.setInstanceFollowRedirects(false);
          }
        };
    factory.setOutputStreaming(false);
    RestTemplate client = new RestTemplate(new BufferingClientHttpRequestFactory(factory));
    for (HttpMessageConverter<?> converter : client.getMessageConverters()) {
      if (converter instanceof StringHttpMessageConverter) {
        StringHttpMessageConverter c = (StringHttpMessageConverter) converter;
        c.setWriteAcceptCharset(false);
        break;
      }
    }
    client
        .getInterceptors()
        .add(
            new ClientHttpRequestInterceptor() {

              @Override
              public ClientHttpResponse intercept(
                  HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                  throws IOException {
                return execution.execute(request, body);
              }
            });
    client.getInterceptors().add(new LoggingInterceptor());
    return client;
  }

  private static RequestSpecification given() {
    RequestSpecification specification =
        new RequestSpecBuilder()
            .addFilter(new RequestLoggingFilter())
            .addFilter(new ResponseLoggingFilter())
            .setRelaxedHTTPSValidation()
            .setBaseUri(base)
            .build();

    return RestAssured.given().spec(specification);
  }

  //  public static String getDefaultAppToken() {
  //    if (appToken == null) {
  //      synchronized (ShapeAPI.class) {
  //        if (appToken == null)
  //          appToken =
  //              ShapeAPI.exchangeToken("JYf0azPrf1RAvhUhpGZudVU9bBEa")
  //                  .contentType(ContentType.JSON)
  //                  .extract()
  //                  .path("jwt");
  //      }
  //    }
  //    return appToken;
  //  }

  public static ValidatableResponse token(
      String username, String password, String clientId, String clientSecret) {
    return given()
        .contentType(ContentType.URLENC)
        .formParam("grant_type", "password")
        .formParam("username", username)
        .formParam("password", password)
        .formParam("client_id", clientId)
        .formParam("client_secret", clientSecret)
        .when()
        .post("/oauth/token")
        .then();
  }

  public static ValidatableResponse login(String email, String password) {
    return RestAssured.given()
        .spec(
            new RequestSpecBuilder()
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .setRelaxedHTTPSValidation()
                .build())
        .contentType(ContentType.URLENC)
        .formParam("username", email)
        .formParam("password", password)
        .formParam("consumer_key", "iXyhYOR6pJl9TtYyYSzoiBXiB6sa")
        .formParam("consumer_secret", "6_9n4Hqz7j8jGQ0P6A6gjcGWXCka")
        .when()
        // app_type=NATIVE will make it easier to get user jwt token
        .post(String.format("https://api-shape.%s.com/iam/login?app_type=NATIVE", SHEnv.getEnv()))
        .then();
  }

  //  public static ValidatableResponse exchangeToken(String access_token) {
  //    return given()
  //        .contentType(ContentType.JSON)
  //        .header("Authorization", "Bearer JYf0azPrf1RAvhUhpGZudVU9bBEa")
  //        .body(ImmutableMap.<String, String>builder().put("token", access_token).build())
  //        .when()
  //        .post("/oauth/extoken/v1")
  //        .then();
  //  }

  /** @return guid */
  public static String createUser(String email, String password) {
    String body =
        "{\n"
            + "\t\"customer\": {\n"
            + "           \"emailAddress\": \""
            + email
            + "\",\n"
            + "           \"password\": \""
            + password
            + "\",\n"
            + "           \"defaultContact\": {\n"
            + "                    \"name\": {\n"
            + "                             \"firstName\": \"NiceFN\",\n"
            + "                             \"lastName\": \"AwesomeLN\"\n"
            + "                     },\n"
            + "                    \"companyName\": \"testing\",\n"
            + "                    \"address\": {\n"
            + "                           \"line1\": \"123, main st\",\n"
            + "                           \"line2\": \"\",\n"
            + "                           \"city\": \"san mateo\",\n"
            + "                           \"state\": \"CA\",\n"
            + "                           \"country\": \"US\",\n"
            + "                           \"postalCode\": \"94404\"\n"
            + "                     },\n"
            + "                    \"phone\": {\n"
            + "\n"
            + "                           \"phoneNumber\": \"4556224567\",\n"
            + "                           \"countryCallingCode\": \"001\",\n"
            + "                           \"ext\": \"625\"\n"
            + "                      }\n"
            + "             },\n"
            + "\n"
            + "            \"marketingEmailOptIn\":\"Y\",\n"
            + "            \"acceptedAgreements\": [\n"
            + "               {\n"
            + "                        \"destination\": \"US\"\n"
            + "               }\n"
            + "            ]\n"
            + "       }\n"
            + "}";

    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("Authorization", "Bearer JYf0azPrf1RAvhUhpGZudVU9bBEa");
    HttpEntity<String> entity = new HttpEntity<String>(body, headers);
    ResponseEntity<HashMap> response =
        restTemplate.exchange(base + "/user/customers/v2", HttpMethod.POST, entity, HashMap.class);
    return ((Map) response.getBody().get("customer")).get("id").toString();
    //    return response.getBody().get("customer.id").toString();
    //    return given()
    //        .contentType(ContentType.JSON)
    //        .header("Authorization", "Bearer JYf0azPrf1RAvhUhpGZudVU9bBEa")
    //        .body(body)
    //        .when()
    //        .post("/user/customers/v2")
    //        .then()
    //        .statusCode(201)
    //        .extract()
    //        .path("customer.id");
  }
}
