package com.stubhub.identity.token.service.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stubhub.identity.token.service.client.OAuthClientDetailsDto;
import com.stubhub.identity.token.service.client.OAuthClientRegistrationService;
import com.stubhub.identity.token.service.client.OAuthClientSecretDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
@WebMvcTest(ClientRegistrationController.class)
public class ClientRegistrationControllerTest {

  private static final String BASE_PATH = "/oauth/v1/clients";

  @Mock private OAuthClientRegistrationService service;
  @Mock private ModelMapper modelMapper;
  @InjectMocks private ClientRegistrationController clientRegistrationController;

  private MockMvc mvc;
  private ObjectMapper mapper = new ObjectMapper();

  @Before()
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mvc = MockMvcBuilders.standaloneSetup(clientRegistrationController).build();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void registerClientWithAdminUser_shouldSucceedWith200() throws Exception {
    OAuthClientDetailsDto oAuthClientDetailsDto =
        OAuthClientDetailsDto.builder()
            .clientId("test")
            .clientSecret("test")
            .clientOwner("DL-unitTest")
            .build();
    String body = mapper.writeValueAsString(oAuthClientDetailsDto);
    mvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void updateClientWithAdminUser_shouldReturn200() throws Exception {
    OAuthClientDetailsDto oAuthClientDetailsDto =
        OAuthClientDetailsDto.builder()
            .clientId("test")
            .clientSecret("test")
            .clientOwner("DL-unitTest")
            .build();
    String body = mapper.writeValueAsString(oAuthClientDetailsDto);
    mvc.perform(put(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void resetSecretWithAdmin_shouldReturn200() throws Exception {
    OAuthClientSecretDto clientSecret = new OAuthClientSecretDto();
    clientSecret.setOldSecret("test_old");
    clientSecret.setNewSecret("test_new");
    mvc.perform(
            put(BASE_PATH + "/test/secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(clientSecret)))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void findClient_shouldReturn200() throws Exception {
    mvc.perform(get(BASE_PATH + "/test")).andExpect(status().isOk());
  }
}
