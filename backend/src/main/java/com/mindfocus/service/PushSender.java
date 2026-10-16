package com.mindfocus.service;

import com.mindfocus.config.AppProperties;
import com.mindfocus.domain.PushSubscription;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import java.security.Security;
import java.util.Map;

/** Sends Web Push messages. Does nothing when no VAPID keys are configured. */
@Component
@Slf4j
public class PushSender {
    private final ObjectMapper mapper;
    private final String publicKey;
    private PushService service;

    public PushSender(AppProperties props, ObjectMapper mapper) {
        this.mapper = mapper;
        this.publicKey = props.push().publicKey() == null ? "" : props.push().publicKey();
        String priv = props.push().privateKey();
        if (!publicKey.isBlank() && priv != null && !priv.isBlank()) {
            try {
                Security.addProvider(new BouncyCastleProvider());
                this.service = new PushService(publicKey, priv, props.push().subject());
            } catch (Exception e) {
                log.warn("Web Push disabled, bad VAPID keys: {}", e.getMessage());
            }
        } else {
            log.info("Web Push disabled: set VAPID_PUBLIC and VAPID_PRIVATE to enable it");
        }
    }

    public String publicKey() {
        return publicKey;
    }

    public boolean enabled() {
        return service != null;
    }

    public void send(PushSubscription sub, String title, String body) {
        if (service == null) return;
        try {
            String payload = mapper.writeValueAsString(Map.of("title", title, "body", body));
            service.send(new Notification(sub.getEndpoint(), sub.getP256dh(), sub.getAuth(), payload));
        } catch (Exception e) {
            log.warn("Push to {} failed: {}", sub.getEndpoint(), e.getMessage());
        }
    }
}
