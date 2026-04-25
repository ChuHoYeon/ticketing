package com.ticketing.queue_server.controller;

import com.ticketing.queue_server.dto.RegisterUserResponse;
import com.ticketing.queue_server.service.UserQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class UserQueueController {
    private final UserQueueService userQueueService;

    // 대기열 등록 API
    @PostMapping("")
    public Mono<RegisterUserResponse> registerUser(@RequestParam(value = "queue", defaultValue = "default") String queue,
                                                   @RequestParam("user_id") Long userId) {
        return userQueueService.registerWaitQueue(queue, userId)
                .map(RegisterUserResponse::new);
    }
}
