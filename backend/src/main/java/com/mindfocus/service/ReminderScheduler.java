package com.mindfocus.service;

import com.mindfocus.config.AppProperties;
import com.mindfocus.domain.AppNotification;
import com.mindfocus.domain.Plan;
import com.mindfocus.domain.PushSubscription;
import com.mindfocus.domain.User;
import com.mindfocus.repo.NotificationRepository;
import com.mindfocus.repo.PlanRepository;
import com.mindfocus.repo.PushSubscriptionRepository;
import com.mindfocus.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Every minute: find plans whose reminder time has arrived and notify the owner
 * in three ways (in-app bell, email, browser push). Each plan is reminded once.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm");

    private final PlanRepository plans;
    private final UserRepository users;
    private final NotificationRepository notifications;
    private final PushSubscriptionRepository subscriptions;
    private final PushSender push;
    private final JavaMailSender mail;
    private final AppProperties props;

    @Scheduled(fixedDelay = 60_000, initialDelay = 15_000)
    @Transactional
    public void run() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of(props.zone()));
        for (Plan p : plans.findByDoneFalseAndReminderSentFalseAndDeadlineNotNullAndDeadlineBefore(now.plusDays(14))) {
            LocalDateTime fireAt = p.getDeadline().minusMinutes(p.getReminderMinutesBefore());
            if (fireAt.isAfter(now)) continue;
            notifyOwner(p);
            p.setReminderSent(true);
        }
    }

    private void notifyOwner(Plan p) {
        String title = "Deadline: " + p.getTitle();
        String message = "Due " + p.getDeadline().format(FMT);

        AppNotification n = new AppNotification();
        n.setUserId(p.getUserId());
        n.setTitle(title);
        n.setMessage(message);
        notifications.save(n);

        subscriptions.findByUserId(p.getUserId()).forEach(s -> push.send(s, title, message));

        User user = users.findById(p.getUserId()).orElse(null);
        if (user != null) {
            try {
                SimpleMailMessage m = new SimpleMailMessage();
                m.setFrom(props.mailFrom());
                m.setTo(user.getEmail());
                m.setSubject(title);
                m.setText("Hi " + user.getDisplayName() + ",\n\n\"" + p.getTitle() + "\" is due "
                        + p.getDeadline().format(FMT) + ".\n\nYour Dayfolio");
                mail.send(m);
            } catch (Exception e) {
                log.warn("Reminder email to {} failed: {}", user.getEmail(), e.getMessage());
            }
        }
    }
}
