package com.mindfocus.web;

import com.mindfocus.domain.AppNotification;
import com.mindfocus.domain.PushSubscription;
import com.mindfocus.repo.NotificationRepository;
import com.mindfocus.repo.PushSubscriptionRepository;
import com.mindfocus.security.CurrentUser;
import com.mindfocus.service.PushSender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository notifications;
    private final PushSubscriptionRepository subscriptions;
    private final PushSender push;

    public record Keys(@NotBlank String p256dh, @NotBlank String auth) {}

    public record SubscribeRequest(@NotBlank String endpoint, @NotNull Keys keys) {}

    @GetMapping("/notifications")
    public List<AppNotification> list() {
        return notifications.findTop50ByUserIdOrderByCreatedAtDesc(CurrentUser.id());
    }

    @PostMapping("/notifications/{id}/seen")
    @Transactional
    public void markSeen(@PathVariable Long id) {
        notifications.findByIdAndUserId(id, CurrentUser.id()).ifPresent(n -> n.setSeen(true));
    }

    @PostMapping("/notifications/seen-all")
    @Transactional
    public void markAllSeen() {
        notifications.findByUserId(CurrentUser.id()).forEach(n -> n.setSeen(true));
    }

    /** Public: the browser needs the VAPID key before the user subscribes. Empty when push is not configured. */
    @GetMapping(value = "/push/public-key", produces = "text/plain")
    public String publicKey() {
        return push.publicKey();
    }

    @PostMapping("/push/subscribe")
    @Transactional
    public void subscribe(@Valid @RequestBody SubscribeRequest req) {
        if (subscriptions.existsByEndpoint(req.endpoint())) return;
        PushSubscription s = new PushSubscription();
        s.setUserId(CurrentUser.id());
        s.setEndpoint(req.endpoint());
        s.setP256dh(req.keys().p256dh());
        s.setAuth(req.keys().auth());
        subscriptions.save(s);
    }
}
