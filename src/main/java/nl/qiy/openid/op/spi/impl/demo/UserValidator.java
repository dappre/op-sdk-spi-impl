/*
 * This work is protected under copyright law in the Kingdom of
 * The Netherlands. The rules of the Berne Convention for the
 * Protection of Literary and Artistic Works apply.
 * Digital Me B.V. is the copyright owner.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.qiy.openid.op.spi.impl.demo;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Throwables;

import io.dropwizard.jackson.Jackson;
import nl.qiy.oic.op.qiy.QiyNodeClient;
import nl.qiy.oic.op.qiy.QiyOAuthUser;
import nl.qiy.openid.op.spi.impl.demo.OpSdkSpiImplConfiguration.CardLoginOption;

/**
 * TODO: friso should have written a comment here to tell us what this class does
 *
 * @author Friso Vrolijken
 * @since 29 jul. 2016
 */
public class UserValidator implements Serializable {
    /**
     * Generated
     */
    private static final long serialVersionUID = 1L;
    /** 
     * Standard SLF4J Logger 
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UserValidator.class);
    private static final ObjectWriter MAP_WRITER = Jackson.newObjectMapper().writerFor(HashMap.class);
    private static final ObjectWriter SET_WRITER = Jackson.newObjectMapper().writerFor(HashSet.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private static Client jaxrsClient;
    private static URI dappreURI;

    private final QiyOAuthUser userImpl;

    private URI referenceUri;
    private static byte[] welcomeMessageBytes;

    /**
     * Constructor for UserValidator
     * 
     * @param userImpl
     *            the user to be validated
     */
    public UserValidator(QiyOAuthUser userImpl) {
        super();
        this.userImpl = userImpl;
    }

    public static synchronized void setJaxRsClient(Client client) {
        jaxrsClient = client;
    }

    private static URI getDappreURI() {
        try {
            if (dappreURI == null) {
                dappreURI = OpSdkSpiImplConfiguration.getInstance().dappreBaseURI.toURI();
            }
    
            return dappreURI;
        } catch (URISyntaxException e) {
            LOGGER.warn("Error while doing getDappreURI", e);
            throw Throwables.propagate(e);
        }
    }

