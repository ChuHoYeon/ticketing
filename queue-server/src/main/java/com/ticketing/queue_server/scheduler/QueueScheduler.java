package com.ticketing.queue_server.scheduler;

import com.ticketing.queue_server.service.UserQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true")
public class QueueScheduler {

    private final UserQueueService userQueueService;

    @Scheduled(initialDelay = 5000, fixedDelay = 10000)
    public void scheduleAllowUser() {
        // 타이머가 돌 때마다 서비스 로직을 호출만 해줍니다.
        userQueueService.scheduleAllowUser();
    }

}
