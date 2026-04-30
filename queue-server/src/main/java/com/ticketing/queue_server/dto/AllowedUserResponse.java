package com.ticketing.queue_server.dto;

/**
 * 사용자 진입 가능 여부 조회 API 응답 DTO
 *
 * 사용자가 현재 서비스 페이지에 진입할 수 있는 상태인지 반환합니다.
 *
 * @param allowed 진입 가능 여부입니다. true이면 진입 가능, false이면 대기 필요 상태입니다.
 */
public record AllowedUserResponse(Boolean allowed) {
}