    private static Map<String, Object> createConsentMessage(String shareId) {
        // allow the key to be cached for 1.000.000 ms = 1.000 s = ca. 16 min.
        URI pkURI = getDappreURI()
                .resolve("cardowner/messages/key/" + shareId + "?" + (System.currentTimeMillis() / 1_000_000));
        // @formatter:off
        Response response = jaxrsClient
            .target(pkURI)
            .request(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, QiyNodeClient.getAuthHeader(null))
            .get();
        // @formatter:on
        if (response.getStatusInfo().getFamily() != Status.Family.SUCCESSFUL) {
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                MessageDAO.setIgnorable(shareId);
            }
            LOGGER.error("No success {} while requesting key for {}, remove reference?", response.getStatus(), shareId);
            return null;
        }
        try {
            String pkRepresentation = response.readEntity(String.class);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(pkRepresentation));
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);
            byte[] secret = new byte[32]; // which is 512 bits, 8 bits per byte => 32 bytes
            byte[] iv = new byte[16];
            RANDOM.nextBytes(secret);
            RANDOM.nextBytes(iv);
            byte[] message = MessageCrypto
                    .encryptSymmetric(getWelcomemessagebytes(), secret, iv);
            // TODO [RB 20160808] Temporary. Once the iOS and Android clients supporting padded encryption have been
            // around for some time this should be changed to encryptAsymmetricWithPadding
            // [FV] Target: 20161201
            byte[] encryptedIV = MessageCrypto.encryptAsymmetricNoPadding(publicKey, iv);
            byte[] encryptedSecret = MessageCrypto.encryptAsymmetricNoPadding(publicKey, secret);

            LOGGER.info("Sending consent request iv length: {}, secret length: {}", iv.length, secret.length);

            Map<String, Object> msgRep = new HashMap<>();
            msgRep.put("shareId", shareId);
            msgRep.put("iv", Base64.getEncoder().encodeToString(encryptedIV));
            msgRep.put("key", Base64.getEncoder().encodeToString(encryptedSecret));
            msgRep.put("message", Base64.getEncoder().encodeToString(message));

            return msgRep;
        } catch (GeneralSecurityException e) {
            LOGGER.warn("Error while doing createConsentMessage", e);
            throw Throwables.propagate(e);
        }
    }

    private static byte[] getWelcomemessagebytes() {
        byte[] result = welcomeMessageBytes;
        if (result == null) {
            result = OpSdkSpiImplConfiguration.getInstance().welcomeMessage.getBytes();
            welcomeMessageBytes = result;
        }
        return result;
    }

    /**
     * Note that consent can only be given, it can not be taken away (per share id). To revoke consent, the user has to
     * delete the card of the organisation
     */
    private static void processPendingMessages() {
        URI messagesURI = getDappreURI().resolve("cardowner/messages/?" + System.currentTimeMillis());
        Optional<List<Map<String, Object>>> list = QiyNodeClient.getList(messagesURI);
        list.ifPresent(UserValidator::processMessages);
    }

    /**
     * @param messages
     *            messages retrieved from the server
     */
    private static void processMessages(List<Map<String, Object>> messages) {
        LOGGER.debug("processing {} messages", messages.size());
        for (Map<String, Object> message : messages) {
            String shareId = (String) message.get("shareId");
            if (Boolean.TRUE.equals(message.get("sender"))) {
                MessageDAO.setSent(shareId);
                LOGGER.debug("skipping message that we sent");
                continue;
            }

            // else
            MessageDAO.setReceived(shareId);

            byte[] secret = MessageCrypto.decryptAsymmetric((String) message.get("key"), 32);
            byte[] iv = MessageCrypto.decryptAsymmetric((String) message.get("iv"), 16);
            byte[] encMsg = Base64.getDecoder().decode((String) message.get("message"));
            byte[] decrypted = MessageCrypto.decryptSymmetric(encMsg, secret, iv);
            String received = new String(decrypted).trim();

            LOGGER.info("received payload {} from message {}", received, message.get("messageId"));
            // @formatter:off
            if ("yes".equalsIgnoreCase(received) || 
                "ok".equalsIgnoreCase(received)  || 
                "ja".equalsIgnoreCase(received)  ){// @formatter:on
                MessageDAO.setConsented(shareId);
            }
        }
    }

    public QiyOAuthUser getValidatedUser() {
        if (userImpl == null) {
            return null;
        }
        // else we need to revalidate if we may still use this user, so unset the claims we've got
        userImpl.unsetClaims();
        if (getReferenceUri() == null) {
            return null;
        }
        // else
        // connection exists, find out which shareIds are associated with it
        // make sure the consent request is sent for each of them
        Set<String> shareIds = getShareIds();
        userImpl.setShareIds(shareIds);
        String welcomeMessage = OpSdkSpiImplConfiguration.getInstance().welcomeMessage;
        if (welcomeMessage != null) {
            for (String shareId : shareIds) {
                ensureWelcomeSent(shareId);
            }
            processPendingMessages();
        }
        
        setClaimsFromCards(shareIds);

        return userImpl.getClaims() == null ? null : userImpl;
    }

    /**
     * @param shareId
     */
    private static void ensureWelcomeSent(String shareId) {
        if (MessageDAO.isSent(shareId)) {
            return;
        }
        LOGGER.debug("requesting consent");
        // even if this fails; don't try again
        MessageDAO.setSent(shareId);
        Map<String, Object> consentMessage = createConsentMessage(shareId);
        if (consentMessage == null) {
            return;
        }
        try {
            byte[] data = MAP_WRITER.writeValueAsBytes(consentMessage);
            URI messagesURI = getDappreURI().resolve("cardowner/messages/");
            // @formatter:off
            Response response = jaxrsClient
                .target(messagesURI)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, QiyNodeClient.getAuthHeader(data))
                .post(Entity.json(consentMessage));
            // @formatter:on
            if (response.getStatusInfo().getFamily() != Status.Family.SUCCESSFUL) {
                LOGGER.error("No succes while posting message: {} {}", response.getStatus(),
                        response.readEntity(String.class));
                throw new IllegalArgumentException("Did not succeed in posting consent message");
            }
            // else
        } catch (JsonProcessingException e) {
            LOGGER.warn("Error while doing sendConsentMessage", e);
            throw Throwables.propagate(e);
        }
    }

    /**
     * @param shareIds
     */
    private void setClaimsFromCards(Set<String> shareIds) {
        try {
            Set<String> consentedShareIds = shareIds;
            if (OpSdkSpiImplConfiguration.getInstance().cardLoginOption != CardLoginOption.NO_CARD) {
                // @formatter:off
                consentedShareIds = shareIds
                    .stream()
                    .filter(MessageDAO::hasConsent)
                    .collect(Collectors.toSet()); // @formatter:on
            }
            if (consentedShareIds.isEmpty()) {
                return;
            }
            // else
            URI cardList = getDappreURI().resolve("cardowner/v2/othercards/cards");
            byte[] data = SET_WRITER.writeValueAsBytes(consentedShareIds);
            // @formatter:off
            Response response = jaxrsClient
                .target(cardList)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, QiyNodeClient.getAuthHeader(data))
                    .post(Entity.json(consentedShareIds));// @formatter:on

            if (response.getStatusInfo().getFamily() == Status.Family.SUCCESSFUL) {
                List<Map<String, Object>> cards = response.readEntity(List.class);
                LOGGER.debug("{} shared cards fetched", cards.size());
                if (cards.isEmpty()) {
                    return;
                }
                // multiple cards should all have the same values for fields, so merge them
                Map<String, Object> unifiedCard = new HashMap<>();
                if (OpSdkSpiImplConfiguration.getInstance().cardLoginOption != CardLoginOption.NO_CARD) {
                    cards.forEach(unifiedCard::putAll);
                } else {
                    unifiedCard.put("no-user-info-requested", "by config");
                }
                userImpl.setClaims(unifiedCard);
            }
        } catch (JsonProcessingException e) {
            LOGGER.warn("Error while doing fetchCards", e);
            throw Throwables.propagate(e);
        }
    }

    /**
     * Gets all the shareIds; i.e. gets all the values of the references for the dappre protocol
     * 
     * @return see description
     */
    private Set<String> getShareIds() {
        // @formatter:off
        URI uri = UriBuilder
                .fromUri(getReferenceUri())
                .queryParam("nocache", System.currentTimeMillis())
                .build();
        return QiyNodeClient
                .getList(uri)
                .orElseThrow(IllegalStateException::new)
                .stream()
                .filter(e -> "https://protocols.qidida.nl/dappre/v.1.0.0".equals(e.get("protocol")))
                .map(m -> (String) m.get("reference"))
                .map(Base64.getDecoder()::decode)
                .map(String::new)
                .filter(MessageDAO::dontIgnore)
                .collect(Collectors.toSet()); // @formatter:on
    }

    /**
     * Simple getter
     * 
     * @return the userImpl
     */
    public QiyOAuthUser getUser() {
        return userImpl;
    }

    /**
     * Lazy getter
     * 
     * @return the link to all the references received for this connection
     */
    public URI getReferenceUri() {
        if (referenceUri == null) {
            QiyConnection connection = QiyNodeClient.get(userImpl.getConnectionUri(), QiyConnection.class);
            if (connection == null) {
                return null;
            }
            Map<String, URI> links = connection.links;
            referenceUri = links.get("references");
        }
        return referenceUri; 
    }
}
