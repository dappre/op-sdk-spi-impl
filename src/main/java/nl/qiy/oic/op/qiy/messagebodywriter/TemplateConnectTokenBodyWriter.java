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
package nl.qiy.oic.op.qiy.messagebodywriter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import nl.qiy.oic.op.qiy.QiyConnectTokenRepresentation;

/**
 * Pluggable writer for the class {@link QiyConnectTokenRepresentation}
 *
 * @author friso
 * @since 16 dec. 2016
 */
@Provider
public class TemplateConnectTokenBodyWriter implements MessageBodyWriter<QiyConnectTokenRepresentation> {
    /** 
     * Standard SLF4J Logger 
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateConnectTokenBodyWriter.class);
    private static final STGroup stGroup = new STGroup('$', '$');
    private static final Map<MediaType, ST> templatePrototypes = new ConcurrentHashMap<>();

    public static void registerTemplate(String template, MediaType mediaType) {
        if (template == null || template.trim().length() == 0 || mediaType == null) {
            throw new IllegalArgumentException("Cannot register " + template + ", " + mediaType);
        }
        templatePrototypes.put(mediaType, new ST(stGroup, template));
        LOGGER.debug("template registered for media type: {}\n{}\n", mediaType, template);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == QiyConnectTokenRepresentation.class && templatePrototypes.containsKey(mediaType);
    }

    @Override
    public long getSize(QiyConnectTokenRepresentation t, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        // deprecated by JAX-RS 2.0 and ignored by Jersey runtime
        return 0;
    }

    @Override
    public void writeTo(QiyConnectTokenRepresentation t, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException {
        ST template = new ST(templatePrototypes.get(mediaType));
        template.add("dappreappuri", t.getDappreAppUri());
        template.add("notificationuri", t.getNotificationUri());
        template.add("connectqrcode", Base64.getEncoder().encodeToString(t.getQrCode()));
        template.add("connectjson", t.getQrJson());
        byte[] content = template.render().getBytes(StandardCharsets.UTF_8);
        entityStream.write(content);
    }
}
