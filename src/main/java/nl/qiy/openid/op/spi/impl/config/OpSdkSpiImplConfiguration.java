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

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;
import nl.qiy.oic.op.qiy.QRConfig;
import nl.qiy.oic.op.qiy.QiyNodeConfig;
import nl.qiy.openid.op.spi.impl.jedis.JedisConfiguration;

/**
 * Main configuration class. Loaded by Dropwizard, also implements the Qiy openid-connect-idp configuration interface
 *
 * @author Friso Vrolijken
 * @since 9 mei 2016
 */
public class OpSdkSpiImplConfiguration extends Configuration {
    /**
     * What should our status be with regards to cards? Three options:
     * <ul>
     * <li>We require a shared card before we say we're logged in
     * <li>We're logged in if a card is shared, and if we've got it we'll hand out the details
     * <li>We do require a card to be shared (this is because we can't properly delete connections within Dappre), but
     * we do not want to give out the details on the card. This means that we do not need to ask the user's consent
     * </ul>
     *
     * @author friso
     * @since 1 dec. 2016
     */
    public enum CardLoginOption {
        NO_CARD, WANT_CARD, NEED_CARD;

    }

    private static final String DEFAULT_WELCOME_MESSAGE = "May we use your card data? Answer 'yes' if you agree";

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

    public final String cardMsgUri;

    /**
     * purpose (idToken, userInfo or requestObject) mapped to [key alias mapped to config]
     */
    @NotEmpty
    public final Map<String, Map<String, JWKConfig>> jwkConfigs;
    @NotNull
    public URL dappreBaseURI;
    public final CardLoginOption cardLoginOption;

    public final String welcomeMessage;

    @NotNull
    public JedisConfiguration jedisConfiguration;


    private static OpSdkSpiImplConfiguration instance;

    // @formatter:off
    @JsonCreator // NOSONAR
    public OpSdkSpiImplConfiguration(@JsonProperty("qrConfig") QRConfig qrConfig, // NOSONAR
            @JsonProperty("clientConfig") List<OAuthClientConfig> clientConfig,
            @JsonProperty("nodeConfig") QiyNodeConfig nodeConfig,
            @JsonProperty("cryptoConfig") CryptoConfig cryptoConfig, 
            @JsonProperty("baseUri") String baseUri,
            @JsonProperty("registerCallbackUri") String registerCallbackUri,
            @JsonProperty("iss") String iss,
            @JsonProperty("jwkConfigs") Map<String, Map<String, JWKConfig>> jwkConfigs,
            @JsonProperty("cardMsgUri") String cardMsgUri,
            @JsonProperty("cardLoginOption") String cardLoginOption,
            @JsonProperty("welcomeMessage") String welcomeMessage,
            @JsonProperty("jedisConfiguration") JedisConfiguration jedisConfiguration) {
        // @formatter:on
        super();
        this.clientConfig = clientConfig;
        this.cryptoConfig = cryptoConfig == null ? new CryptoConfig() : cryptoConfig;
        this.nodeConfig = nodeConfig;
        this.qrConfig = qrConfig == null ? new QRConfig() : qrConfig;
        this.baseUri = baseUri;
        this.registerCallbackUri = registerCallbackUri;
        this.iss = iss;
        this.jwkConfigs = jwkConfigs;
        this.cardMsgUri = cardMsgUri;
        this.cardLoginOption = cardLoginOption == null ? CardLoginOption.NO_CARD
                : CardLoginOption.valueOf(cardLoginOption);
        this.jedisConfiguration = jedisConfiguration == null
                ? new JedisConfiguration(null, null, null, null, null, null, null, null)
                : jedisConfiguration;
        if (this.cardLoginOption == CardLoginOption.NO_CARD && welcomeMessage == null) {
            this.welcomeMessage = null;
        } else {
            this.welcomeMessage = welcomeMessage == null ? DEFAULT_WELCOME_MESSAGE : welcomeMessage;
        }
    }

    public static void setInstance(OpSdkSpiImplConfiguration inst) {
        instance = inst;
    }

    public static OpSdkSpiImplConfiguration getInstance() {
        return instance;
    }
}

