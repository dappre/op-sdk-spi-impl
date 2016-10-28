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

import java.io.Serializable;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * The connect token is received from the node and should be provided to the user. The user should use it to create a
 * connection.
 *
 * @author Friso Vrolijken
 * @since 3 mei 2016
 */
public class ConnectToken implements Serializable {
    /**
     * Default value
     */
    private static final long serialVersionUID = 1L;
    /** 
     * Standard SLF4J Logger 
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectToken.class);
    /**
     * For Jackson serialisation
     */
    private static final ObjectWriter WRITER = new ObjectMapper().writerFor(ConnectToken.class);

    /**
     * As defined by the Qiy specifications
     */
    public final URI target;
    /**
     * As defined by the Qiy specifications
     */
    public final byte[] tmpSecret;
    /**
     * As defined by the Qiy specifications
     */
    public final String identifier;

    // TODO, temp fix, replace with better version after consult with WSFT
    public final boolean a;

    /**
     * JSON constructor for ConnectToken
     * 
     * @param target
     *            target at the issuer where the connection should lead to
     * @param tmpSecret
     *            a temporary secret per Qiy Spec
     * @param value
     *            there might be some broken QR codes that use this for the tmpSecret
     * @param identifier
     *            a String identifying us in the QTF
     * @param a
     *            if true, this is a login request, else this is a Dappre request
     */
    @JsonCreator
    @SuppressWarnings("ucd")
    public ConnectToken(@JsonProperty("target") URI target, @JsonProperty("tmpSecret") byte[] tmpSecret,
            @JsonProperty("value") byte[] value, @JsonProperty("identifier") String identifier,
            @JsonProperty("a") Boolean a) {
        super();
        this.target = target;
        this.tmpSecret = tmpSecret == null ? value : tmpSecret;
        this.identifier = identifier;
        this.a = Boolean.TRUE.equals(a);
    }

    /**
     * Returns the JSON representation of this object. e.g.
     * 
     * <pre>
     * {"target":"https://sp.example.com/targets/092437-0a098l098-987asdfo098234",
     *  "tmpSecret":"Ui89Das23se==",
     *  "identifier":"dummy"}
     * </pre>
     * 
     * @return see description
     */
    public String toJSON() {
        try {
            return WRITER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Error while doing toJSON", e);
            // Sonar complains when throwing RuntimeException
            throw new IllegalStateException(e);
        }
    }
}
