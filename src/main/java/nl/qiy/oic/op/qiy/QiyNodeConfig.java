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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStore.SecretKeyEntry;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;

import io.dropwizard.jackson.Jackson;

/**
 * Configuration that is needed to talk to the Qiy Node
 *
 * @author Friso Vrolijken
 * @since 9 mei 2016
 */
public final class QiyNodeConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(QiyNodeConfig.class);

    @NotEmpty
    public final String id;
    @NotEmpty
    public final String endpoint;
    @NotNull
    public final PrivateKey privateKey;
    @NotNull
    public final PublicKey publicKey;
    @NotEmpty
    public final String password;

    @JsonCreator // NOSONAR
    // @formatter:off
    public QiyNodeConfig(@JsonProperty("id") String id, 
            @JsonProperty("endpoint") String endpoint,
            @JsonProperty("secretsFilename") String secretsFilename, 
            @JsonProperty("keystore") String keystore, 
            @JsonProperty("keystorePassPhrase") String keystorePassPhrase, 
            @JsonProperty("keyPassPhrase") String keyPassPhrase) throws IOException, GeneralSecurityException { // @formatter:on
        super();
        this.id = id;
        this.endpoint = endpoint;

        if (Strings.isNullOrEmpty(secretsFilename)) {
            LOGGER.info("No secretsFilename found, using keystore (old way)");
            KeyStore.SecretKeyEntry passwordEntry = (SecretKeyEntry) loadKey("nodesecret", keystore,
                    "jceks", keystorePassPhrase.toCharArray(), keyPassPhrase.toCharArray());
            KeyStore.PrivateKeyEntry privateKeyEntry = (PrivateKeyEntry) loadKey("nodekeypair",
                    keystore, "jceks", keystorePassPhrase.toCharArray(), keyPassPhrase.toCharArray());

            this.privateKey = privateKeyEntry.getPrivateKey();
            LOGGER.debug("privatekey found");
            this.publicKey = privateKeyEntry.getCertificate().getPublicKey();
            LOGGER.debug("public key found");
            this.password = Base64.getEncoder().encodeToString(passwordEntry.getSecretKey().getEncoded());
            LOGGER.debug("password found");
        } else {
            File secretsFile = new File(secretsFilename);
            LOGGER.info("Using secrets file " + secretsFile.getAbsolutePath());
            Map<String, String> secrets = Jackson.newObjectMapper().readValue(secretsFile, Map.class);
            Preconditions.checkState(this.id.equals(secrets.get("id")),
                    "The id in the config file must match the id in the secrets file");
            LOGGER.debug("ids match!");
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] keyBytes = Base64.getDecoder().decode(secrets.get("privateKey"));
            this.privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            LOGGER.debug("private key set");
            keyBytes = Base64.getDecoder().decode(secrets.get("publicKey"));
            this.publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
            LOGGER.debug("public key set");
            this.password = secrets.get("nodePassword");
            LOGGER.debug("password set {}", this.password != null);
        }
    }

    private static KeyStore.Entry loadKey(String alias, String keystoreFilename, String keystoreType,
            char[] keystorePass, char[] keyPass) {
        File f = new File(keystoreFilename);
        if (!f.exists() || !f.isFile() || !f.canRead()) {
            throw new IllegalStateException(f.getAbsolutePath() + " is not a regular readable file");
        }
        try (InputStream is = new FileInputStream(f)) {
            KeyStore keyStore = KeyStore.getInstance(keystoreType);
            keyStore.load(is, keystorePass);
            ProtectionParameter protector = null;
            if (keyPass != null) {
                protector = new KeyStore.PasswordProtection(keyPass);
            }
            return keyStore.getEntry(alias, protector);
        } catch (Exception e) {
            LOGGER.warn("Error while doing loadKeyStore", e);
            throw Throwables.propagate(e);
        }
    }

    /**
     * Constructor for QiyNodeConfig only to be used for UnitTesting, might be invalid!
     * 
     * @param id
     *            the id
     * @param password
     *            the password
     * @param privateKey
     *            the private key
     * @param publicKey
     *            the public key
     */
    private QiyNodeConfig(String id, String password, PrivateKey privateKey, PublicKey publicKey, String endpoint) { // NOSONAR
        super();
        this.id = id;
        this.password = password;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.endpoint = endpoint;
    }

    /**
     * For unit testing we need an instance that has a private key
     * 
     * @param privateKey
     *            the private key to be set
     * @return an invalid instance
     */
    public static QiyNodeConfig unitTestInstance(PrivateKey privateKey) {
        return new QiyNodeConfig(null, null, privateKey, null, null);
    }
}