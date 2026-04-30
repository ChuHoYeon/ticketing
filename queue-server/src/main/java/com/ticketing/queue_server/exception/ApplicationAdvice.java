package com.ticketing.queue_server.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

/**
 * 애플리케이션 전역 예외 처리 클래스
 *
 * 컨트롤러에서 발생하는 사용자 정의 예외를 공통 응답 형식으로 변환합니다.
 */
@RestControllerAdvice
public class ApplicationAdvice {

    /**
     * 사용자 정의 애플리케이션 예외 처리
     *
     * ApplicationException이 발생하면 예외에 포함된 HTTP 상태 코드,
     * 에러 코드, 에러 사유를 사용하여 클라이언트에 응답합니다.
     *
     * @param ex 처리할 사용자 정의 애플리케이션 예외입니다.
     * @return 에러 코드와 에러 사유를 포함한 HTTP 응답입니다.
     */
    @ExceptionHandler(value = {ApplicationException.class})
    Mono<ResponseEntity<ServerExceptionResponse>> applicationExceptionHandler(ApplicationException ex) {
        return Mono.just(ResponseEntity
                .status(ex.getHttpStatus())
                .body(new ServerExceptionResponse(ex.getCode(), ex.getReason()))
        );
    }

    /**
     * 서버 예외 응답 DTO
     *
     * API 요청 처리 중 발생한 예외 정보를 클라이언트에 전달하기 위한 응답 객체입니다.
     *
     * @param code 애플리케이션에서 정의한 에러 코드입니다.
     * @param reason 에러 발생 사유입니다.
     */
    public record ServerExceptionResponse(String code, String reason) {}
}
