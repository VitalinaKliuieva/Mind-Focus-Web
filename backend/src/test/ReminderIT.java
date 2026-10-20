package com.mindfocus;

import com.mindfocus.config.AppProperties;
import com.mindfocus.service.ReminderScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The reminder job: the part of the system that works without any user action, so it needs the most care. */
class ReminderIT extends AbstractIntegrationTest {

    @Autowired
    private ReminderScheduler scheduler;

    @Autowired
    private AppProperties props;

    private String deadlineIn(long minutes) {
        return LocalDateTime.now(ZoneId.of(props.zone())).plusMinutes(minutes).withNano(0).toString();
    }

    private static String plan(String title, String deadline, int remindMinutesBefore) {
        return j("{'title':'" + title + "','type':'DAY','planDate':'2026-10-17','deadline':'" + deadline
                + "','reminderMinutesBefore':" + remindMinutesBefore + "}");
    }

    private void verifyMailsTo(String email, int times) {
        verify(mailSender, times(times)).send(argThat((SimpleMailMessage m) ->
                m.getTo() != null && m.getTo().length > 0 && email.equals(m.getTo()[0])));
    }

    @Test
    @DisplayName("a due reminder creates one notification and one email, even if the job runs twice")
    void dueReminderIsSentExactlyOnce() throws Exception {
        String email = uniqueEmail();
        String token = register(email);
        create("/api/plans", token, plan("Hand in report", deadlineIn(10), 60));

        scheduler.run();
        scheduler.run();

        getAs("/api/notifications", token)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Deadline: Hand in report"))
                .andExpect(jsonPath("$[0].seen").value(false));
        getAs("/api/plans", token).andExpect(jsonPath("$[0].reminderSent").value(true));
        verifyMailsTo(email, 1);
    }

    @Test
    @DisplayName("a reminder is not sent before its time")
    void futureReminderWaits() throws Exception {
        String email = uniqueEmail();
        String token = register(email);
        create("/api/plans", token, plan("Far away", deadlineIn(3 * 24 * 60), 60));

        scheduler.run();

        getAs("/api/notifications", token).andExpect(jsonPath("$.length()").value(0));
        getAs("/api/plans", token).andExpect(jsonPath("$[0].reminderSent").value(false));
        verifyMailsTo(email, 0);
    }

    @Test
    @DisplayName("the offset decides when it fires: 1 day before fires for a deadline in 20 hours")
    void offsetIsRespected() throws Exception {
        String token = newUserToken();
        create("/api/plans", token, plan("Offset", deadlineIn(20 * 60), 24 * 60));
        scheduler.run();
        getAs("/api/notifications", token).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("an already overdue plan is still reminded")
    void overduePlanIsReminded() throws Exception {
        String token = newUserToken();
        create("/api/plans", token, plan("Late", deadlineIn(-5), 0));
        scheduler.run();
        getAs("/api/notifications", token).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("a finished plan is not reminded")
    void donePlanIsSkipped() throws Exception {
        String email = uniqueEmail();
        String token = register(email);
        long id = create("/api/plans", token, plan("Already done", deadlineIn(10), 60));
        putJson("/api/plans/" + id, token,
                j("{'title':'Already done','type':'DAY','planDate':'2026-10-17','deadline':'" + deadlineIn(10)
                        + "','reminderMinutesBefore':60,'done':true}")).andExpect(status().isOk());

        scheduler.run();

        getAs("/api/notifications", token).andExpect(jsonPath("$.length()").value(0));
        verifyMailsTo(email, 0);
    }

    @Test
    @DisplayName("moving the deadline re-arms the reminder so it fires again")
    void movedDeadlineRearms() throws Exception {
        String token = newUserToken();
        long id = create("/api/plans", token, plan("Moved", deadlineIn(10), 60));
        scheduler.run();
        getAs("/api/plans", token).andExpect(jsonPath("$[0].reminderSent").value(true));

        putJson("/api/plans/" + id, token, plan("Moved", deadlineIn(25), 60))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reminderSent").value(false));

        scheduler.run();
        getAs("/api/notifications", token).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("editing only the title does not re-arm a reminder that was already sent")
    void titleEditDoesNotRearm() throws Exception {
        String token = newUserToken();
        String deadline = deadlineIn(10);
        long id = create("/api/plans", token, plan("Before", deadline, 60));
        scheduler.run();

        putJson("/api/plans/" + id, token, plan("After", deadline, 60))
                .andExpect(jsonPath("$.reminderSent").value(true));
        scheduler.run();
        getAs("/api/notifications", token).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("if the mail server is down, the in-app reminder is still created and not repeated")
    void mailFailureDoesNotLoseTheReminder() throws Exception {
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));
        String token = newUserToken();
        create("/api/plans", token, plan("Resilient", deadlineIn(10), 60));

        scheduler.run();
        scheduler.run();

        getAs("/api/notifications", token).andExpect(jsonPath("$.length()").value(1));
        getAs("/api/plans", token).andExpect(jsonPath("$[0].reminderSent").value(true));
    }

    @Test
    @DisplayName("notifications are private, can be marked as read, and 'read all' works")
    void notificationsAreReadable() throws Exception {
        String token = newUserToken();
        String other = newUserToken();
        create("/api/plans", token, plan("One", deadlineIn(5), 60));
        create("/api/plans", token, plan("Two", deadlineIn(6), 60));
        scheduler.run();

        getAs("/api/notifications", other).andExpect(jsonPath("$.length()").value(0));

        long firstId = read(getAs("/api/notifications", token)).get(0).get("id").asLong();
        postJson("/api/notifications/" + firstId + "/seen", other, "").andExpect(status().isOk());   // not his: no effect
        getAs("/api/notifications", token).andExpect(jsonPath("$[?(@.seen==true)]").isEmpty());

        postJson("/api/notifications/" + firstId + "/seen", token, "").andExpect(status().isOk());
        postJson("/api/notifications/seen-all", token, "").andExpect(status().isOk());
        getAs("/api/notifications", token).andExpect(jsonPath("$[?(@.seen==false)]").isEmpty());
    }

    @Test
    @DisplayName("completing a plan stores the time, reopening clears it")
    void completionTimestamp() throws Exception {
        String token = newUserToken();
        String body = "{'title':'t','type':'DAY','planDate':'2026-10-17','done':%s}";
        long id = create("/api/plans", token, j(body.formatted("false")));
        putJson("/api/plans/" + id, token, j(body.formatted("true")))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
        putJson("/api/plans/" + id, token, j(body.formatted("false")))
                .andExpect(jsonPath("$.completedAt").isEmpty());
    }
}
