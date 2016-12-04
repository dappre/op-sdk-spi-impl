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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

import nl.qiy.oic.op.api.AuthenticationRequest;
import nl.qiy.oic.op.api.AuthenticationResponse;
import nl.qiy.oic.op.domain.OAuthUser;
import nl.qiy.oic.op.service.ConfigurationService;
import nl.qiy.oic.op.service.OAuthUserService;
import nl.qiy.oic.op.service.spi.AuthorizationFlow;
import nl.qiy.oic.op.service.spi.Configuration;
import nl.qiy.openid.op.spi.impl.demo.OpSdkSpiImplConfiguration;
import nl.qiy.openid.op.spi.impl.demo.OpSdkSpiImplConfiguration.CardLoginOption;

/**
 * The authorization flow that will allow the user to log in using her Qiy Node. It starts the flow by displaying a QR
 * code. When a callback is invoked, the flow ends.
 *
 * @author Friso Vrolijken
 * @since 12 mei 2016
 */
@Path("qiy")
public class QiyAuthorizationFlow implements AuthorizationFlow {
    /**
     * Standard SLF4J Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(QiyAuthorizationFlow.class);
    private static final Random RANDOM = new SecureRandom();
    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(8);
    private static QiyAuthorizationFlow instance;
    private static ServerSentEventStreams eventStreams;

    // I suppose we should want to migrate this to Redis or something
    private static final Map<String, HttpSession> TO_BE_LOGGED_IN = new HashMap<>();

    private static MessageFormat pageFormat;
    private static UriBuilder notificationUriBuilder;
    private static UriBuilder callbackUriBuilder;

    // possibly null, if we're only after login and not concerned about sharing cards
    private static URL baseDappreUrl;

    private static class StartFlowCombiner implements BinaryOperator<Response> {
        /**
         * Default constructor for StartFlowCombiner
         */
        StartFlowCombiner() {
            super();
        }

