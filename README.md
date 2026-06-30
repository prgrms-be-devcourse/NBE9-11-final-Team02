# SportTeam

스포츠 시설 예약, 팀 매칭, 참가비 결제와 환불, 실시간 알림을 하나의 흐름으로 연결하는 생활 스포츠 매칭 플랫폼입니다.

---

## 프로젝트 소개

- 사용자는 예약 가능한 스포츠 시설을 조회하고 원하는 시간대의 매칭방에 참가할 수 있습니다.
- 방장은 시설을 선점하고 매칭방을 생성하여 팀원을 모집할 수 있습니다.
- 참가자는 TossPayments 기반 결제를 통해 참가비를 결제하고, 서버는 금액 위변조를 방지하기 위해 결제 전 금액을 재검증합니다.
- 모집 마감 시 정원 충족 여부에 따라 매칭 확정 또는 자동 취소 및 환불 큐 발행이 수행됩니다.
- Kafka와 SSE를 통해 모집 완료, 매칭 취소, 경기 전 리마인드 알림을 비동기로 전달합니다.
- 시설 관리자와 관리자는 시설, 예약, 정산, 사용자 제한 상태를 관리할 수 있습니다.

---

## 프로젝트 개요

생활 스포츠를 하려면 시설 예약, 팀원 모집, 참가비 정산을 여러 서비스에서 따로 처리해야 하는 불편함이 있습니다.

SportTeam은 다음 흐름을 하나의 백엔드 시스템으로 연결합니다.

```text
시설 조회
  -> 시설 슬롯 선점
  -> 매칭방 생성
  -> 참가 신청
  -> 참가비 결제
  -> 모집 마감 확정/취소
  -> 알림 및 환불 처리
```

주요 목표는 다음과 같습니다.

- 시설 예약과 매칭 모집을 하나의 도메인 흐름으로 통합
- Redisson 기반 분산 락으로 인기 시간대 동시성 제어
- TossPayments 연동을 통한 결제 승인, 웹훅 멱등 처리, 환불 처리
- Redis 대기열을 통한 진입 순서 제어
- Kafka + SSE 기반 실시간 알림
- Blue/Green 배포와 GitHub Actions 기반 CI/CD 구성

---

## 기술 스택

### Backend

![Java](https://img.shields.io/badge/Java-24-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-6DB33F?logo=springboot&logoColor=white)
![Spring Web](https://img.shields.io/badge/Spring%20Web-6DB33F?logo=spring)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?logo=spring)
![Spring Batch](https://img.shields.io/badge/Spring%20Batch-6DB33F?logo=spring)
![Gradle](https://img.shields.io/badge/Gradle-Kotlin%20DSL-02303A?logo=gradle)

### Security

![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?logo=springsecurity&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?logo=jsonwebtokens&logoColor=white)
![Validation](https://img.shields.io/badge/Jakarta%20Validation-FF6F00)

### Database / Cache / Messaging

![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7.4-DC382D?logo=redis&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.7.0-231F20?logo=apachekafka&logoColor=white)
![H2](https://img.shields.io/badge/H2-Test%20Database-blue)

### Query / Lock / Docs

![QueryDSL](https://img.shields.io/badge/QueryDSL-0769AD)
![Redisson](https://img.shields.io/badge/Redisson-Redis%20Lock-DC382D)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?logo=swagger&logoColor=black)

### Infra / Monitoring

![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)
![Docker Compose](https://img.shields.io/badge/Docker%20Compose-2496ED?logo=docker&logoColor=white)
![AWS EC2](https://img.shields.io/badge/AWS%20EC2-FF9900?logo=amazonec2&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?logo=githubactions&logoColor=white)
![SonarCloud](https://img.shields.io/badge/SonarCloud-F3702A?logo=sonarcloud&logoColor=white)
![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?logo=prometheus&logoColor=white)
![Grafana](https://img.shields.io/badge/Grafana-F46800?logo=grafana&logoColor=white)

---

## 아키텍처

```text
Client / Frontend
  |
  | REST API / SSE / WebSocket
  v
Nginx
  |
  v
Spring Boot API Server
  |
  |-- Auth / User
  |     |-- 회원가입 / 로그인 / JWT 재발급 / 로그아웃
