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

package nl.qiy.openid.op.spi.impl.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

import io.dropwizard.Configuration;
import nl.qiy.oic.op.qiy.QRConfig;
import nl.qiy.oic.op.qiy.QiyNodeConfig;

/**
 * Main configuration class. Loaded by Dropwizard, also implements the Qiy openid-connect-idp configuration interface
 *
 * @author Friso Vrolijken
 * @since 9 mei 2016
 */
public class OpSdkSpiImplConfiguration extends Configuration {
    // private static final Logger LOGGER = LoggerFactory.getLogger(OpSdkSpiImplConfiguration.class);

    @NotNull
    public final QRConfig qrConfig;
    @NotNull
    public final List<OAuthClientConfig> clientConfig;
    @NotNull
    public final QiyNodeConfig nodeConfig;
    @NotNull
    public final CryptoConfig cryptoConfig;
    @NotEmpty
    public final String baseUri;
    @NotEmpty
    public final String registerCallbackUri;
    @NotEmpty
    public final String iss;

    public final URL htmlQiyConnectTokenTemplate;


    /**
     * purpose (idToken, userInfo or requestObject) mapped to [key alias mapped to config]
     */
    @NotEmpty
    public final Map<String, Map<String, JWKConfig>> jwkConfigs;

    @NotNull
    public final URL dappreBaseURI;

    public final Integer sessionTimeoutInSeconds;

    private static OpSdkSpiImplConfiguration instance;

    // @formatter:off
    @JsonCreator // NOSONAR
    public OpSdkSpiImplConfiguration(@JsonProperty("qrConfig") QRConfig qrConfig, // NOSONAR
            @JsonProperty("sessionTimeoutInSeconds") Integer sessionTimeoutInSeconds,
            @JsonProperty("clientConfig") List<OAuthClientConfig> clientConfig,
            @JsonProperty("nodeConfig") QiyNodeConfig nodeConfig,
            @JsonProperty("cryptoConfig") CryptoConfig cryptoConfig, 
            @JsonProperty("baseUri") String baseUri,
            @JsonProperty("dappreBaseURI") String dappreBaseUri,
            @JsonProperty("registerCallbackUri") String registerCallbackUri,
            @JsonProperty("iss") String iss,
            @JsonProperty("jwkConfigs") Map<String, Map<String, JWKConfig>> jwkConfigs,
            @JsonProperty("htmlQiyConnectTokenTemplate") String htmlQiyConnectTokenTemplate,
            @JsonProperty("cardMsgUri") String cardMsgUri,
            @JsonProperty("cardLoginOption") String cardLoginOption,
            @JsonProperty("welcomeMessage") String welcomeMessage,
            @JsonProperty("jedisConfiguration") Object jedisConfiguration) throws MalformedURLException {
        // @formatter:on
        super();
        this.sessionTimeoutInSeconds = sessionTimeoutInSeconds;
        this.clientConfig = clientConfig;
        this.cryptoConfig = cryptoConfig == null ? new CryptoConfig() : cryptoConfig;
        this.nodeConfig = nodeConfig;
        this.qrConfig = qrConfig == null ? new QRConfig() : qrConfig;
        this.baseUri = baseUri;
        this.dappreBaseURI = new URL(dappreBaseUri);
        this.registerCallbackUri = registerCallbackUri;
        this.iss = iss;
        this.jwkConfigs = jwkConfigs;
        this.htmlQiyConnectTokenTemplate = htmlQiyConnectTokenTemplate == null ? null
                : new URL(htmlQiyConnectTokenTemplate);

        Preconditions.checkArgument(this.registerCallbackUri.contains(nodeConfig.id),
                "Field 'registerCallbackUri': %s in the config file is expected to contain the node's id: %s",
                this.registerCallbackUri, nodeConfig.id);
    }

    public static void setInstance(OpSdkSpiImplConfiguration inst) {
        instance = inst;
    }

    public static OpSdkSpiImplConfiguration getInstance() {
        return instance;
    }
}

