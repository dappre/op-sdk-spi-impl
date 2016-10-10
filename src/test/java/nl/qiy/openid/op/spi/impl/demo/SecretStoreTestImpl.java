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

import java.security.PrivateKey;

import com.nimbusds.jose.jwk.JWKSet;

import nl.qiy.oic.op.service.spi.SecretStore;

public class SecretStoreTestImpl implements SecretStore {

    public static PrivateKey pk;

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public String getNodePassword() {
        return null;
    }

    @Override
    public PrivateKey getNodePrivateKey() {
        return SecretStoreTestImpl.pk;
    }

    @Override
    public JWKSet getJWKSet(String type) {
        return null;
    }

}
