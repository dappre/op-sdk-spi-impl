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

package nl.qiy.openid.op.spi.impl.jedis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import redis.clients.jedis.Protocol;

/**
 * TODO: friso should have written a comment here to tell us what this class does
 *
 * @author Friso Vrolijken
 * @since 16 sep. 2016
 */
public class JedisConfiguration {
    public final GenericObjectPoolConfig poolConfiguration;
    public final String host;
    public final Integer port;
    public final Integer timeout;
    public final String password;
    public final Integer database;
    public final String clientName;
    public final Boolean ssl;

    /**
     * Constructor for JedisConfiguration
     * 
     * @param poolConfiguration
     *            There is an object pool where connections are kept, this is it's config
     * @param host
     *            the host where Redis can be found
     * @param port
     *            the port on which Redis is available
     * @param timeout
     *            both the connection and ro timeout
     * @param password
     *            password to connect with Redis
     * @param database
     *            database, defaults to 0
     * @param clientName
     *            the client's name
     * @param ssl
     *            wheter or not to use ssl to connect to redis
     */
    @JsonCreator
    public JedisConfiguration(@JsonProperty("poolConfiguration") GenericObjectPoolConfig poolConfiguration, 
            @JsonProperty("host") String host, 
            @JsonProperty("port") Integer port, 
            @JsonProperty("timeout") Integer timeout,
            @JsonProperty("password") String password, @JsonProperty("database") Integer database,
            @JsonProperty("clientName") String clientName, @JsonProperty("ssl") Boolean ssl) {
        this.poolConfiguration = poolConfiguration == null ? new GenericObjectPoolConfig() : poolConfiguration;
        this.host = host == null ? Protocol.DEFAULT_HOST : host;
        this.port = port == null ? Protocol.DEFAULT_PORT : port;
        this.timeout = timeout == null ? Protocol.DEFAULT_TIMEOUT : timeout;
        this.password = password; // may be null, which is the default
        this.database = database == null ? Protocol.DEFAULT_DATABASE : database;
        this.clientName = clientName; // may be null, which is the default
        this.ssl = Boolean.TRUE.equals(ssl);
    }
}
