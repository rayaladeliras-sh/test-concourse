package com.stubhub.identity.token.service.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stubhub.identity.token.service.utils.NullAwareBeanUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class OAuthClientRegistrationService {

  @Autowired private IdentityClientDetailsService identityClientDetailsService;
  @Autowired private PasswordEncoder encoder;
  @Autowired private ModelMapper modelMapper;

  @SneakyThrows(IllegalArgumentException.class)
  public void addClientDetails(OAuthClientDetailsDto client) {
    StopWatch sw = new StopWatch();
    sw.start();
    log.info("Register client with payload client={}", client);
    if (StringUtils.isEmpty(client.getClientId())) {
      throw new IllegalArgumentException("Client id is empty.");
    }
    if (StringUtils.isEmpty(client.getClientSecret())) {
      throw new IllegalArgumentException("Client secret is empty.");
    }
    if (StringUtils.isEmpty(client.getClientOwner())) {
      throw new IllegalArgumentException("Client owner is empty.");
    }
    checkAdditionalInfo(client.getAdditionalInformation());
    OAuthClientDetails oAuthClientDetails = null;
    try {
      oAuthClientDetails = identityClientDetailsService.findById(client.getClientId());
    } catch (ClientRegistrationException e) {
      log.info("No client was found, it's ok to register a new client");
    }

    if (null != oAuthClientDetails) {
      throw new ClientRegistrationException(
          "The client id '" + client.getClientId() + "' already exist in database.");
    }

    try {
      oAuthClientDetails = identityClientDetailsService.findByName(client.getClientName());
    } catch (ClientRegistrationException e) {
      log.info("No client was found, it's ok to register a new client");
    }

    if (null != oAuthClientDetails) {
      throw new ClientRegistrationException(
          "The clientName '" + client.getClientName() + "' already exist in database.");
    }

    oAuthClientDetails = new OAuthClientDetails();
    NullAwareBeanUtils.copyProperties(client, oAuthClientDetails);
    identityClientDetailsService.addClientDetails(oAuthClientDetails);
    sw.stop();
    log.debug(
        "--PERF--add client details successfully with duration: {} ms", sw.getTotalTimeMillis());
  }

  @SneakyThrows(IllegalArgumentException.class)
  public OAuthClientDetailsDto updateClientDetails(OAuthClientDetailsDto client) {
    StopWatch sw = new StopWatch();
    sw.start();
    log.info("Update client with payload client={}", client);
    // don't allow set client secret in update
    if (!StringUtils.isEmpty(client.getClientSecret())) {
      throw new IllegalArgumentException("Client secret is not allowed modified in update.");
    }
    if (StringUtils.isEmpty(client.getClientId())) {
      throw new IllegalArgumentException("Client id is required for update.");
    }
    checkAdditionalInfo(client.getAdditionalInformation());
    OAuthClientDetails oAuthClientDetails =
        identityClientDetailsService.findById(client.getClientId());
    NullAwareBeanUtils.copyNonNullProperties(client, oAuthClientDetails);
    identityClientDetailsService.updateClientDetails(oAuthClientDetails);
    sw.stop();
    log.debug(
        "--PERF--update client details successfully with duration: {} ms", sw.getTotalTimeMillis());
    return convertToDto(oAuthClientDetails);
  }

  @SneakyThrows(ClientRegistrationException.class)
  public void updateClientSecret(String clientId, OAuthClientSecretDto oAuthClientSecret) {
    StopWatch sw = new StopWatch();
    sw.start();
    log.info("Update secret for client by clientId={}", clientId);
    OAuthClientDetails clientDetails = identityClientDetailsService.loadClientByClientId(clientId);
    if (encoder.matches(oAuthClientSecret.getOldSecret(), clientDetails.getClientSecret())) {
      identityClientDetailsService.updateClientSecret(clientId, oAuthClientSecret.getNewSecret());
    } else {
      throw new ClientRegistrationException("Old client secret does not match !!!");
    }
    sw.stop();
    log.debug(
        "--PERF--update client secret successfully with duration: {} ms", sw.getTotalTimeMillis());
  }

  public OAuthClientDetailsDto getClientById(String clientId) {
    StopWatch sw = new StopWatch();
    sw.start();
    log.info("Get client by clientId={}", clientId);
    OAuthClientDetailsDto oAuthClientDetailsDto =
        convertToDto(identityClientDetailsService.findById(clientId));
    sw.stop();
    log.debug(
        "--PERF--get client by id successfully with duration: {} ms", sw.getTotalTimeMillis());
    return oAuthClientDetailsDto;
  }

  public List<OAuthClientDetailsDto> getAllClients() {
    StopWatch sw = new StopWatch();
    sw.start();
    log.info("Get all client information");
    List<OAuthClientDetailsDto> oAuthClientDetailsDtos =
        identityClientDetailsService
            .listAllClientDetails()
            .stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    sw.stop();
    log.debug("--PERF--get all clients successfully with duration: {} ms", sw.getTotalTimeMillis());
    return oAuthClientDetailsDtos;
  }

  public void delete(String clientId) {
    StopWatch sw = new StopWatch();
    sw.start();
    log.info("Delete client by clientId={}", clientId);
    identityClientDetailsService.delete(clientId);
    sw.stop();
    log.debug("--PERF--delete client successfully with duration: {} ms", sw.getTotalTimeMillis());
  }

  private OAuthClientDetailsDto convertToDto(OAuthClientDetails oAuthClientDetails) {
    OAuthClientDetailsDto oAuthClientDetailsDto =
        modelMapper.map(oAuthClientDetails, OAuthClientDetailsDto.class);
    return oAuthClientDetailsDto;
  }

  private void checkAdditionalInfo(String additionalInformation) {
    if (!StringUtils.isEmpty(additionalInformation)) {
      try {
        new ObjectMapper()
            .readValue(additionalInformation, new TypeReference<Map<String, Object>>() {});
      } catch (IOException e) {
        throw new IllegalArgumentException("The format of additional information is not correct");
      }
    }
  }
}
