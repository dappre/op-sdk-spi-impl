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

import com.google.common.base.Preconditions;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Persistence of received consent messages. This will keep track of whether they are sent, the answer is received,
 * consent was received or it should be ignored.
 *
 * @author Friso Vrolijken
 * @since 1 jul. 2016
 */
public class MessageDAO {
    private static final int BIT_FLAG_MESSAGE_SENT = 0;
    private static final int BIT_FLAG_MESSAGE_RECEIVED = 1;
    private static final int BIT_FLAG_CONSENT_RECEIVED = 2;
    private static final int BIT_FLAG_IGNOREABLE = 3;

    private static JedisPool pool;

    /**
     * Private constructor for MessageDAO, should not be used; only use the static methods
     */
    private MessageDAO() {
        throw new UnsupportedOperationException();
    }

    /**
     * Simple setter for the static pool, which must be set before any operation can be used
     * 
     * @param newPool
     *            the new value
     */
    public static synchronized void setPool(JedisPool newPool) {
        pool = newPool;
    }

    private static void assertPoolSet() {
        Preconditions.checkNotNull(pool, "The JedisPool must be set before attempting this operation"); // NOSONAR
    }

    /**
     * Helper method, checks if a given flag has been set to true
     * 
     * @param shareId
     *            unique identifier
     * @param bitFlag
     *            index in a bit sequence
     * @return true iff the n-th bit is set to true
     */
    private static boolean hasFlag(String shareId, int bitFlag) {
        assertPoolSet();
        try (Jedis jedis = pool.getResource()) {
            return jedis.getbit(shareId.getBytes(), bitFlag) == Boolean.TRUE;
        }
    }

    /**
     * Helper method, sets a given index in the bit sequence to true
     * 
     * @param shareId
     *            unique identifier
     * @param bitFlag
     *            index in the sequence
     */
    private static void setFlag(String shareId, int bitFlag) {
        assertPoolSet();
        try (Jedis jedis = pool.getResource()) {
            jedis.setbit(shareId.getBytes(), bitFlag, true);
        }
    }

    /**
     * Sets the property 'consented' to true for this shareId. The caller should verify that there was a message receive
     * that states the consent.
     * 
     * @param shareId
     *            unique identifier
     */
    public static void setConsented(String shareId) {
        setFlag(shareId, BIT_FLAG_CONSENT_RECEIVED);
    }

    /**
     * Sets the property 'sent' to true for this shareId. The caller should verify that a message was sent in which
     * consent is requested.
     * 
     * @param shareId
     *            unique identifier
     */
    public static void setSent(String shareId) {
        setFlag(shareId, BIT_FLAG_MESSAGE_SENT);
    }

    /**
     * Sets the property 'received' to true for this shareId. The caller should verify that an answer to the consent
     * request has been received.
     * 
     * @param shareId
     *            unique identifier
     */
    public static void setReceived(String shareId) {
        setFlag(shareId, BIT_FLAG_MESSAGE_RECEIVED);
    }

    /**
     * @param shareId
     *            unique identifier
     * @return if any message has been sent
     */
    public static boolean isSent(String shareId) {
        return hasFlag(shareId, BIT_FLAG_MESSAGE_SENT);
    }

    /**
     * @param shareId
     *            unique identifier
     * @return if consent was given
     */
    public static boolean hasConsent(String shareId) {
        return hasFlag(shareId, BIT_FLAG_CONSENT_RECEIVED);
    }

    /**
     * Sets the property 'ignorable' to true for this shareId. The caller should verify that this shareId is no longer
     * valid.
     * 
     * @param shareId
     *            unique identifier
     */
    public static void setIgnorable(String shareId) {
        setFlag(shareId, BIT_FLAG_IGNOREABLE);
    }

    /**
     * @param shareId
     *            unique identifier
     * @return if we should take notice of this shareId
     */
    public static boolean dontIgnore(String shareId) {
        return !hasFlag(shareId, BIT_FLAG_IGNOREABLE);
    }
}
