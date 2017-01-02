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

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Representation of a ConnectToken
 *
 * @author friso
 * @since 15 dec. 2016
 */
public class QiyConnectTokenRepresentation {
    private final byte[] qrCode;
    private final String qrJson;
    private final URI notificationUri;
    private final URI dappreAppUri;

    /**
     * Constructor for QiyConnectTokenRepresentation, used by Jackson
     * 
     * @param qrCode
     *            png image
     * @param qrJson
     *            the content of the QR image as json
     * @param notificationUri
     *            the URI where the server sent events will be sent to
     * @param dappreAppUri
     *            the URI that can be used on a device to connect (if the dappre scheme has been registered)
     */
    // @formatter:off
    @JsonCreator
    public QiyConnectTokenRepresentation(
            @JsonProperty("qrCode") byte[] qrCode, 
            @JsonProperty("qrJson") String qrJson,
            @JsonProperty("notificationUri") String notificationUri,
            @JsonProperty("dappreConnectUri") String dappreAppUri) { // @formatter:on
        super();
        this.qrCode = qrCode;
        this.qrJson = qrJson;
        this.notificationUri = URI.create(notificationUri);
        this.dappreAppUri = URI.create(dappreAppUri);
    }

    /**
     * Constructor for QiyConnectTokenRepresentation
     * 
     * @param client
     *            a client that has been instantiated, so that it can give us the connect token in several formats
     * @param notificationUri
     *            the uri where notification of change of the status of the connect token will be given
     */
    public QiyConnectTokenRepresentation(QiyNodeClient client, URI notificationUri) {
        super();
        this.qrCode = client.connectTokenAsQRCode();
        this.qrJson = client.connectTokenAsJson();
        this.dappreAppUri = URI.create(client.connectTokenAsDappreLink());
        this.notificationUri = notificationUri;
    }

    /**
     * Simple getter
     * 
     * @return the qrCode
     */
    public byte[] getQrCode() {
        return qrCode;
    }

    /**
     * Simple getter
     * 
     * @return the qrJson
     */
    public String getQrJson() {
        return qrJson;
    }

    /**
     * Simple getter
     * 
     * @return the notificationUri
     */
    public URI getNotificationUri() {
        return notificationUri;
    }

    /**
     * Simple getter
     * 
     * @return the dappreConnectUri
     */
    public URI getDappreAppUri() {
        return dappreAppUri;
    }

}
