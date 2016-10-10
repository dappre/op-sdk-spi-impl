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

import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.SecretKeyEntry;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    @NotEmpty
    public final String keystore;
    @NotEmpty
    public final char[] keystorePassPhrase;
    @NotEmpty
    public final char[] keyPassPhrase;

    private final KeyStore.SecretKeyEntry passwordEntry;
    private final KeyStore.PrivateKeyEntry privateKeyEntry;

    @JsonCreator
    // @formatter:off
    public QiyNodeConfig(@JsonProperty("id") String id, 
            @JsonProperty("endpoint") String endpoint,
            @JsonProperty("keystore") String keystore, 
            @JsonProperty("keystorePassPhrase") String keystorePassPhrase, 
            @JsonProperty("keyPassPhrase") String keyPassPhrase) {// @formatter:on
        super();
        this.id = id;
        this.endpoint = endpoint;
        this.keystore = keystore;
        this.keystorePassPhrase = keystorePassPhrase.toCharArray(); // might throw a NPE, which is all right
        this.keyPassPhrase = keyPassPhrase.toCharArray(); // might throw a NPE, which is all right
        this.passwordEntry = (SecretKeyEntry) SecretStoreImpl.loadKey("nodesecret", keystore, "jceks",
                this.keystorePassPhrase, this.keyPassPhrase);
        this.privateKeyEntry = (PrivateKeyEntry) SecretStoreImpl.loadKey("nodekeypair", keystore, "jceks",
                this.keystorePassPhrase, this.keyPassPhrase);
    }

    public PrivateKey getPrivateKey() {
        return privateKeyEntry.getPrivateKey();
    }

    public String getPassword() {
        return Base64.getEncoder().encodeToString(passwordEntry.getSecretKey().getEncoded());
    }

    public PublicKey getPublicKey() {
        return privateKeyEntry.getCertificate().getPublicKey();
    }
}