        @Override
        public Response apply(Response r1, Response r2) {
            // TODO improve this implementation, should maybe be something like Sitemesh, until than: first non-null is
            // returned, or null if both are null
            if (r1 == null) {
                return r2;
            }
            // else
            if (r2 == null) {
                return r1;
            }
            // else both are non-null
            // check to see which one we generated, return that, ignore the other
            return r1;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Register a callback URIwith our node and display the resulting json as QR
     * code. Returns a HTML page consisting only of HTML code.
     */
    @Override
    public Response startFlow(AuthenticationRequest inputs, HttpSession session) {
        LOGGER.debug("registering callback through Qiy node client");
        String random = getTBLIRandom();
        TO_BE_LOGGED_IN.put(random, session);

        // FIXME optionally listen for events, should only happen in a way that only allows for a dev setup
        // QiyNodeClient.listen(uri, evtConsumer);

        Map<String, Object> callbackDef = getCallbackDefinition(inputs, random);
        QiyNodeClient client = QiyNodeClient.registerCallback(inputs, callbackDef, baseDappreUrl);
        String qrCode = Base64.getEncoder().encodeToString(client.connectTokenAsQRCode());
        String qrJson = client.connectTokenAsJson();
        String dappreConnectUri = client.connectTokenAsDappreLink();

        URI notificationURL = getNotificationUrl(random);
        MessageFormat format = getPageFormat();
        Object[] args = { qrCode, qrJson, notificationURL, dappreConnectUri };
        String page = format.format(args);

        // FIXME optionally return another media type (JSON) here

        return Response.ok(page, MediaType.TEXT_HTML).build();
    }

    /**
     * Defines what the callback should look like. We're expecting a POST with media type "application/json" and the
     * body should be the original input. That way we don't have to keep the state here
     * 
     * @param inputs
     * @param random
     * @return see description
     */
    private static Map<String, Object> getCallbackDefinition(AuthenticationRequest inputs, String random) {
        Map<String, Object> callbackDef = new HashMap<>();
        String callbackUri = getCallbackUri(random);
        callbackDef.put("uri", callbackUri);
        callbackDef.put("method", "POST");
        callbackDef.put("type", MediaType.APPLICATION_JSON);
        callbackDef.put("body", inputs.toBytes());
        return callbackDef;
    }

    /**
     * @return A random identifier for a ToBeLoggedIn user
     */
    private static String getTBLIRandom() {
        String sRandom;
        do {
            byte[] random = new byte[32];
            RANDOM.nextBytes(random);
            // NB: this conversion does not play well with leading zeros (they get skipped as leading zeros are
            // meaningless to numbers). For our purposes that doesn't matter, change this code if it does
            sRandom = new BigInteger(1, random).toString(36);
        } while (sRandom == null || TO_BE_LOGGED_IN.containsKey(sRandom));
        return sRandom;
    }

    private static URI getNotificationUrl(String sRandom) {
        if (notificationUriBuilder == null) {
            String baseUri = ConfigurationService.get(Configuration.BASE_URI);
            UriBuilder tmp = UriBuilder.fromUri(baseUri);
            // @formatter:off
            try(Stream<Method> stream = Arrays.stream(QiyAuthorizationFlow.class.getDeclaredMethods())) {
                Method method = stream 
                        .filter(m -> "watchLoginStatus".equals(m.getName()))
                        .findFirst()
                        .orElseThrow(IllegalStateException::new);
                // @formatter:on
                tmp = tmp.path(QiyAuthorizationFlow.class).path(method);
            }
            notificationUriBuilder = tmp;
        }
        return notificationUriBuilder.build(sRandom);
    }

    private MessageFormat getPageFormat() {
        if (pageFormat == null) {
            LOGGER.info("reading login page format {}", this.getClass().getResource("/loginPageFormat.html"));
            try (Reader sr = new InputStreamReader(this.getClass().getResourceAsStream("/loginPageFormat.html"));
                    BufferedReader reader = new BufferedReader(sr)) {
                MessageFormat mf = new MessageFormat(reader.lines().collect(Collectors.joining("\n")));
                pageFormat = mf; // NOSONAR
            } catch (IOException e) {
                LOGGER.warn("Error while doing getPageFormat", e);
                throw new IllegalStateException("illegal page format");
            }
        }
        return pageFormat;
    }

    @Override
    public BinaryOperator<Response> startFlowCombiner() {
        return new StartFlowCombiner();
    }


    @Override
    public boolean isHealthy() {
        String callbackUri = getCallbackUri("testmijnu");
        String notifictionUri = getNotificationUrl("testmijook").toString();
        // @formatter:off
        boolean result = callbackUri != null && 
                callbackUri.contains("testmijnu") && 
                notifictionUri != null && 
                notifictionUri.contains("testmijook");
        // @formatter:on
        LOGGER.debug("{} isHealthy called: {}", this.getClass(), result);
        return result;
    }

    /**
     * Registers for events for a given random id. Expect at some point in the future that there will be an event there
     * when the user has logged in
     * 
     * @param random
     *            generated at {@link #startFlow(AuthenticationRequest, HttpSession)}
     * @param request
     *            used for the session, there may already be a logged in user
     * @return the event-stream
     */
    @Path("watch/{random}")
    @GET
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    @SuppressWarnings({ "ucd", "resource" })
    public static Response watchLoginStatus(@PathParam("random") String random, @Context HttpServletRequest request) {
        EventOutput eventOutput = null;
        Optional<OAuthUser> loggedIn = OAuthUserService.getLoggedIn(request.getSession());
         if (loggedIn.isPresent()) {
            notifyUserLoggedIn(random, loggedIn.get(), null);
         } else {
            eventOutput = eventStreams.newEventOutput(random);
         }
        // @formatter:off
        return Response.ok()
                .header(HttpHeaders.CONTENT_ENCODING, "identity")
                .entity(eventOutput)
                .build(); // @formatter:on
    }

    /**
     * This method gets invoked when the user has made a connection with the node. The URL for this has been registered
     * in {@link #startFlow(AuthenticationRequest, HttpSession)} and upon connection the node invokes it. The user can
     * now be uniquely identified, so call login
     * 
     * @param random
     *            the random value that was used in {@link #startFlow(AuthenticationRequest, HttpSession)} to register
     *            this login attempt
     * @param cbInput
     *            the input we said we'd like to receive
     * @return Normally a 200 (OK) response to signal to the router (who made this request) that the content was
     *         received in working order
     */
    @Path("callback/{random}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @SuppressWarnings("ucd")
    public static Response callbackFromQiyNode(@PathParam("random") String random, CallbackInput cbInput) {
        try {
            LOGGER.debug("Callback from Qiy node invoked for random {}", random);
            HttpSession session = TO_BE_LOGGED_IN.get(random);
            if (session == null) {
                String msg = "No session waits for login with id " + random;
                LOGGER.warn(msg);
                throw new IllegalArgumentException(msg);
            }

            LOGGER.info("Calling login for {}", cbInput.pid);
            QiyOAuthUser template = new QiyOAuthUser(cbInput);
            OAuthUser oAuthUser = OAuthUserService.login(template, session);

            // works, but don't want it, look in scm history for a listen that doesn't work yet
            if (oAuthUser == null) {
                if (OpSdkSpiImplConfiguration.getInstance().cardLoginOption == CardLoginOption.NO_CARD) {
                    LOGGER.warn("No logged in user found after callback {}", template.getSubject());
                    notify(random, "error", "not logged in after callback");
                    return Response.ok().build(); // node can't help it so answer OK to it
                }
                LOGGER.info("No user returned, submitting loop to thread pool");
                THREAD_POOL.execute(() -> {
                    for (int i = 0; i < 120; i++) {
                        try {
                            Thread.sleep(500);
                        } catch (Exception e) {
                            LOGGER.warn("Error while doing ", e);
                            throw new IllegalStateException(e);
                        }
                        OAuthUser loggedIn = OAuthUserService.login(template, session);
                        if (loggedIn != null) {
                            notifyUserLoggedIn(random, loggedIn, cbInput);
                            break;
                        }
                    }
                });
            } else {
                notifyUserLoggedIn(random, oAuthUser, cbInput);
            }
            return Response.ok().build();
        } catch (Throwable t) {
            LOGGER.warn("Error while doing callbackFromQiyNode", t);
            throw Throwables.propagate(t);
        }
    }

    private static void notifyUserLoggedIn(String random, OAuthUser oAuthUser, CallbackInput cbInput) {
        LOGGER.info("Notifying {} of login with random {}", oAuthUser.getSubject(), random, cbInput.pid);
        AuthenticationRequest request = AuthenticationRequest.fromBytes(cbInput.body);
        Response response = AuthenticationResponse.getResponse(request, oAuthUser);
        Map<String, String> body = new HashMap<>();
        if (response.getStatusInfo().getFamily() == Status.Family.REDIRECTION) {
            body.put("url", response.getLocation().toString());
        } else if (response.getStatusInfo().getFamily() == Status.Family.SUCCESSFUL) {
            Object entity = response.getEntity();
            body.put("page", entity == null ? null : entity.toString());
        }
        notify(random, "loggedIn", body);
        // final event, so clean up
        eventStreams.remove(random);
    }

    private static void notify(String streamId, String eventName, Object eventData) {
        // @formatter:off
        OutboundEvent chunk = new OutboundEvent.Builder()
                .name(eventName)
                .data(eventData)
                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                .build();
        // @formatter:on
        eventStreams.write(streamId, chunk);
    }

    private static String getCallbackUri(String random2) {
        if (callbackUriBuilder == null) {
            String baseUri = ConfigurationService.get(Configuration.BASE_URI);
            UriBuilder tmp = UriBuilder.fromUri(baseUri);
            try (Stream<Method> stream = Arrays.stream(QiyAuthorizationFlow.class.getDeclaredMethods())) {
                // @formatter:off
                Method method = stream
                        .filter(m -> "callbackFromQiyNode".equals(m.getName()))
                        .findFirst()
                        .orElseThrow(IllegalStateException::new);
                // @formatter:on
                tmp = tmp.path(QiyAuthorizationFlow.class).path(method);
            }
            callbackUriBuilder = tmp;
        }
        return callbackUriBuilder.build(random2).toString();
    }

    /**
     * Gets the singleton instance, setting the cardMessageUrl in the process if the instance was null
     * 
     * @param baseDappreURL
     *            the base URL for Dappre
     * @return see description
     */
    public static QiyAuthorizationFlow getInstance(URL baseDappreURL) {
        if (instance == null) {
            baseDappreUrl = baseDappreURL;
            instance = new QiyAuthorizationFlow();
            eventStreams = ServerSentEventStreams.getInstance();
        }
        return instance;
    }

}
