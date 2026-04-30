package com.ticketing.queue_server.controller;

import com.ticketing.queue_server.service.UserQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpCookie;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class WaitingRoomController {

    private final UserQueueService userQueueService;

    /**
     * 대기실 페이지 진입 API
     *
     * 사용자가 서비스 페이지에 바로 진입할 수 있는지 확인하고,
     * 진입 가능한 경우 요청받은 redirectUrl로 리다이렉트합니다.
     *
     * 진입 토큰이 없거나 유효하지 않은 경우 사용자를 대기열에 등록하고,
     * 현재 대기 순번과 함께 대기실 화면을 반환합니다.
     *
     * 이미 대기열에 등록된 사용자인 경우 새로 등록하지 않고 기존 대기 순번을 조회합니다.
     *
     * HTTP Method: GET
     * URL: /waiting-room
     *
     * @param queue 대기열 이름, 값이 없으면 "default" 대기열을 사용합니다.
     * @param userId 대기열에 등록하거나 진입 여부를 확인할 사용자 ID입니다.
     * @param redirectUrl 대기열 통과 후 이동할 서비스 페이지 URL입니다.
     * @param exchange 현재 HTTP 요청과 응답 정보를 담고 있는 WebFlux exchange 객체입니다.
     * @return 진입 가능 시 리다이렉트 응답, 대기 필요 시 대기실 화면 렌더링 응답
     */
    @GetMapping("/waiting-room")
    public Mono<Rendering> waitingRoomPage(@RequestParam(name = "queue", defaultValue = "default") String queue,
                                           @RequestParam(name = "user_id") Long userId,
                                           @RequestParam(name = "redirect_url") String redirectUrl,
                                           ServerWebExchange exchange) {
        String key = "user-queue-%s-token".formatted(queue);
        HttpCookie cookieValue = exchange.getRequest().getCookies().getFirst(key);
        String token = cookieValue == null ? "" : cookieValue.getValue();

        return userQueueService.isAllowedByToken(queue, userId, token)
                .filter(allowed -> allowed)
                .flatMap(_ -> Mono.just(Rendering.redirectTo(redirectUrl).build()))
                .switchIfEmpty(
                        userQueueService.registerWaitQueue(queue, userId)
                                .onErrorResume(ex -> userQueueService.getRank(queue, userId))
                                .map(rank -> Rendering.view("waiting-room")
                                        .modelAttribute("number", rank)
                                        .modelAttribute("userId", userId)
                                        .modelAttribute("queue", queue)
                                        .build()
                                )
                );
    }
}
