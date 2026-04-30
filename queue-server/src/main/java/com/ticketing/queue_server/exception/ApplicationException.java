package com.ticketing.queue_server.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 사용자 정의 예외 클래스
 *
 * 서비스 로직에서 발생하는 비즈니스 예외를 표현하기 위해 사용합니다.
 * HTTP 상태 코드, 애플리케이션 에러 코드, 에러 사유를 함께 보관합니다.
 */
@AllArgsConstructor
@Getter
public class ApplicationException extends RuntimeException{

    /**
     * 클라이언트에 반환할 HTTP 상태 코드입니다.
     */
    private HttpStatus httpStatus;

    /**
     * 애플리케이션에서 정의한 에러 코드입니다.
     */
    private String code;

    /**
     * 에러 발생 사유입니다.
     */
    private String reason;
}
