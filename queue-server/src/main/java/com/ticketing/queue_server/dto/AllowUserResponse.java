package com.ticketing.queue_server.dto;

/**
 * 대기열 사용자 진입 허용 API 응답 DTO
 *
 * 요청한 진입 허용 사용자 수와 실제로 진입 허용 처리된 사용자 수를 반환합니다.
 *
 * @param requestCount 클라이언트가 요청한 진입 허용 사용자 수입니다.
 * @param allowedCount 실제로 대기열에서 진입 허용 처리된 사용자 수입니다.
 */
public record AllowUserResponse(Long requestCount, Long allowedCount) {
}
