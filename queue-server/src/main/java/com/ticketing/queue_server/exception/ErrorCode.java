package com.ticketing.queue_server.exception;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 에러 코드 정의 enum
 *
 * 서비스에서 발생할 수 있는 비즈니스 예외를 코드화하여 관리합니다.
 * 각 에러 코드는 HTTP 상태 코드, 애플리케이션 에러 코드, 에러 메시지를 포함합니다.
 */
@AllArgsConstructor
public enum ErrorCode {

    /**
     * 이미 대기열에 등록된 사용자가 다시 등록을 요청한 경우 발생하는 에러입니다.
     */
    QUEUE_ALREADY_REGISTERED_USER(HttpStatus.CONFLICT, "UQ-0001", "Already registered in queue");

    private final HttpStatus httpStatus;
    private final String code;
    private final String reason;

    /**
     * 현재 ErrorCode 정보를 기반으로 ApplicationException을 생성합니다.
     *
     * @return 현재 에러 코드 정보를 포함한 ApplicationException 객체입니다.
     */
    public ApplicationException build() {
        return new ApplicationException(httpStatus, code, reason);
    }

    /**
     * 현재 ErrorCode 정보를 기반으로 ApplicationException을 생성합니다.
     *
     * reason 문자열에 포맷 인자가 필요한 경우 전달받은 args를 사용하여
     * 에러 메시지를 완성합니다.
     *
     * @param args 에러 메시지 포맷에 사용할 인자입니다.
     * @return 포맷팅된 에러 사유를 포함한 ApplicationException 객체입니다.
     */
    public ApplicationException build(Object ...args) {
        return new ApplicationException(httpStatus, code, reason.formatted(args));
    }
}
