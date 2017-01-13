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

package nl.qiy.openid.op.spi.impl.keystore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;

import nl.qiy.oic.op.service.spi.SecretStore;
import nl.qiy.openid.op.spi.impl.config.JWKConfig;
import nl.qiy.openid.op.spi.impl.config.OpSdkSpiImplConfiguration;

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

    @Override
    public boolean isHealthy() {
        boolean result = true;
        LOGGER.debug("{} isHealthy called: {}", this.getClass(), result);
        return result;
    }

    @Override
    public JWKSet getJWKSet(String type) {
        OpSdkSpiImplConfiguration config = OpSdkSpiImplConfiguration.getInstance();
        Map<String, JWKConfig> jwkConfig = config.jwkConfigs.get(type);
        List<JWK> result = new ArrayList<>();
        for (Entry<String, JWKConfig> kv : jwkConfig.entrySet()) {
            JWKReader reader = new JWKReader(kv.getKey(), kv.getValue());
            result.add(reader.getJWK());
        }
        return new JWKSet(result);
    }
}



