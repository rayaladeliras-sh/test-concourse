package com.stubhub.identity.token.service.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.stubhub.identity.token.service.client.OAuthClientDetails;
import com.stubhub.identity.token.service.client.OAuthClientDetailsDto;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NullAwareBeanUtilsTest {

  @Test
  public void copyProperty() {
    OAuthClientDetailsDto dto = new OAuthClientDetailsDto();
    dto.setClientId("1");
    dto.setClientSecret("1");
    dto.setScope(Arrays.asList("scope"));
    dto.setStatus(OAuthClientDetails.Status.INACTIVE);

    OAuthClientDetails details = new OAuthClientDetails();
    details.setStatus(OAuthClientDetails.Status.ACTIVE);
    details.setClientSecret("2");
    details.setLastUpdateBy("test");
    details.setClientOwner("test");
    details.setAuthorities(Arrays.asList("authorites"));

    NullAwareBeanUtils.copyNonNullProperties(dto, details);

    assertEquals(details.getClientId(), "1");
    assertEquals(details.getClientSecret(), "1");
    assertEquals(details.getLastUpdateBy(), "test");
    assertEquals(details.getClientOwner(), "test");
    assertEquals(details.getStatus().name(), OAuthClientDetails.Status.INACTIVE.name());
    assertTrue(details.getScope().contains("scope"));
    details
        .getAuthorities()
        .stream()
        .findFirst()
        .ifPresent(grantedAuthority -> assertEquals(grantedAuthority.getAuthority(), "authorites"));
  }
}
