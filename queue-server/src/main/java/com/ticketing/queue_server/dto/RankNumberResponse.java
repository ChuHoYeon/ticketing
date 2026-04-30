package com.ticketing.queue_server.dto;

/**
 * 사용자 대기 순번 조회 API 응답 DTO
 *
 * 사용자의 현재 대기열 순번을 반환합니다.
 *
 * @param rank 사용자의 현재 대기 순번입니다. 대기열에 존재하지 않는 경우 -1이 반환될 수 있습니다.
 */
public record RankNumberResponse(Long rank) {
}
