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

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;

import nl.qiy.oic.op.domain.OAuthUser;
import nl.qiy.oic.op.qiy.QiyOAuthUser;
import nl.qiy.oic.op.service.spi.UserSessionManager;

/**
 * The demo implementation for {@link UserSessionManager}, uses an internal {@link Cache} and the HTTP Session to store
 * logged in uses. Note that this won't scale, which is not a problem for the demo environment
 *
 * @author Friso Vrolijken
 * @since 9 mei 2016
 */
public class UserSessionManagerImpl implements UserSessionManager {
    /** 
     * Standard SLF4J Logger 
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UserSessionManagerImpl.class);
    private static final String LOGGED_IN_USER = "nl.qiy.idp.LOGGED_IN_USER";

    @Override
    public boolean isHealthy() {
        LOGGER.debug("{} isHealthy called: {}", this.getClass(), true);
        return true;
    }

    @Override
    public OAuthUser getLoggedIn(HttpSession session) {
        QiyOAuthUser userImpl = (QiyOAuthUser) session.getAttribute(LOGGED_IN_USER);
        if (userImpl == null) {
            return null;
        }
        // else
        UserValidator uv = new UserValidator(userImpl);
        // check if the access hasn't been revoked
        return uv.getValidatedUser();
    }

    @Override
    public void logout(HttpSession session) {
        session.removeAttribute(LOGGED_IN_USER);
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

        QiyOAuthUser userImpl = (QiyOAuthUser) session.getAttribute(LOGGED_IN_USER);

        if (userImpl == null || !input.getSubject().equals(userImpl.getSubject())) {
            userImpl = new QiyOAuthUser(input);
            session.setAttribute(LOGGED_IN_USER, userImpl);
        }

        UserValidator uv = new UserValidator(userImpl);
        userImpl = uv.getValidatedUser();
        if (userImpl == null) {
            Preconditions.checkState(OpSdkSpiImplConfiguration.getInstance().requireCard);
            return null;
        }

        // TODO [FV 20160730] move this?
        userImpl.resetLoginTime();
        LOGGER.debug("User {} is logged in", userImpl.getSubject());
        return userImpl;
    }
}
