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

import java.net.URI;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.qiy.oic.op.domain.AddressClaim;
import nl.qiy.oic.op.domain.OAuthUser;
import nl.qiy.oic.op.domain.StandardClaims;

/**
 * Implementation for a OAuthUser within the context of the Qiy Trust Framework
 *
 * @author Friso Vrolijken
 * @since 24 jun. 2016
 */
public class QiyOAuthUser implements OAuthUser {
    /**
     * Standard SLF4J Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(QiyOAuthUser.class);
    /**
     * Generated
     */
    private static final long serialVersionUID = 1L;

    private final CallbackInput cbInput;
    private final Set<String> shareIds = new HashSet<>();

    private Instant loginTime = Instant.MIN;
    private StandardClaims claims;

    /**
     * Creates a template user
     * 
     * @param cbInput
     *            callback information from QTF
     */
    QiyOAuthUser(CallbackInput cbInput) {
        super();
        this.cbInput = cbInput;
    }

    /**
     * Creates a user from a template, this is a user to be stored
     * 
     * @param input
     *            copy this user's callback input
     */
    public QiyOAuthUser(QiyOAuthUser input) {
        super();
        this.cbInput = input.cbInput;
    }

    @Override
    public Instant getLoginTime() {
        return loginTime;
    }

    /**
     * sets the time of the last login to now
     */
    public void resetLoginTime() {
        LOGGER.debug("Resetting login time for user {}", cbInput.pid);
        loginTime = Instant.now();
    }

    @Override
    public String getSubject() {
        return cbInput.pid;
    }

    /**
     * Sets the standard claims, which means this user is no longer a new user
     * 
     * @param standardClaims
     *            new value
     */
    public void setClaims(StandardClaims standardClaims) {
        claims = standardClaims;
    }

    /**
     * Simple getter
     * 
     * @return the claims
     */
    @Override
    public StandardClaims getClaims() {
        return claims;
    }

    /**
     * @param map
     *            name-value pairs where the name should match a property of the {@link StandardClaims}, the "address"
     *            field is expected to be a Map that will translate to {@link AddressClaim}
     */
    public void setClaims(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            unsetClaims();
            return;
        }
        Long updatedAt = map.containsKey("lastUpdatedDate") ? Long.valueOf((String) map.get("lastUpdatedDate")) : null;
        @SuppressWarnings("unchecked")
        AddressClaim address = cardAddressToAddressClaim((Map<String, String>) map.get("address"));

        // resulted in response header too large, so using null
        String pictureURL = null;
        // @formatter:off
        this.claims = new StandardClaims(
            cbInput.pid,                      // sub 
            null,                             // name
            (String) map.get("firstName"),    // givenName
            (String) map.get("lastName"),     // familyName
            null,                             // middleName
            null,                             // nickname
            null,                             // preferredUsername 
            null,                             // profile
            pictureURL,                       // picture
            (String) map.get("websiteURL"),   // website
            (String) map.get("emailAddress"), // email
            Boolean.FALSE,                    // emailVerified
            null,                             // gender
            null,                             // birthdate
            null,                             // zoneinfo
            null,                             // locale
            (String) map.get("mobileNumber"), // phoneNumber
            Boolean.FALSE,                    // phoneNumberVerified
            address,                          // address
            updatedAt); // @formatter:on

    }

    private static AddressClaim cardAddressToAddressClaim(Map<String, String> input) {
        if (input == null) {
            return null;
        }
        // else
        // @formatter:off
        return new AddressClaim(null,       // formatted
                input.get("street"),        // streetAddress
                input.get("locality"),      // locality
                input.get("region"),        // region
                input.get("postalCode"),    // postalCode
                input.get("country"));      // country
        // @formatter:on
    }

    /**
     * @return the URI for the connection between the node that is owned by the RP and the logged in user
     */
    public URI getConnectionUri() {
        return cbInput.connection;
    }

    public void unsetClaims() {
        this.claims = null;
    }

    public void setShareIds(Set<String> newValue) {
        this.shareIds.clear();
        if (newValue != null) {
            this.shareIds.addAll(newValue);
        }
    }

    public Set<String> getShareIds() {
        return this.shareIds;
    }
}
