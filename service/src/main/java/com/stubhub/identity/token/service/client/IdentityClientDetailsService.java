package com.stubhub.identity.token.service.client;

import com.stubhub.identity.token.service.exception.ExceptionSupplier;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

@Slf4j
@Service
public class IdentityClientDetailsService
    implements ClientDetailsService, ClientRegistrationService {

  @Autowired private OAuthClientDetailsRepository clientDetailsRepository;

  @Autowired private PasswordEncoder encoder;

  /**
   * This will only load active client, not all. Check {@link
   * IdentityClientDetailsService#listAllClientDetails()} for loading all
   */
  @Cacheable("clients")
  @Override
  public OAuthClientDetails loadClientByClientId(String clientId)
      throws ClientRegistrationException {

    StopWatch sw = new StopWatch();
    sw.start();
    Optional<OAuthClientDetails> result =
        clientDetailsRepository.findByClientIdAndStatus(clientId, OAuthClientDetails.Status.ACTIVE);
    OAuthClientDetails clientDetails =
        result.orElseThrow(
            new ExceptionSupplier(
                new ClientRegistrationException(
                    "No active client found with client id:" + clientId)));
    sw.stop();
    log.info(
        "method=loadClientByClientId, duration={} ms, message=\"find active client {} from spanner database \"",
        sw.getTotalTimeMillis(),
        clientId);
    return clientDetails;
  }

  public OAuthClientDetails findById(String clientId) throws ClientRegistrationException {
    Optional<OAuthClientDetails> result = clientDetailsRepository.findById(clientId);
    log.info("method=findById, message=\"find client {} from spanner database.\"", clientId);
    return result.orElseThrow(
        new ExceptionSupplier(
            new ClientRegistrationException("No client found with client id:" + clientId)));
  }

  @Override
  @Transactional
  public void addClientDetails(ClientDetails clientDetails) throws ClientAlreadyExistsException {
    if (!(clientDetails instanceof OAuthClientDetails)) {
      throw new IllegalArgumentException(
          "Invalid instance type, expect OAuthClientDetails, but found "
              + clientDetails.getClass());
    }
    OAuthClientDetails oAuthClientDetails = (OAuthClientDetails) clientDetails;
    oAuthClientDetails.setCreateAt(Date.from(Instant.now()));
    oAuthClientDetails.setClientSecret(encoder.encode(oAuthClientDetails.getClientSecret()));
    log.info(
        "method=addClientDetails, message=\"add client: {} into spanner database.\"",
        clientDetails.getClientId());
    clientDetailsRepository.save(oAuthClientDetails);
  }

  @Override
  @Transactional
  public void updateClientDetails(ClientDetails clientDetails) throws NoSuchClientException {
    if (!(clientDetails instanceof OAuthClientDetails)) {
      throw new IllegalArgumentException(
          "Invalid instance type, expect OAuthClientDetails, but found "
              + clientDetails.getClass());
    }
    log.info(
        "method=updateClientDetails, message=\"update client: {} into spanner database.\"",
        clientDetails.getClientId());
    clientDetailsRepository.save((OAuthClientDetails) clientDetails);
  }

  @Override
  @Transactional
  public void updateClientSecret(String clientId, String secret) throws NoSuchClientException {
    OAuthClientDetails oAuthClientDetails = findById(clientId);
    oAuthClientDetails.setClientSecret(encoder.encode(secret));
    log.info(
        "method=updateClientSecret, message=\"update client: {} change client secret into spanner database.\"",
        clientId);
    clientDetailsRepository.save(oAuthClientDetails);
  }

  @Override
  @Transactional
  public void removeClientDetails(String clientId) throws NoSuchClientException {
    OAuthClientDetails oAuthClientDetails = loadClientByClientId(clientId);
    oAuthClientDetails.setStatus(OAuthClientDetails.Status.INACTIVE);
    log.info(
        "method=removeClientDetails, message=\"remove client: {} inactive client into spanner database.\"",
        clientId);
    clientDetailsRepository.save(oAuthClientDetails);
  }

  @Transactional
  public void delete(String clientId) {
    clientDetailsRepository.deleteById(clientId);
    log.info(
        "method=deleteClientDetails, message=\"delete client: {}  from spanner database.\"",
        clientId);
  }

  @Override
  public List<ClientDetails> listClientDetails() {
    return listAllClientDetails()
        .stream()
        .map(ClientDetails.class::cast)
        .collect(Collectors.toList());
  }

  public List<OAuthClientDetails> listAllClientDetails() {
    return StreamSupport.stream(clientDetailsRepository.findAll().spliterator(), false)
        .collect(Collectors.toList());
  }

  public OAuthClientDetails findByName(String clientName) {
    Optional<OAuthClientDetails> client =
        clientDetailsRepository.findByClientNameAndStatus(
            clientName, OAuthClientDetails.Status.ACTIVE);
    log.info("method=findByName, message=\"find client: {} from spanner database\"", clientName);
    return client.orElseThrow(
        new ExceptionSupplier(
            new ClientRegistrationException("No client found with clientName :" + clientName)));
  }
}
