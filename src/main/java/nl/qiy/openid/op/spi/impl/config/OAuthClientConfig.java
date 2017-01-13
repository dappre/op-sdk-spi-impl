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

package nl.qiy.openid.op.spi.impl.config;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;

/**
 * Configuration to be used to create OAuthClientImpl instances
 *
 * @author Friso Vrolijken
 * @since 9 mei 2016
 */
public final class OAuthClientConfig {

    @NotEmpty
    public final String id;

    @NotNull
    @Size(min = 0)
    public final Collection<String> origin;

    @NotEmpty
    public final String uriRegex;

    private final Pattern uriPattern;

    @JsonCreator
    public OAuthClientConfig(@JsonProperty("id") String id, @JsonProperty("uriRegex") String uriRegex,
            @JsonProperty("origin") Collection<String> origin) {
        super();
        this.id = id;
        this.uriRegex = Strings.isNullOrEmpty(uriRegex) ? "^https?://.*" : uriRegex;
        this.uriPattern = Pattern.compile(this.uriRegex);
        this.origin = origin == null ? Collections.emptyList() : origin;
    }

    /**
     * Gets the pattern that was created by compiling {@link #uriRegex}
     * 
     * @return see description
     */
    public Pattern getUriPattern() {
        return uriPattern;
    }
}
