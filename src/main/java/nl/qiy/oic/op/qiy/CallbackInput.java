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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Simple data class
 *
 * @author Friso Vrolijken
 * @since 29 apr. 2016
 */
public class CallbackInput implements Serializable {
    /**
     * generated
     */
    private static final long serialVersionUID = 1L;
    public final String pid;
    public final URI connection;
    public final byte[] body;

    /**
     * JSON constructor for CallbackInput
     * 
     * @param pid
     *            the persistent identifier (which will become the sub in the idToken)
     * @param connection
     *            the URI for the connection that can be used to communicate
     * @param body
     *            the serialized form of the AuthenticationRequest
     */
    @JsonCreator
    public CallbackInput(@JsonProperty("pid") String pid, @JsonProperty("connection") String connection,
            @JsonProperty("body") byte[] body) {
        super();
        this.pid = pid;
        this.body = body;
        this.connection = URI.create(connection);
    }
}
