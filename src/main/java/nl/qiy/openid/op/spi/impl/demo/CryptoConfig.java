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

import java.util.HashMap;
import java.util.Map;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;

/**
 * Configuration of the cryptology used.
 *
 * @author Friso Vrolijken
 * @since 9 mei 2016
 */
public final class CryptoConfig {
    @NotEmpty
    public final String asyncAlgorithm;
    public final String asyncProvider;
    @NotEmpty
    public final String sigAlgorithm;
    public final String sigProvider;
    
    /**
     * Default constructor for CryptoConfig
     */
    public CryptoConfig() {
        this(null, null, null, null);
    }

    @JsonCreator
    public CryptoConfig(@JsonProperty("asyncAlgoritm") String asyncAlgorithm,
            @JsonProperty("asyncProvider") String asyncProvider, @JsonProperty("sigAlgoritm") String sigAlgorithm,
            @JsonProperty("sigProvider") String sigProvider) {
        super();
        this.asyncAlgorithm = Strings.isNullOrEmpty(asyncAlgorithm) ? "RSA" : asyncAlgorithm;
        this.asyncProvider = asyncProvider;
        this.sigAlgorithm = Strings.isNullOrEmpty(sigAlgorithm) ? "SHA256withRSA" : sigAlgorithm;
        this.sigProvider = sigProvider;
    }

    /**
     * converts this object to a map that can be used by the generic library code
     * 
     * @return see description
     */
    public Map<String, String> asMap() {
        Map<String, String> result = new HashMap<>();
        result.put("asyncProvider", asyncProvider);
        result.put("asyncAlgorithm", asyncAlgorithm);
        result.put("sigProvider", sigProvider);
        result.put("sigAlgorithm", sigAlgorithm);
        return result;
    }
}
