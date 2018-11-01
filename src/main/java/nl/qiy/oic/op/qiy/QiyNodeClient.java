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

package nl.qiy.oic.op.qiy;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.ProviderException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.imageio.ImageIO;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.glassfish.jersey.media.sse.EventInput;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Preconditions;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import nl.qiy.oic.op.service.ConfigurationService;
import nl.qiy.openid.op.spi.impl.config.CryptoConfig;
import nl.qiy.openid.op.spi.impl.config.OpSdkSpiImplConfiguration;

/**
 * Handles the communication with a QiyNode
 *
 * @author Friso Vrolijken
 * @since 29 apr. 2016
 */
public class QiyNodeClient {
    /**
     * 
     */
    private static final String NO_CT_SET = "No connect token has been set";
    /**
     * Standard SLF4J Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(QiyNodeClient.class);
    private static final ObjectWriter MAP_WRITER = new ObjectMapper().writerFor(HashMap.class);
    private static Map<String, Object> cardShareData = null;

    private static OpSdkSpiImplConfiguration config = null;
    private static Client jaxrsClient = null;
    private static byte[] nodeIdBytes = null;
    private static String nodeId = null;
    private static Image qiyLogo = null;
    private static Map<String, Object> nodeApiInfo = null;
    private static URI nodeEventUri = null;

    private final ConnectToken connectToken;

    /**
     * Private constructor for QiyNodeClient
     * 
     * @param connectToken
     *            a connectToken received from the QiyNode
     */
    private QiyNodeClient(ConnectToken connectToken) {
        super();
        this.connectToken = connectToken;
    }

    public static synchronized void setJaxRsClient(Client client) {
        jaxrsClient = client;
    }

    private static OpSdkSpiImplConfiguration getConfig() {
        if (config == null) {
            config = OpSdkSpiImplConfiguration.getInstance();
        }
        return config;
    }

