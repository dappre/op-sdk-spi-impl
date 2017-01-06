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

import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.qiy.oic.op.service.spi.Configuration;

/**
 * Demo implementation for {@link Configuration}, uses the Dropwizard {@link io.dropwizard.Configuration} to fetch the
 * actual values
 *
 * @author Friso Vrolijken
 * @since 9 mei 2016
 */
public class ConfigurationImpl implements Configuration {
    /**
     * Standard SLF4J Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationImpl.class);

    /**
     * Dropwizard configuration, will do the heavy lifting
     */
    private OpSdkSpiImplConfiguration delegate;

    private ValidatorFactory validationFactory = Validation.buildDefaultValidatorFactory();

    @Override
    public boolean isHealthy() {
        boolean result = getDelegate() != null && validationFactory.getValidator().validate(getDelegate()).isEmpty();
        LOGGER.debug("{} init called: {}", this.getClass(), result);
        return result;
    }

    /**
     * lazily gets the delegate
     * 
     * @return see description
     */
    private OpSdkSpiImplConfiguration getDelegate() {
        if (delegate == null) {
            delegate = OpSdkSpiImplConfiguration.getInstance();
        }
        if (delegate == null) {
            throw new NullPointerException("Configuration was not loaded");
        }
        return delegate;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String key) { // CC too high, changing it won't make the code better: NOSONAR
        T result;

        switch (key) {
        case BASE_URI:
            result = (T) getDelegate().baseUri;
            break;
        case REGISTER_CALLBACK_URI:
            result = (T) getDelegate().registerCallbackUri;
            break;
        case NODE_ID:
            result = (T) getDelegate().nodeConfig.id;
            break;
        case NODE_ENDPOINT:
            result = (T) getDelegate().nodeConfig.endpoint;
            break;
        case SIGNATURE:
            result = (T) getDelegate().cryptoConfig.asMap();
            break;
        case ISS:
            result = (T) getDelegate().iss;
            break;
        case "dappreBaseURI":
            result = (T) getDelegate().dappreBaseURI;
            break;
        default:
            throw new IllegalStateException("No configuration known for " + key);
        }
        if (result == null) {
            throw new IllegalStateException("No configuration known for " + key + " (2)");
        }
        // else
        return result;
    }
}
