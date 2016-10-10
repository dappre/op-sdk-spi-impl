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

import java.io.Serializable;
import java.net.URI;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * TODO: friso should have written a comment here to tell us what this class does
 *
 * @author Friso Vrolijken
 * @since 1 aug. 2016
 */
public class QiyConnection implements Serializable {
    /**
     * Generated
     */
    private static final long serialVersionUID = 1L;
    public final Date activeFrom;
    public final Date activeUntil;

    public final Map<String, URI> links;

    public QiyConnection(@JsonProperty("activeFrom") Date activeFrom, @JsonProperty("activeUntil") Date activeUntil,
            @JsonProperty("links") Map<String, URI> links) {
        super();
        this.activeFrom = activeFrom;
        this.activeUntil = activeUntil;
        this.links = links;
    }
    
}
