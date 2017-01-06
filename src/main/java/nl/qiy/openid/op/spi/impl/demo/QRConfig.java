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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for QRCode generation, will default to:
 * <dl>
 * <dt>width
 * <dd>150 px
 * <dt>height
 * <dd>150 px
 * <dt>errorCorrection
 * <dd>L
 * <dt>margin
 * <dd>0 px
 * </dl>
 * Values (if any) should be given without units (i.e. {"width": 130})
 *
 * @author Friso Vrolijken
 * @since 9 mei 2016
 */
public final class QRConfig {

    public final Integer width;
    public final Integer height;
    public final String errorCorrection;
    public final Integer margin;
    public final Integer logoWidth;
    public final Integer logoHeight;

    /**
     * Default constructor for QRConfig, using all the defaults
     */
    public QRConfig() {
        this(null, null, null, null, null, null);
    }

    /**
     * Constructor for QRConfig that sets all the values (using defaults if given values are null).
     * 
     * @param width
     *            in pixels
     * @param height
     *            in pixels
     * @param errorCorrection
     *            possible values:
     *            <dl>
     *            <dt>L
     *            <dd>up to 7% damage
     *            <dt>M
     *            <dd>up to 15% damage
     *            <dt>Q
     *            <dd>up to 25% damage
     *            <dt>H
     *            <dd>up to 30% damage
     *            </dl>
     * @param margin
     *            in pixels
     * @param logoWidth
     *            the width of the Qiy logo within the QR code
     * @param logoHeight
     *            the height of the Qiy logo within the QR code
     */
    // @formatter:off
    @JsonCreator
    public QRConfig(
            @JsonProperty("width") Integer width, 
            @JsonProperty("height") Integer height,
            @JsonProperty("errorCorrection") String errorCorrection, 
            @JsonProperty("margin") Integer margin,
            @JsonProperty("logoWidth") Integer logoWidth, 
            @JsonProperty("logoHeight") Integer logoHeight) {// @formatter:on
        super();
        this.width = width == null ? Integer.valueOf(150) : width;
        this.height = height == null ? Integer.valueOf(150) : height;
        this.errorCorrection = errorCorrection == null ? "L" : errorCorrection;
        this.margin = margin == null ? Integer.valueOf(0) : margin;
        this.logoHeight = logoHeight == null ? Integer.valueOf(33) : logoHeight;
        this.logoWidth = logoWidth == null ? Integer.valueOf(46) : logoWidth;
    }
}
