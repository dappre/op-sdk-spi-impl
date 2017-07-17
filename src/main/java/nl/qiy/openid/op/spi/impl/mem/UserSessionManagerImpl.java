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

import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import nl.qiy.oic.op.domain.IDToken;
import nl.qiy.oic.op.domain.OAuthUser;
import nl.qiy.oic.op.qiy.QiyOAuthUser;
import nl.qiy.oic.op.service.spi.UserSessionManager;
import nl.qiy.openid.op.spi.impl.config.OpSdkSpiImplConfiguration;

/**
 * TODO: friso should have written a comment here to tell us what this class does
 *
 * @author friso
 * @since 17 jul. 2017
 */
public class UserSessionManagerImpl implements UserSessionManager {
    /** 
     * Standard SLF4J Logger 
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UserSessionManagerImpl.class);
    private static final String LOGGED_IN_USER = "nl.qiy.idp.LOGGED_IN_USER";
    private static final Long BEARER_EXPIRY_SECONDS = Long.valueOf(900L);
    private static final Cache<String, IDToken> BEARER_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(BEARER_EXPIRY_SECONDS, TimeUnit.SECONDS).build();

    private Boolean skipSessionStorage;

    @Override
    public boolean isHealthy() {
        LOGGER.debug("{} isHealthy called: {}", this.getClass(), true);
        return true;
    }

    private boolean isSkipSessionStorage() {
        if (skipSessionStorage == null) {
            Integer sessionTimeoutInSeconds = OpSdkSpiImplConfiguration.getInstance().sessionTimeoutInSeconds;
            skipSessionStorage = sessionTimeoutInSeconds != null && sessionTimeoutInSeconds.intValue() == 1;
        }
        return skipSessionStorage.booleanValue();
    }



    @Override
    public OAuthUser getLoggedIn(HttpSession session) {
        if (isSkipSessionStorage()) {
            // short circuit
            LOGGER.debug("Skipping session lookup since we're configured that way");
            return null;
        }

        QiyOAuthUser result = (QiyOAuthUser) session.getAttribute(LOGGED_IN_USER);
        // DILEMMA: Dappre doesn't delete the underlying connection when the user deletes the organisation's card. So
        // we'd have to fetch the card to know if the connection has been deleted. But we haven't got consent to fetch
        // the user's card (which is personal data). So for the time being assume the user will remain logged in for the
        // time of the session.
        return result;
    }

    @Override
    public void logout(HttpSession session) {
        session.removeAttribute(LOGGED_IN_USER);
        session.invalidate();
    }

    @Override
    public OAuthUser login(OAuthUser template, HttpSession session) {
        if (!(template instanceof QiyOAuthUser)) { // allow for this to be one in a series of UserSessionManagers
            return null;
        }
        // else

        // This method was called from QiyAuthorizationFlow#callbackFromQiyNode, so we know that the input's subject and
        // connectionURI have been set
        QiyOAuthUser input = (QiyOAuthUser) template;
        Preconditions.checkNotNull(input.getSubject(), "The subject may not be null");
        Preconditions.checkNotNull(input.getConnectionUri(), "The connectionUri may not be null");

        QiyOAuthUser userImpl;

        if (isSkipSessionStorage()) {
            LOGGER.debug("Skipping session storage since we're configured that way");
            userImpl = new QiyOAuthUser(input);
        } else {
            userImpl = (QiyOAuthUser) session.getAttribute(LOGGED_IN_USER);
            boolean newLogin = userImpl == null || !input.getSubject().equals(userImpl.getSubject());
            if (newLogin) {
                userImpl = new QiyOAuthUser(input);
                session.setAttribute(LOGGED_IN_USER, new QiyOAuthUser(input));
                userImpl.resetLoginTime();
            }
        }
        LOGGER.debug("User {} is logged in", userImpl);
        return userImpl;
    }

    @Override
    public IDToken getBearer(String bearerKey) {
        return BEARER_CACHE.getIfPresent(bearerKey);
    }

    @Override
    public Long addBearer(String at, IDToken idt) {
        BEARER_CACHE.put(at, idt);
        return BEARER_EXPIRY_SECONDS;
    }
}
