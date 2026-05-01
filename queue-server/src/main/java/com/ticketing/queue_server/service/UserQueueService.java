package com.ticketing.queue_server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.Instant;

import static com.ticketing.common.queue.QueueTokenUtils.generateSHA256Token;
import static com.ticketing.queue_server.exception.ErrorCode.QUEUE_ALREADY_REGISTERED_USER;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueueService {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    private final String USER_QUEUE_WAIT_KEY = "users:queue:%s:wait";
    private final String USER_QUEUE_WAIT_KEY_FOR_SCAN = "users:queue:*:wait";
    private final String USER_QUEUE_PROCEED_KEY = "users:queue:%s:proceed";


    /**
     * 대기열 등록 처리
     *
     * 사용자를 Redis Sorted Set 기반 대기열에 등록합니다.
     * 사용자 ID를 value로 저장하고, 현재 Unix Timestamp를 score로 사용합니다.
     *
     * Redis Key 형식:
     * users:queue:{queue}:wait
     *
     * 이미 등록된 사용자인 경우 QUEUE_ALREADY_REGISTERED_USER 예외를 발생시킵니다.
     *
     * @param queue 대기열 이름입니다.
     * @param userId 등록할 사용자 ID입니다.
     * @return 등록 후 사용자의 현재 대기 순번입니다. 순번은 1부터 시작합니다.
     */
    public Mono<Long> registerWaitQueue(final String queue, final Long userId) {
        long unixTimestamp = Instant.now().getEpochSecond();
        return reactiveRedisTemplate.opsForZSet().add(USER_QUEUE_WAIT_KEY.formatted(queue), userId.toString(), unixTimestamp)
                .filter(i -> i)
                .switchIfEmpty(Mono.error(QUEUE_ALREADY_REGISTERED_USER.build()))
                .flatMap(i -> reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_WAIT_KEY.formatted(queue), userId.toString()))
                .map(rank -> rank >= 0 ? rank + 1 : rank);
    }

    /**
     * 대기열 사용자 진입 허용 처리
     *
     * 대기열에서 가장 먼저 등록된 사용자부터 지정한 수만큼 꺼내고,
     * 해당 사용자를 진입 가능 대기열로 이동시킵니다.
     *
     * Redis wait queue에서 popMin으로 사용자를 제거한 뒤,
     * Redis proceed queue에 사용자를 추가합니다.
     *
     * Redis Key 형식:
     * wait queue: users:queue:{queue}:wait
     * proceed queue: users:queue:{queue}:proceed
     *
     * @param queue 대기열 이름입니다.
     * @param count 진입 허용할 사용자 수입니다.
     * @return 실제로 진입 허용 처리된 사용자 수입니다.
     */
    public Mono<Long> allowUser(final String queue, final Long count) {
        return reactiveRedisTemplate.opsForZSet().popMin(USER_QUEUE_WAIT_KEY.formatted(queue), count)
                .flatMap(member -> reactiveRedisTemplate.opsForZSet().add(USER_QUEUE_PROCEED_KEY.formatted(queue), member.getValue(), Instant.now().getEpochSecond()))
                .count();
    }

    /**
     * 사용자 진입 가능 여부 조회
     *
     * 사용자가 진입 가능 대기열에 포함되어 있는지 확인합니다.
     *
     * Redis proceed queue에 사용자 ID가 존재하면 true를 반환하고,
     * 존재하지 않으면 false를 반환합니다.
     *
     * @param queue 대기열 이름입니다.
     * @param userId 진입 가능 여부를 확인할 사용자 ID입니다.
     * @return 사용자의 진입 가능 여부입니다.
     */
    public Mono<Boolean> isAllowed(final String queue, final Long userId) {
        return reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_PROCEED_KEY.formatted(queue), userId.toString())
                .defaultIfEmpty(-1L)
                .map(rank -> rank >= 0);
    }

    /**
     * 토큰 기반 사용자 진입 가능 여부 검증
     *
     * 서버에서 생성한 토큰과 클라이언트가 전달한 토큰을 비교하여
     * 사용자의 진입 가능 여부를 판단합니다.
     *
     * 토큰은 queue와 userId를 기반으로 생성됩니다.
     *
     * @param queue 대기열 이름입니다.
     * @param userId 검증할 사용자 ID입니다.
     * @param token 클라이언트가 전달한 진입 토큰입니다.
     * @return 토큰이 유효하면 true, 유효하지 않으면 false입니다.
     */
    public Mono<Boolean> isAllowedByToken(final String queue, final Long userId, final String token) {
        return this.generateToken(queue, userId)
                .filter(gen -> gen.equalsIgnoreCase(token))
                .map(i -> true)
                .defaultIfEmpty(false);
    }

    /**
     * 사용자 대기 순번 조회
     *
     * 지정한 대기열에서 사용자의 현재 순번을 조회합니다.
     *
     * Redis Sorted Set의 rank는 0부터 시작하므로,
     * 사용자에게 반환할 때는 1을 더해 1부터 시작하는 순번으로 변환합니다.
     *
     * 사용자가 대기열에 존재하지 않으면 -1을 반환합니다.
     *
     * @param queue 대기열 이름입니다.
     * @param userId 대기 순번을 조회할 사용자 ID입니다.
     * @return 사용자의 현재 대기 순번입니다.
     */
    public Mono<Long> getRank(final String queue, final Long userId) {
        return reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_WAIT_KEY.formatted(queue), userId.toString())
                .defaultIfEmpty(-1L)
                .map(rank -> rank >= 0 ? rank + 1 : rank);
    }

    /**
     * 사용자 진입 토큰 생성
     *
     * queue와 userId를 기반으로 SHA-256 해시 토큰을 생성합니다.
     *
     * 생성된 토큰은 사용자가 대기열을 통과했는지 확인하는 용도로 사용되며,
     * 클라이언트 쿠키에 저장될 수 있습니다.
     *
     * @param queue 대기열 이름입니다.
     * @param userId 토큰을 생성할 사용자 ID입니다.
     * @return SHA-256 기반의 사용자 진입 토큰입니다.
     */
    public Mono<String> generateToken(final String queue, final Long userId) {
        return Mono.just(generateSHA256Token(queue, userId));
    }

    /**
     * 대기열 자동 진입 허용 스케줄링 처리
     *
     * Redis에 존재하는 모든 wait queue를 스캔한 뒤,
     * 각 대기열마다 정해진 수만큼 사용자를 진입 가능 상태로 변경합니다.
     *
     * 현재는 대기열별 최대 100명의 사용자를 진입 허용 처리합니다.
     *
     * 이 메서드는 스케줄러에서 주기적으로 호출되는 것을 목적으로 합니다.
     */
    public void scheduleAllowUser() {
        log.info("called scheduling");

        var maxAllowUserCount = 100L;
        reactiveRedisTemplate.scan(ScanOptions.scanOptions()
                .match(USER_QUEUE_WAIT_KEY_FOR_SCAN)
                .count(100)
                .build())
                .map(key -> key.split(":")[2])
                .flatMap(queue -> allowUser(queue, maxAllowUserCount).map(allowed -> Tuples.of(queue, allowed)))
                .doOnNext(tuple -> log.info("Tried %d and allowed %d members of %s queue".formatted(maxAllowUserCount, tuple.getT2(), tuple.getT1())))
                .subscribe();
    }
}
