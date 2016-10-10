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
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStore.ProtectionParameter;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;

import nl.qiy.oic.op.service.spi.SecretStore;

/**
 * The demo implementation for a {@link SecretStore}, uses the Dropwizard configuration to store secrets, which may not
 * be the best of places
 *
 * @author Friso Vrolijken
 * @since 9 mei 2016
 */
public class SecretStoreImpl implements SecretStore {
    /**
     * Standard SLF4J Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SecretStoreImpl.class);
    private final OpSdkSpiImplConfiguration config = OpSdkSpiImplConfiguration.getInstance();

    @Override
    public boolean isHealthy() {
        boolean result = getNodePassword() != null && getNodePrivateKey() != null;
        LOGGER.debug("{} isHealthy called: {}", this.getClass(), result);
        return result;
    }

    @Override
    public String getNodePassword() {
        return config.nodeConfig.getPassword();
    }

    @Override
    public PrivateKey getNodePrivateKey() {
        return config.nodeConfig.getPrivateKey();
    }

    @Override
    public JWKSet getJWKSet(String type) {
        Map<String, JWKConfig> jwkConfig = config.jwkConfigs.get(type);
        List<JWK> result = new ArrayList<>();
        for (Entry<String, JWKConfig> kv : jwkConfig.entrySet()) {
            JWKReader reader = new JWKReader(kv.getKey(), kv.getValue());
            result.add(reader.getJWK());
        }
        return new JWKSet(result);
    }

    static KeyStore.Entry loadKey(String alias, String keystoreFilename, String keystoreType, char[] keystorePass,
            char[] keyPass) {
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

}



