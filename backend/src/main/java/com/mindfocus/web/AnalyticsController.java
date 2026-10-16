package com.mindfocus.web;

import com.mindfocus.security.CurrentUser;
import com.mindfocus.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService analytics;

    @GetMapping("/summary")
    public AnalyticsService.Summary summary(@RequestParam(defaultValue = "14") int days) {
        return analytics.summary(CurrentUser.id(), Math.max(1, Math.min(days, 90)));
    }
}