    /**
     * Factory method to make sure no client can exists that is in an invalid state. Registering a callback will always
     * be the first thing to do with the client
     * 
     * @param inputConnectToken
     *            all the information needed to let the Node know how to call this OP once the Node has established a
     *            persistent id for the user
     * @return an initialised QiyNodeClient
     */
    static QiyNodeClient registerCallback(Map<String, Object> inputConnectToken) {
        byte[] databytes;
        try {
            databytes = MAP_WRITER.writeValueAsBytes(inputConnectToken);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error while doing registerCallback", e);
            throw new IllegalArgumentException(e);
        }
        String target = OpSdkSpiImplConfiguration.getInstance().registerCallbackUri;
        Response response;
        try {
            // @formatter:off
            response = jaxrsClient
                .target(target)
                .request(MediaType.APPLICATION_JSON)
                .header("password", getConfig().nodeConfig.password)
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader(databytes))
                .post(Entity.json(inputConnectToken));
            // @formatter:on
        } catch (ProcessingException e) {
            LOGGER.error("Connection failed {}", target);
            throw e;
        }
        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
            ConnectToken connectToken = response.readEntity(ConnectToken.class);
            return new QiyNodeClient(connectToken);
        }

        LOGGER.warn("Tried to register a callback, which failed. response {}", response);
        throw new IllegalStateException(
                "Error " + response.getStatus() + " while requesting connect token from " + target);
    }

    public static String getAuthHeader(byte[] data) {
        try {
            String nonce = Long.toString(System.currentTimeMillis());
            byte[] nonceBytes = nonce.getBytes(StandardCharsets.UTF_8);
            String nid = getNodeId();
            byte[] id = getNodeIdBytes();

            CryptoConfig cryptoConfig = OpSdkSpiImplConfiguration.getInstance().cryptoConfig;
            String sigAlg = cryptoConfig.sigAlgorithm;
            String sigProv = cryptoConfig.sigProvider;

            if (sigAlg == null) {
                throw new NullPointerException("No configuration found for sigAlgorithm");
            }
            Signature sig;
            if (sigProv == null || sigProv.trim().isEmpty()) {
                sig = Signature.getInstance(sigAlg);
            } else {
                sig = Signature.getInstance(sigAlg, sigProv);
            }
            sig.initSign(getConfig().nodeConfig.privateKey);
            sig.update(id, 0, id.length);
            sig.update(nonceBytes, 0, nonceBytes.length);
            if (data != null) {
                sig.update(data, 0, data.length);
            }
            byte[] signature = sig.sign();

            String result = b64(signature);
            LOGGER.debug("signature: {}", result);
            return String.format("QTF %s %s:%s", nid, nonce, result);
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException | NoSuchProviderException e) {
            LOGGER.error("Incorrect configuration of the environment", e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * Gets the ID of the Qiy Node from the {@link ConfigurationService}. Returns it as bytes using UTF-8 as character
     * set.
     * 
     * @return see description
     */
    private static byte[] getNodeIdBytes() {
        if (nodeIdBytes == null) {
            byte[] value = getNodeId().getBytes(StandardCharsets.UTF_8);
            nodeIdBytes = value;
        }
        return nodeIdBytes;
    }

    /**
     * Gets the ID of the Qiy Node from the {@link ConfigurationService}.
     * 
     * @return see description
     */
    private static String getNodeId() {
        if (nodeId == null) {
            String value = getConfig().nodeConfig.id;
            nodeId = value;
        }
        return nodeId;
    }

    /**
     * Returns the bytes for a PNG encoded image representation of the connect token.
     * 
     * @return see description
     * @throws IllegalStateException
     *             if no connectToken has been set
     * @throws RuntimeException
     *             if there was a problem writing to the {@link ByteArrayOutputStream} that is used internally to
     *             generate the byte[] result
     */
    byte[] connectTokenAsQRCode() {
        if (connectToken == null) {
            throw new IllegalStateException(NO_CT_SET);
        }
        QRConfig qrConfig = OpSdkSpiImplConfiguration.getInstance().qrConfig;

        BitMatrix encode = stringToBitMatrix(connectToken.toJSON());
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(encode);

        Image logo = qiyLogo();
        int x = (qrConfig.width / 2) - (logo.getWidth(null) / 2);
        int y = (qrConfig.height / 2) - (logo.getHeight(null) / 2);

        BufferedImage combined = new BufferedImage(qrConfig.width, qrConfig.height, BufferedImage.TYPE_INT_ARGB);
        combined.getGraphics().drawImage(bufferedImage, 0, 0, null);
        combined.getGraphics().drawImage(logo, x, y, null);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(combined, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            LOGGER.error("Error while doing connectTokenAsQRCode", e);
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the Qiy Logo, scaled to the dimensions stated in the configuration. If either of the config values
     * (logoWidth or logoHeight) is null, the image is not scaled
     * 
     * @return see description
     */
    public static Image qiyLogo() {
        if (qiyLogo == null) {
            QRConfig qrConfig = OpSdkSpiImplConfiguration.getInstance().qrConfig;
            Preconditions.checkNotNull(qrConfig, "QRConfig has not been set");
            Preconditions.checkNotNull(qrConfig.width, "QRConfig has no width");
            Preconditions.checkNotNull(qrConfig.height, "QRConfig has no height");
            
            double surface = (double) qrConfig.width * qrConfig.height;
            double errSurface;

            switch (qrConfig.errorCorrection) {
            case "L": // up to 7% damage allowed
                errSurface = surface * 0.07;
                break;
            case "M": // up to 15% damage allowed
                errSurface = surface * 0.15;
                break;
            case "Q": // up to 25% damage allowed
                errSurface = surface * 0.25;
                break;
            case "H":
                errSurface = surface * 0.30;
                break;
            default:
                throw new IllegalStateException("Unknown error correction" + qrConfig.errorCorrection);
            }
                
            qiyLogoFromSVG(errSurface);
        }
        return qiyLogo;
    }

    private static void qiyLogoFromSVG(double errSurface) {
        double newDim = Math.sqrt(errSurface); // assuming svg is square, if it's not, look in the history of this file

        try (InputStream logoSvgStream = QiyNodeClient.class.getResourceAsStream("/qiy-logo-qrcode.svg")) {
            TranscoderInput logoInput = new TranscoderInput(logoSvgStream);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            TranscoderOutput resizedOutput = new TranscoderOutput(baos);
            
            PNGTranscoder pngTranscoder = new PNGTranscoder();
            pngTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, new Float(newDim));

            pngTranscoder.transcode(logoInput, resizedOutput);
            
            logoSvgStream.close();
            baos.flush();

            Image image = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
            qiyLogo = image;
        } catch (IOException | TranscoderException e) {
            LOGGER.warn("Error while reading qiyLogo: {}", e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns JSON representation of the connect token.
     * 
     * @return see description
     * @throws IllegalStateException
     *             if no connectToken has been set
     */
    public String connectTokenAsJson() {
        if (connectToken == null) {
            throw new IllegalStateException(NO_CT_SET);
        }
        return connectToken.toJSON();
    }

    /**
     * let's save some screen real-estate; shortcut for Base64.getEncoder().encodeToString(bytes)
     * 
     * @param bytes
     *            source
     * @return see description
     */
    private static String b64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * let's save some screen real-estate; shortcut for Base64.getEncoder().encodeToString(string{UTF-8})
     * 
     * @param source
     *            source
     * @return see description
     */
    private static String b64(String source) {
        return Base64.getEncoder().encodeToString(source.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns the connectToken represented as a link with the dappre scheme, which should open directly on devices
     * 
     * @return see description
     */
    public String connectTokenAsDappreLink() {
        if (connectToken == null) {
            throw new IllegalStateException(NO_CT_SET);
        }
        // @formatter:off
        return String.format(
                "dappre://connect/?target=%s&tmpSecret=%s&identifier=%s&a=%s",
                b64(connectToken.target.toString()), 
                b64(b64(connectToken.tmpSecret)), 
                b64(connectToken.identifier),
                b64(Boolean.toString(connectToken.a))
        ); // @formatter:on
    }

    /**
     * Returns the {@link ConnectToken}.
     * 
     * @return see description
     * @throws IllegalStateException
     *             if no connectToken has been set
     */
    public ConnectToken getConnectToken() {
        if (connectToken == null) {
            throw new IllegalStateException(NO_CT_SET);
        }
        return connectToken;
    }

    private static BitMatrix stringToBitMatrix(String input) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            QRConfig qrConfig = OpSdkSpiImplConfiguration.getInstance().qrConfig;
            Integer width = qrConfig.width;
            Integer height = qrConfig.height;
            String errCorr = qrConfig.errorCorrection;
            Integer margin = qrConfig.margin;

            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.valueOf(errCorr));
            hints.put(EncodeHintType.MARGIN, margin);
            return writer.encode(input, BarcodeFormat.QR_CODE, width, height, hints);
        } catch (WriterException e) {
            LOGGER.error("Error while doing stringToBitMatrix", e);
            // Sonar won't let me throw a RuntimeException, suppose this comes closest
            throw new ProviderException(e);
        }
    }

    /**
     * Wrapper around a HTTPClient's get method which adds the Authorization header and expects a JSON body
     * 
     * @param uri
     *            the URI to call
     * @param type
     *            return type
     * @param <T>
     *            return type, bound by the type parameter
     * @return whatever was gotten
     */
    public static <T> T get(URI uri, Class<T> type) {
        Response response = doGet(uri);
        if (response.getStatusInfo().getFamily() == Status.Family.SUCCESSFUL) {
            return response.readEntity(type);
        }
        return null;
    }

    /**
     * Wrapper around a HTTPClient's get method which adds the Authorization header and expects a JSON body
     * 
     * @param uri
     *            the URI to call
     * @return whatever was gotten
     */
    public static Response doGet(URI uri) {
        // @formatter:off
        return jaxrsClient
            .target(uri)
            .request(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, getAuthHeader(null))
            .get(); // @formatter:on
    }

    /**
     * Listens on the given URI for events and delegates them to
     * 
     * @param evtConsumer
     *            should check if the inbound event is null, which means the connection has been closed (no further
     *            events will appear)
     */
    public static void listen(Function<InboundEvent, Boolean> evtConsumer) {
        URI target = getNodeEventUri();
        // @formatter:off
        Builder builder = jaxrsClient
                .target(target)
                .request()
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader(null)); // @formatter:on

        try (EventInput eventInput = builder.get(EventInput.class)) {
            while (!eventInput.isClosed()) {
                InboundEvent inboundEvent = eventInput.read();
                if (inboundEvent == null || Boolean.FALSE.equals(evtConsumer.apply(inboundEvent))) {
                    // connection has been closed
                    break;
                }
            }
        }
    }

    private static URI getNodeEventUri() {
        if (nodeEventUri == null) {
            Map<String, Object> apiInfo = getNodeApiInfo();
            @SuppressWarnings("unchecked")
            Map<String, String> links = (Map<String, String>) apiInfo.get("links");
            nodeEventUri = URI.create(links.get("events"));
        }
        return nodeEventUri;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getNodeApiInfo() {
        if (nodeApiInfo == null) {
            String apiInfoUri = getConfig().nodeConfig.endpoint;
            URI uri = URI.create(apiInfoUri);
            nodeApiInfo = get(uri, Map.class);
        }
        return nodeApiInfo;
    }

    /**
     * @param baseDappreURL
     */
    public static void readCardMessage(URL baseDappreURL) {
        try {
            URI cardApiURI = baseDappreURL.toURI().resolve("api");
            Map<String, Object> api = responseForReadCardMessage(cardApiURI, "Could not get CardAPI information.");
            if (api == null) {
                // logging will have been done
                return;
            }
            @SuppressWarnings("unchecked")
            URI cardsURI = URI.create((String)((Map<String, Object>)api.get("links")).get("cards"));
            Map<String, Object> cardsWithAttributes = responseForReadCardMessage(cardsURI, "Could not get Cards.");
            if (cardsWithAttributes == null) {
                // logging will have been done
                return;
            }

            @SuppressWarnings("unchecked")
            Collection<Map<String, Object>> cards = (Collection<Map<String, Object>>) cardsWithAttributes.get("cards");
            if (cards.isEmpty()) {
                LOGGER.error("No cards defined.");
                if (cardShareData == null) {
                    LOGGER.error("cardShareData is still null!");
                }
                return;
            }
            // else get share action for the first card
            @SuppressWarnings("unchecked")
            URI shareActionUri = URI.create((String)((Map<String, Object>)cards.iterator().next().get("links")).get("shareAction"));
            Map<String, Object> shareAction = responseForReadCardMessage(shareActionUri,
                    "Could not get share action " + shareActionUri);
            if (shareAction != null && !shareAction.isEmpty()) {
                LOGGER.info("updating cardShareData");
                cardShareData = Collections.unmodifiableMap(shareAction);
            }
        } catch (URISyntaxException e) {
            LOGGER.error("Error while doing registerCallback", e);
            throw new IllegalStateException("Please check your configuration");
        }
    }

    private static Map<String, Object> responseForReadCardMessage(URI uri, String message) {
        Response apiResponse = doGet(uri);
        if (apiResponse.getStatusInfo().getFamily() != Status.Family.SUCCESSFUL) {
            LOGGER.error("{} Status {}", message, apiResponse.getStatus());
            if (cardShareData == null) {
                LOGGER.error("cardShareData is still null!");
            }
            return null;
        }

        return apiResponse.readEntity(HashMap.class);
    }

}
