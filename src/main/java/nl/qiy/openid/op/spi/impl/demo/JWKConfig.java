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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;

import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * TODO: friso should have written a comment here to tell us what this class does
 *
 * @author Friso Vrolijken
 * @since 30 mei 2016
 */
public class JWKConfig {
    /** 
     * Standard SLF4J Logger 
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JWKConfig.class);
    @NotEmpty
    public final String keystoreFilename;
    @NotEmpty
    public final String keystoreType;
    @NotEmpty
    public final char[] keystorePassPhrase;
    @NotEmpty
    public final char[] keyPassPhrase;

    @JsonCreator
    public JWKConfig(@JsonProperty("keystoreFilename") String keystoreFilename,
            @JsonProperty("keystoreType") String keystoreType,
            @JsonProperty("keystorePassphrase") String keystorePassphrase,
            @JsonProperty("keyPassphrase") String keyPassphrase) {
        super();
        Path p = Paths.get(keystoreFilename);
        if(Files.isReadable(p) && Files.isRegularFile(p)) {
            this.keystoreFilename = keystoreFilename;
        } else {
            throw new IllegalArgumentException(p.toAbsolutePath() + " is not a regular readable file");
        }
        if (keystoreType == null) {
            this.keystoreType = KeyStore.getDefaultType();
        } else {
            try {
                KeyStore.getInstance(keystoreType);
            } catch (KeyStoreException e) {
                LOGGER.warn("Error while doing JWKConfig", e);
                throw new IllegalArgumentException("Invalid key store type " + keystoreType, e);
            }
            this.keystoreType = keystoreType;
        }
        if (keystorePassphrase == null) {
            this.keystorePassPhrase = null;
        } else {
            this.keystorePassPhrase = keystorePassphrase.toCharArray();
        }

        if (keyPassphrase == null) {
            this.keyPassPhrase = null;
        } else {
            this.keyPassPhrase = keyPassphrase.toCharArray();
        }
    }
    
}
