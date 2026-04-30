package com.ticketing.queue_server.controller;

import com.ticketing.queue_server.dto.AllowUserResponse;
import com.ticketing.queue_server.dto.AllowedUserResponse;
import com.ticketing.queue_server.dto.RankNumberResponse;
import com.ticketing.queue_server.dto.RegisterUserResponse;
import com.ticketing.queue_server.service.UserQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class UserQueueController {

    private final UserQueueService userQueueService;

    /**
     * 대기열 등록 API
     *
     * 사용자를 지정한 대기열에 등록합니다.
     * Redis 대기열에 사용자가 정상적으로 등록되면 현재 대기 순번을 반환합니다.
     *
     * 이미 대기열에 등록된 사용자인 경우 예외가 발생할 수 있습니다.
     *
     * HTTP Method: POST
     * URL: /api/v1/queue/
     *
     * @param queue 대기열 이름, 값이 없으면 "default" 대기열을 사용합니다.
     * @param userId 대기열에 등록할 사용자 ID입니다.
     * @return 등록된 사용자의 현재 대기 순번을 포함한 응답
     */
    @PostMapping("/")
    public Mono<RegisterUserResponse> registerUser(@RequestParam(name = "queue", defaultValue = "default") String queue,
                                                   @RequestParam(name = "user_id") Long userId) {
        return userQueueService.registerWaitQueue(queue, userId)
                .map(RegisterUserResponse::new);
    }

    /**
     * 대기열 사용자 진입 허용 API
     *
     * 지정한 대기열에서 대기 중인 사용자 중 요청한 수만큼 사용자를 꺼내
     * 진입 가능 상태로 변경합니다.
     *
     * 주로 관리자 기능 또는 스케줄러에서 대기열을 순차적으로 처리할 때 사용합니다.
     *
     * HTTP Method: POST
     * URL: /api/v1/queue/allow
     *
     * @param queue 대기열 이름, 값이 없으면 "default" 대기열을 사용합니다.
     * @param count 진입 허용 처리할 사용자 수입니다.
     * @return 요청한 허용 수와 실제 허용된 사용자 수를 포함한 응답
     */
    @PostMapping("/allow")
    public Mono<AllowUserResponse> allowUser(@RequestParam(name = "queue", defaultValue = "default") String queue,
                                             @RequestParam(name = "count") Long count) {
        return userQueueService.allowUser(queue, count)
                .map(allowed -> new AllowUserResponse(count, allowed));
    }

    /**
     * 사용자 진입 가능 여부 조회 API
     *
     * 사용자가 전달한 토큰을 검증하여 현재 서비스 페이지에 진입 가능한 상태인지 확인합니다.
     *
     * 토큰이 유효하면 진입 가능 상태로 판단하고,
     * 토큰이 없거나 유효하지 않으면 대기열에서 대기해야 하는 상태로 판단합니다.
     *
     * HTTP Method: GET
     * URL: /api/v1/queue/allowed
     *
     * @param queue 대기열 이름, 값이 없으면 "default" 대기열을 사용합니다.
     * @param userId 진입 가능 여부를 확인할 사용자 ID입니다.
     * @param token 사용자의 대기열 통과 여부를 검증하기 위한 토큰입니다.
     * @return 사용자의 진입 가능 여부를 포함한 응답
     */
    @GetMapping("/allowed")
    public Mono<AllowedUserResponse> isAllowedUser(@RequestParam(name = "queue", defaultValue = "default") String queue,
                                                   @RequestParam(name = "user_id") Long userId,
                                                   @RequestParam(name = "token") String token
                                                   ) {
        return userQueueService.isAllowedByToken(queue, userId, token)
                .map(AllowedUserResponse::new);
    }

    /**
     * 사용자 대기 순번 조회 API
     *
     * 지정한 대기열에서 사용자의 현재 대기 순번을 조회합니다.
     *
     * 사용자가 대기열에 존재하면 1부터 시작하는 순번을 반환하고,
     * 대기열에 존재하지 않으면 -1을 반환합니다.
     *
     * HTTP Method: GET
     * URL: /api/v1/queue/rank
     *
     * @param queue 대기열 이름, 값이 없으면 "default" 대기열을 사용합니다.
     * @param userId 대기 순번을 조회할 사용자 ID입니다.
     * @return 사용자의 현재 대기 순번을 포함한 응답
     */
    @GetMapping("/rank")
    public Mono<RankNumberResponse> getRankUser(@RequestParam(name = "queue", defaultValue = "default") String queue,
                                                @RequestParam(name = "user_id") Long userId) {
        return userQueueService.getRank(queue, userId)
                .map(RankNumberResponse::new);
    }

    /**
     * 사용자 진입 토큰 발급 API
     *
     * 지정한 사용자에 대한 진입 토큰을 생성하고 응답 쿠키에 저장합니다.
     *
     * 발급된 토큰은 이후 진입 가능 여부 조회 API에서 사용자의 진입 권한을
     * 검증하는 데 사용됩니다.
     *
     * 쿠키 이름은 대기열 이름을 포함하여 생성됩니다.
     * 예: user-queue-default-token
     *
     * HTTP Method: GET
     * URL: /api/v1/queue/touch
     *
     * @param queue 대기열 이름, 값이 없으면 "default" 대기열을 사용합니다.
     * @param userId 토큰을 발급할 사용자 ID입니다.
     * @param exchange 현재 HTTP 요청과 응답 정보를 담고 있는 WebFlux exchange 객체입니다.
     * @return 생성된 진입 토큰
     */
    @GetMapping("/touch")
    public Mono<?> touch(@RequestParam(name = "queue", defaultValue = "default") String queue,
                         @RequestParam(name = "user_id") Long userId,
                         ServerWebExchange exchange) {
        return Mono.defer(() -> userQueueService.generateToken(queue, userId))
                .map(token -> {
                    exchange.getResponse().addCookie(
                            ResponseCookie.from("user-queue-%s-token".formatted(queue), token)
                                    .maxAge(Duration.ofSeconds(300))
                                    .path("/")
                                    .build()
                    );

                    return token;
                });
    }
}
