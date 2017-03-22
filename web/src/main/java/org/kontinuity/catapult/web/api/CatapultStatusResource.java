package org.kontinuity.catapult.web.api;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import javax.enterprise.event.Observes;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.kontinuity.catapult.core.api.StatusMessageEvent;

/**
 * A websocket based resource that informs clients about the status of the operations
 *
 * https://abhirockzz.wordpress.com/2015/02/10/integrating-cdi-and-websockets/
 */
@ServerEndpoint("/status")
public class CatapultStatusResource {
    private static final Logger log = Logger.getLogger(CatapultStatusResource.class.getName());

    private static Map<UUID, Session> peers = Collections.synchronizedMap(new WeakHashMap<>());

    private final ObjectMapper objectMapper = new ObjectMapper();

    @OnClose
    public void onClose(Session session, CloseReason closeReason) throws IOException {
        for (Map.Entry<UUID, Session> key : peers.entrySet()) {
            if (Objects.equals(key.getValue().getId(), session.getId())) {
                peers.remove(key.getKey());
            }
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        peers.put(UUID.fromString(message), session);
    }

    /**
     * Listen to status changes and pushes back to the registered sessions
     *
     * @param msg
     * @throws IOException
     */
    public void onEvent(@Observes StatusMessageEvent msg) throws IOException {
        Session session = peers.get(msg.getId());
        if (session != null) {
            //TODO Use AsyncRemote instead?
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(msg));
        } else {
            log.warning(() -> "no active websocket session found for projectile with ID " + msg.getId());
        }
    }
}