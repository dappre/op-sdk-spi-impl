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

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.SecretKeyEntry;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import io.dropwizard.jackson.Jackson;

/**
 * Configuration that is needed to talk to the Qiy Node
 *
 * @author Friso Vrolijken
 * @since 9 mei 2016
 */
public final class QiyNodeConfig {
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
            KeyStore.SecretKeyEntry passwordEntry = (SecretKeyEntry) SecretStoreImpl.loadKey("nodesecret", keystore,
                    "jceks", keystorePassPhrase.toCharArray(), keyPassPhrase.toCharArray());
            KeyStore.PrivateKeyEntry privateKeyEntry = (PrivateKeyEntry) SecretStoreImpl.loadKey("nodekeypair",
                    keystore, "jceks", keystorePassPhrase.toCharArray(), keyPassPhrase.toCharArray());

            this.privateKey = privateKeyEntry.getPrivateKey();
            this.publicKey = privateKeyEntry.getCertificate().getPublicKey();
            this.password = Base64.getEncoder().encodeToString(passwordEntry.getSecretKey().getEncoded());
        } else {
            File secretsFile = new File(secretsFilename);
            Map<String, String> secrets = Jackson.newObjectMapper().readValue(secretsFile, Map.class);
            Preconditions.checkState(this.id.equals(secrets.get("id")),
                    "The id in the config file must match the id in the secrets file");
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] keyBytes = Base64.getDecoder().decode(secrets.get("privateKey"));
            this.privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            keyBytes = Base64.getDecoder().decode(secrets.get("publicKey"));
            this.publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
            this.password = secrets.get("nodePassword");
        }
    }
}
