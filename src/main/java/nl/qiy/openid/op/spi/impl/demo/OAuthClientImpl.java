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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.qiy.oic.op.domain.OAuthClient;

/**
 * The demo implementation for a {@link OAuthClient}
 *
 * @author Friso Vrolijken
 * @since 9 mei 2016
 */
public final class OAuthClientImpl implements OAuthClient {
    /**
     * generated
     */
    private static final long serialVersionUID = 1L;
    /** 
     * Standard SLF4J Logger 
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthClientImpl.class);

    final String id;
    final Collection<String> origin;
    private final Pattern uriPattern;

    /**
     * Default constructor for ClientImpl
     * 
     * @param conf
     *            the configuration that holds the details
     */
    OAuthClientImpl(OAuthClientConfig conf) {
        super();
        this.id = conf.id;
        this.origin = conf.origin;
        uriPattern = conf.getUriPattern();
    }

    @Override
    public boolean ownsURI(URI redirectUri) {
        if (redirectUri == null) {
            throw new NullPointerException("null input is not allowed for ownsURI in " + this.getClass().getName());
        }
        LOGGER.debug("checking {} for client {}", redirectUri, id);
        return uriPattern.matcher(redirectUri.toString()).matches();
    }

    /**
     * Sonar will warn about the Lists not being Serializable, yet I want to keep the semantics of the interface rather
     * than the concrete class. This keeps clients of this class honest. This method exists to get rid of the warnings.
     * <p>
     * <strong>NB: this means that the concrete instantiations of the collection fields in this class MUST be
     * Serializable</strong>
     * 
     * @param stream
     *            where to write it to
     * @throws IOException
     *             when stream.defaultReadObject does
     */
    @SuppressWarnings("static-method")
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
    }

    /**
     * Sonar will warn about the Lists not being Serializable, yet I want to keep the semantics of the interface rather
     * than the concrete class. This keeps clients of this class honest. This method exists to get rid of the warnings.
     * <p>
     * <strong>NB: this means that the concrete instantiations of the collection fields in this class MUST be
     * Serializable</strong>
     * 
     * @param stream
     *            where to read from
     * @throws IOException
     *             when stream.defaultReadObject does
     * @throws ClassNotFoundException
     *             when stream.defaultReadObject does
     */
    @SuppressWarnings("static-method")
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
    }
}
