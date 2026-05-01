# 🎫 Ticketing

대규모 트래픽 상황에서 시스템 다운을 방지하고, 공정한 순차 처리를 보장하기 위한 **Redis 기반 대기열 시스템** 학습 프로젝트입니다.

## 🚀 Project Overview
사용자가 서비스에 직접 진입하는 대신, Redis 대기열을 거쳐 순차적으로 입장하도록 제어합니다. 티켓팅, 수강 신청, 선착순 이벤트 등 순간적인 트래픽 폭주가 예상되는 시나리오를 해결하는 데 초점을 맞췄습니다.

## 🛠 Tech Stack
- **Language:** Java 25
- **Framework:** Spring Boot 4.0.6
- **Reactive:** Spring WebFlux, Spring Data Redis Reactive (queue-server)
- **Web:** Spring Web MVC, Thymeleaf (website)
- **Database:** Redis
- **Test:** JUnit 5, Reactor Test, Embedded Redis

## 📂 Project Structure
Gradle 멀티 모듈 구조를 사용하여 관심사를 분리했습니다.
- **`common`**: 공통 DTO, 예외 클래스 및 공통 유틸리티
- **`queue-server`**: [WebFlux] 대기열 등록, 순번 조회, 진입 허용 처리 및 스케줄러 핵심 로직
- **`website`**: [MVC] 실제 서비스가 제공되는 웹 애플리케이션 (대기열 통과자 전용)

## 🔄 System Flow
1. **진입 시도**: 사용자가 `website` 접속 시 쿠키의 대기열 토큰 확인
2. **대기실 리다이렉트**: 유효한 토큰이 없을 경우 `queue-server`의 대기 페이지로 이동
3. **순번 폴링**: 대기실에서 본인의 순번을 주기적으로 조회 (Redis Sorted Set 기반)
4. **자동 진입 처리**: `queue-server` 내부 스케줄러가 설정된 인원만큼 순차적으로 진입 허용 상태로 변경
5. **토큰 발급**: 진입이 허용되면 토큰을 발급받아 쿠키에 저장 후 원래 `website`로 복귀

## 💾 Redis Data Structure
Redis의 **Sorted Set**을 활용하여 선착순(FIFO) 구조를 구현했습니다.
- **Wait Queue**: `users:queue:{name}:wait`
  - `score`: 등록 시각(Timestamp) / `member`: 사용자 식별자
- **Proceed Queue**: `users:queue:{name}:proceed`
  - 진입이 허용된 사용자 정보 (토큰 검증 시 활용)

## ✨ Key Features
- **비동기 처리**: WebFlux와 Reactive Redis를 사용하여 대용량 요청을 효율적으로 처리
- **자동 스케줄링**: 운영자의 개입 없이 설정에 따라 자동으로 대기 인원 진입 처리
- **중복 방지**: 동일 사용자의 중복 대기열 등록 차단
- **격리된 구조**: 대기열 로직과 실제 서비스 로직을 모듈로 분리하여 유연성 확보