package com.ticketing.queue_server.dto;

/**
 * 대기열 등록 API 응답 DTO
 *
 * 사용자가 대기열에 등록된 후 현재 대기 순번을 반환합니다.
 *
 * @param rank 등록된 사용자의 현재 대기 순번입니다.
 */
public record RegisterUserResponse(Long rank) {
}
