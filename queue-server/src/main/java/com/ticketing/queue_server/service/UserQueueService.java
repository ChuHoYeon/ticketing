package com.ticketing.queue_server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

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
     * 대기열 등록 API
     * Redis SortedSet 사용
     * key: user-queue | value: unix timestamp
     * @param String queue
     * @param Long userId
     * @return Long rank
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
     * 진입 허용 API
     * 1. wait queue 에서 사용자를 제거
     * 2. proceed queue 에 사용자를 추가
     * @param String queue
     * @param Long count
     * @return Long count
     */
    public Mono<Long> allowUser(final String queue, final Long count) {
        return reactiveRedisTemplate.opsForZSet().popMin(USER_QUEUE_WAIT_KEY.formatted(queue), count)
                .flatMap(member -> reactiveRedisTemplate.opsForZSet().add(USER_QUEUE_PROCEED_KEY.formatted(queue), member.getValue(), Instant.now().getEpochSecond()))
                .count();
    }

    /**
     * 진입 가능 조회 API
     * 진입이 가능한 상태인지 조회
     * @param String queue
     * @param Long userId
     * @return Boolean isAllowed ( true = 대기열을 통과 할 수 있음 | false = 대기열 대기 해야함 )
     */
    public Mono<Boolean> isAllowed(final String queue, final Long userId) {
        return reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_PROCEED_KEY.formatted(queue), userId.toString())
                .defaultIfEmpty(-1L)
                .map(rank -> rank >= 0);
    }

    public Mono<Boolean> isAllowedByToken(final String queue, final Long userId, final String token) {
        return this.generateToken(queue, userId)
                .filter(gen -> gen.equalsIgnoreCase(token))
                .map(i -> true)
                .defaultIfEmpty(false);
    }

    public Mono<Long> getRank(final String queue, final Long userId) {
        return reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_WAIT_KEY.formatted(queue), userId.toString())
                .defaultIfEmpty(-1L)
                .map(rank -> rank >= 0 ? rank + 1 : rank);
    }

    public Mono<String> generateToken(final String queue, final Long userId) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            var input = "user-queue-%s-%d".formatted(queue, userId);
            byte[] encodeHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte aByte : encodeHash) {
                hexString.append(String.format("%02x", aByte));
            }
            return Mono.just(hexString.toString());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }

    public void scheduleAllowUser() {
        log.info("called scheduling");

        var maxAllowUserCount = 3L;
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
