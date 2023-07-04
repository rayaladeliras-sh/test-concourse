package com.stubhub.identity.token.db;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.stubhub.identity.token.service.client.IdentityClientDetailsService;
import com.stubhub.identity.token.service.client.OAuthClientDetails;
import com.stubhub.identity.token.service.client.OAuthClientDetailsRepository;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientRegistrationException;

@RunWith(MockitoJUnitRunner.class)
public class IdentityClientDetailsServiceTest {

  @Mock private PasswordEncoder encoder;

  @Mock private OAuthClientDetailsRepository oAuthClientDetailsRepository;

  @InjectMocks private IdentityClientDetailsService identityClientDetailsService;

  @Test
  public void testAddClientDetails() {

    OAuthClientDetails clientDetails =
        new OAuthClientDetails()
            .toBuilder()
            .clientId("test_builder")
            .clientSecret("secret")
            .build();
    identityClientDetailsService.addClientDetails(clientDetails);
    verify(encoder, times(1)).encode("secret");
    verify(oAuthClientDetailsRepository, times(1)).save(clientDetails);
  }

  @Test
  public void testLoadClientByClientId() {
    OAuthClientDetails clientDetails =
        new OAuthClientDetails()
            .toBuilder()
            .clientId("test_builder")
            .clientSecret("secret")
            .status(OAuthClientDetails.Status.ACTIVE)
            .build();

    when(oAuthClientDetailsRepository.findByClientIdAndStatus(
            "test_builder", OAuthClientDetails.Status.ACTIVE))
        .thenReturn(Optional.of(clientDetails));

    OAuthClientDetails clientDetails1 =
        identityClientDetailsService.loadClientByClientId("test_builder");
    assertEquals(clientDetails.getClientId(), clientDetails1.getClientId());
    assertEquals(OAuthClientDetails.Status.ACTIVE, clientDetails1.getStatus());
  }

  @Test(expected = ClientRegistrationException.class)
  public void testLoadClientByClientId_withoutRecord() {
    OAuthClientDetails clientDetails =
        new OAuthClientDetails()
            .toBuilder()
            .clientId("test_builder")
            .clientSecret("secret")
            .status(OAuthClientDetails.Status.ACTIVE)
            .build();

    when(oAuthClientDetailsRepository.findByClientIdAndStatus(
            "test_builder", OAuthClientDetails.Status.ACTIVE))
        .thenReturn(Optional.empty());

    OAuthClientDetails clientDetails1 =
        identityClientDetailsService.loadClientByClientId("test_builder");
    assertEquals(clientDetails.getClientId(), clientDetails1.getClientId());
    assertEquals(OAuthClientDetails.Status.ACTIVE, clientDetails1.getStatus());
  }

  @Test
  public void testUpdateClientDetails() {
    OAuthClientDetails clientDetails =
        new OAuthClientDetails()
            .toBuilder()
            .clientId("test_builder")
            .clientSecret("secret")
            .status(OAuthClientDetails.Status.ACTIVE)
            .build();
    identityClientDetailsService.updateClientDetails(clientDetails);
  }
}
