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

package nl.qiy.openid.op.spi.impl.mem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.qiy.oic.op.domain.OAuthClient;
import nl.qiy.oic.op.service.spi.ClientStore;
import nl.qiy.openid.op.spi.impl.OAuthClientImpl;
import nl.qiy.openid.op.spi.impl.config.OAuthClientConfig;
import nl.qiy.openid.op.spi.impl.config.OpSdkSpiImplConfiguration;

/**
 * Demo implementation for {@link ClientStore}
 *
 * @author Friso Vrolijken
 * @since 9 mei 2016
 */
public class ClientStoreImpl implements ClientStore {
    /** 
     * Standard SLF4J Logger 
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientStoreImpl.class);

    private Map<String, OAuthClient> clientMap = new HashMap<>();
    private Map<String, Boolean> originMap = new HashMap<>();

    public ClientStoreImpl() {
        super();
        List<OAuthClientConfig> list = OpSdkSpiImplConfiguration.getInstance().clientConfig;
        for (OAuthClientConfig conf : list) {
            OAuthClientImpl clientImpl = new OAuthClientImpl(conf);
            clientMap.put(clientImpl.id, clientImpl);
            for (String o : clientImpl.origin) {
                originMap.put(o, Boolean.TRUE);
            }
        }

    }

    @Override
    public OAuthClient getById(String clientId) {
        return clientMap.get(clientId);
    }

    @Override
    public boolean isHealthy() {
        boolean result = clientMap != null && !clientMap.isEmpty();
        LOGGER.debug("{} isHealthy called: {}", this.getClass(), result);
        return result;
    }

    @Override
    public Boolean existstOrigin(String origin) {
        return originMap.get(origin);
    }

}
