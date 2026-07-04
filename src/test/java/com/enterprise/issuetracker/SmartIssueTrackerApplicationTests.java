package com.enterprise.issuetracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SmartIssueTrackerApplicationTests {

    @Test
    void contextLoads() {
        // Smoke test verifying that all Spring configurations compile and context loads.
    }
}
