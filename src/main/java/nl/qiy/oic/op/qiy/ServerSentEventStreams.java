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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.dropwizard.lifecycle.Managed;

/**
 * Manages the EventOutputs
 *
 * @author Friso Vrolijken
 * @since 24 mrt. 2016
 */
public class ServerSentEventStreams implements Managed {
    /**
     * If we want to log an Exception with trace (so that Sonar will not bug us and we have the option to actually see
     * it), the real error message has already been logged. Put this there as an error message.
     */
    private static final String DUMMY_ERROR = "Error";

    /**
     * A separate class to see if any EventOutput may be deleted. An EventOutput may be deleted if it was closed by
     * either the server side (i.e. we called close() on it) or the client side (i.e. a browser, mobile client) has
     * closed it or gone away.
     *
     * @author Friso Vrolijken
     * @since 27 okt. 2016
     */
    private static class ClosedEventOutputCleaner implements Runnable {
        private static final Logger LOG = LoggerFactory.getLogger(ClosedEventOutputCleaner.class);
        private final ServerSentEventStreams caller;

        /**
         * Constructor for ClosedEventOutputCleaner
         */
        ClosedEventOutputCleaner(ServerSentEventStreams caller) {
            super();
            this.caller = caller;
        }

        @Override
        public void run() {
            // Mark and sweep. First mark any Event output that may be closed from either end (client or server), than
            // remove all closed events from both caches
            Thread.currentThread().setName("ServerSentEventStreams-heartbeat-" + System.currentTimeMillis());
            Set<String> removals = caller.findRemovableStreamIds();
            LOG.debug("Heartbeat: about to remove {} EventOutputs", removals.size());
            removals.forEach(caller::remove);
        }
    }

    /**
     * Standard SLF4J Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerSentEventStreams.class);

    /**
     * Singleton object
     */
    private static final ServerSentEventStreams instance = new ServerSentEventStreams();

    // @formatter:off
    private final Cache<String, EventOutput> eventOutput2StreamId = CacheBuilder
            .newBuilder()
            .maximumSize(1_000_000)
            .expireAfterWrite(30L, TimeUnit.MINUTES)
            .removalListener(notification -> {
                LOGGER.debug("streamId {} is being removed from storage", notification.getKey());
                String key = (String) notification.getKey();
                @SuppressWarnings("resource")
                EventOutput value = (EventOutput) notification.getValue();
                try {
                    if (value == null) {
                        LOGGER.info("null value for streamId {}", key);
                    } else if (value.isClosed()) {
                        LOGGER.info("Stream {} for streamId {} was already closed", value.hashCode(), key);
                    } else {
                        value.close();
                        LOGGER.info("Stream {} for streamId {} closed", value.hashCode(), key);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Error while closing stream {} for streamId {}", value, key, e);
                }
            })
            .build();
    // @formatter:on

    /**
     * checks at regular intervals if every {@link EventOutput} that is registered here is still alive. If the
     * {@link EventOutput} is closed, it is removed from this object.
     */
    private final ScheduledExecutorService heartbeatThread = Executors.newScheduledThreadPool(1);

    /**
     * Default constructor for ServerSentEventStreams
     */
    private ServerSentEventStreams() {
        super();
    }

    /**
     * Returns the singleton object
     * 
     * @return see description
     */
    public static ServerSentEventStreams getInstance() {
        return instance;
    }

    /**
     * Store a new {@link EventOutput} and return that
     * 
     * @param streamId
     *            the streamId of the {@link EventOutput}, which will be used to find it
     * @return see description
     */
    public EventOutput newEventOutput(String streamId) {
        EventOutput eventOutput = new EventOutput();
        eventOutput2StreamId.put(streamId, eventOutput);
        LOGGER.info("Stream {} for streamId {} opened.", eventOutput.hashCode(), streamId);
        return eventOutput;
    }

    /**
     * Write information to all the {@link EventOutput EventOutputs} for a given streamId (if any)
     * 
     * @param streamId
     *            the streamId of the {@link EventOutput}
     * @param chunk
     *            what to write
     */
    @SuppressWarnings("resource")
    public boolean write(String streamId, OutboundEvent chunk) {
        // DOES NOT get called when client side closes the connection
        EventOutput eventOutput = eventOutput2StreamId.getIfPresent(streamId);
        if (eventOutput == null || eventOutput.isClosed()) {
            // whoever closed it, should have already removed this. Else the heart beat job will take care of removing
            // it
            return false;
        }
        // else
        try {
            eventOutput.write(chunk);
        } catch (Exception e) {
            LOGGER.info("Write event to stream {} for streamId {} failed. Removing stream", eventOutput.hashCode(),
                    streamId);
            LOGGER.trace(DUMMY_ERROR, e);
            remove(streamId);
        }
        return true;
    }

    /**
     * @return the list of EventOutputs that this object holds a reference to that have been closed
     */
    Set<String> findRemovableStreamIds() {
        eventOutput2StreamId.cleanUp();
        // @formatter:off
        return eventOutput2StreamId
                .asMap()
                .entrySet()
                .stream()
                .filter(e -> isRemovable(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet()); // @formatter:on
    }

    /**
     * Reports whether the eventOutput has been closed, either by the server (us) or the client (browser, app)
     * 
     * @param eventOutput
     *            object to test
     * @return true if the object has been closed
     */
    private static boolean isRemovable(EventOutput eventOutput) {
        // if we closed the output, it may be removed
        if (eventOutput.isClosed()) {
            LOGGER.info("Marking stream {} for removal", eventOutput.hashCode());
            return true;
        }
        // else
        // if the client closed the output, it may be removed as well. Contrary to what one might think, the
        // EventOutput is not closed in such an occasion, the underlying TCP connection is in a state CLOSE_WAIT.
        // So we send some dummy content over the wire. If that fails we can close the event output.
        try {
            OutboundEvent ping = new OutboundEvent.Builder().comment("ping").build();
            eventOutput.write(ping);
            LOGGER.debug("Stream {} pinged. Keeping it", eventOutput.hashCode());
            // this one looks to be in working order, keep it
            return false;
        } catch (Exception e) {
            LOGGER.info("Ping stream {} failed. Probably closed client side. Marking for removal",
                    eventOutput.hashCode());
            LOGGER.trace(DUMMY_ERROR, e);
            return true;
        }
    }

    /**
     * Removes an {@link EventOutput} from the internal storage. This will cause it to be closed if it was not so
     * already
     * 
     * @param streamId
     *            the identifier
     */
    void remove(String streamId) {
        // listener should take care of the rest
        eventOutput2StreamId.invalidate(streamId);
    }

    @Override
    public void start() throws Exception {
        // TODO make configurable
        heartbeatThread.execute(() -> Thread.currentThread().setName("ServerSentEventStreams-heartbeat"));
        heartbeatThread.scheduleWithFixedDelay(new ClosedEventOutputCleaner(this), 10, 10, TimeUnit.SECONDS);
        LOGGER.info("Running heartbeat every 10 s. (not configurable)");
    }

    @Override
    public void stop() throws Exception {
        heartbeatThread.shutdownNow();
        LOGGER.info("Shut down heartbeat");
    }



}
