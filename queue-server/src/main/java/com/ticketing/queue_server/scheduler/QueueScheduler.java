package com.ticketing.queue_server.scheduler;

import com.ticketing.queue_server.service.UserQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 대기열 자동 처리 스케줄러
 *
 * 설정값 scheduler.enabled가 true인 경우에만 활성화됩니다.
 * 일정 주기마다 대기열의 사용자를 진입 가능 상태로 변경하는 서비스 로직을 호출합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true")
public class QueueScheduler {

    private final UserQueueService userQueueService;

    /**
     * 대기열 사용자 자동 진입 허용 스케줄링
     *
     * 애플리케이션 시작 후 5초 뒤 최초 실행되며,
     * 이후 10초 간격으로 반복 실행됩니다.
     *
     * 실제 대기열 처리 로직은 UserQueueService에 위임합니다.
     */
    @Scheduled(initialDelay = 5000, fixedDelay = 10000)
    public void scheduleAllowUser() {
        // 타이머가 돌 때마다 서비스 로직을 호출만 해줍니다.
        userQueueService.scheduleAllowUser();
    }

}
