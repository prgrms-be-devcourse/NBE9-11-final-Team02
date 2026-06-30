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
  |     |-- 내 프로필 / 운동 통계 / 회원 탈퇴
  |
  |-- Facility / Reservation
  |     |-- 시설 조회 / 슬롯 조회
  |     |-- 시설 관리자 시설 관리
  |     |-- 예약 상태 관리
  |
  |-- Match
  |     |-- 매칭방 생성 / 조회 / 추천
  |     |-- 참가 신청 / 참가 취소
  |     |-- 방장 수동 확정 / 방장 매칭 취소
  |
  |-- Payment / Refund
  |     |-- 결제 사전 검증
  |     |-- TossPayments 승인
  |     |-- TossPayments 웹훅 멱등 처리
  |     |-- 환불 큐 생성 및 환불 처리
  |
  |-- Queue / Notification
  |     |-- Redis 대기열 토큰
  |     |-- Kafka 알림 이벤트
  |     |-- SSE 실시간 알림
  |
  |-- Review / MyPage / Settlement / Admin
        |-- 후기 작성
        |-- 내 기록 / 내 매칭 / 결제 내역
        |-- 정산 집계
        |-- 관리자 시설/회원/제한 관리

External / Infra
  |-- MySQL
  |-- Redis
  |-- Kafka
  |-- TossPayments
  |-- AWS S3
  |-- GitHub Actions -> GHCR -> AWS EC2 Blue/Green Deploy
```

---

## 프로젝트 구조

```text
src/
├─ main/
│  ├─ java/com/back/sportteam/
│  │  ├─ SportTeamApplication.java
│  │  ├─ batch/
│  │  │  ├─ cancel/          # 모집 마감 확정/취소 스케줄러
│  │  │  ├─ completion/      # 경기 종료 처리
│  │  │  ├─ notification/    # 경기 전 리마인드 알림
│  │  │  ├─ payment/         # PENDING 결제 만료 처리
│  │  │  ├─ refund/          # 환불 처리 스케줄러
│  │  │  └─ settlement/      # 정산 배치
│  │  ├─ domain/
│  │  │  ├─ admin/           # 관리자 기능
│  │  │  ├─ auth/            # 인증 / JWT
│  │  │  ├─ facility/        # 시설 / 슬롯
│  │  │  ├─ match/           # 매칭방 / 참가자
│  │  │  ├─ mypage/          # 마이페이지
│  │  │  ├─ notification/    # 알림 / SSE
│  │  │  ├─ payment/         # 결제 / 웹훅 / 환불
│  │  │  ├─ reservation/     # 예약
│  │  │  ├─ review/          # 후기
│  │  │  ├─ settlement/      # 정산
│  │  │  ├─ system/          # 헬스체크 / 대기열
│  │  │  └─ user/            # 사용자 / 프로필
│  │  ├─ global/
│  │  │  ├─ config/
│  │  │  ├─ exception/
│  │  │  ├─ lock/
│  │  │  ├─ response/
│  │  │  └─ util/
│  │  └─ infra/
│  │     ├─ kafka/
│  │     ├─ payment/toss/
│  │     ├─ redis/
│  │     └─ s3/
│  └─ resources/
│     ├─ application.yaml
│     ├─ application-dev.yaml
│     ├─ application-prod.yaml
│     └─ application-test.yaml
└─ test/
   └─ java/com/back/sportteam/
```

---

## 주요 기능

### Auth / User

- 일반 회원가입
- 로그인 및 JWT 발급
- Access Token 재발급
- 로그아웃
- 내 프로필 조회 및 수정
- 선호 종목 / 실력 / 매너 점수 기반 프로필 관리
- 회원 탈퇴

### Facility / Reservation

- 예약 가능 시설 목록 조회
- 시설 상세 조회
- 시설별 슬롯 조회
- 시설 관리자 시설 CRUD
- 시설 이미지 삭제
- 시설 운영 시간 및 슬롯 설정
- 관리자용 예약 현황 조회

### Match

- 시설 선점 기반 매칭방 생성
- QueryDSL 기반 모집 중 매칭 목록 조회
- 사용자 조건 기반 매칭 추천
- 매칭 상세 조회
- 참가자 목록 조회
- 매칭방 참가 신청
- 참가자 중도 이탈
- 방장 수동 확정
- 방장 매칭 취소
- 모집 마감 시 자동 확정/취소
- 경기 종료 처리

### Payment / Refund

- 결제 사전 검증 주문서 생성
- TossPayments 결제 승인
- TossPayments 웹훅 수신
- 웹훅 HMAC 서명 검증
- 웹훅 이벤트 멱등 처리
- 결제 상태 PENDING -> PAID / FAILED 전이
- 승인 시각 기반 참가 상태 처리
- PENDING 결제 만료 보정
- 매칭 취소 및 모집 미달 시 환불 큐 생성
- TossPayments 환불 처리

### Queue / Notification

- Redis 기반 예약 대기열 토큰 발급
- 대기열 순번 조회
- 입장 가능 토큰 소비
- Kafka 기반 매칭 알림 이벤트 발행/소비
- SSE 기반 실시간 알림 구독
- 알림 목록 조회
- 알림 읽음 처리
- 모집 완료 / 매칭 취소 / 경기 전 리마인드 알림

### Review / MyPage / Settlement / Admin

- 경기 종료 후 참가자 및 시설 리뷰 작성
- 시설 리뷰 조회
- 내 시설 리뷰 조회
- 내 운동 기록 조회
- 내 참여 매칭 이력 조회
- 내 매칭 결제 내역 조회
- 정산 요약 및 상세 조회
- 관리자 시설 목록 조회
- 관리자 회원 목록 조회
- 블랙리스트 및 제한 대상 조회
- 사용자 이용 제한 설정

---

## API 문서

Swagger UI:

```text
Local: http://localhost:8090/swagger-ui/index.html
Production: http://3.36.243.212/swagger-ui/index.html
```

### Auth API

| 기능 | Method | URL |
|---|---|---|
| 회원가입 | POST | `/api/v1/auth/signup` |
| 로그인 | POST | `/api/v1/auth/login` |
| 토큰 재발급 | POST | `/api/v1/auth/refresh` |
| 로그아웃 | POST | `/api/v1/auth/logout` |

### User API

| 기능 | Method | URL |
|---|---|---|
| 내 프로필 조회 | GET | `/api/v1/users/me` |
| 내 프로필 수정 | PATCH | `/api/v1/users/me` |
| 내 종목 통계 조회 | GET | `/api/v1/users/me/sport-stats` |
| 내 종목 통계 등록 | POST | `/api/v1/users/me/sport-stats` |
| 회원 탈퇴 | DELETE | `/api/v1/users/me` |

### Facility API

| 기능 | Method | URL |
|---|---|---|
| 예약 가능 시설 조회 | GET | `/api/v1/facilities/available` |
| 시설 상세 조회 | GET | `/api/v1/facilities/{facilityId}` |
| 시설 슬롯 조회 | GET | `/api/v1/facilities/{facilityId}/slots` |
| 시설 리뷰 조회 | GET | `/api/v1/facilities/{facilityId}/reviews` |

### Facility Manager API

| 기능 | Method | URL |
|---|---|---|
| 내 시설 목록 조회 | GET | `/api/v1/manager/facilities` |
| 시설 예약 현황 조회 | GET | `/api/v1/manager/facilities/{facilityId}/reservations` |
| 시설 생성 | POST | `/api/v1/manager/facilities` |
| 시설 수정 | PATCH | `/api/v1/manager/facilities/{facilityId}` |
| 시설 삭제 | DELETE | `/api/v1/manager/facilities/{facilityId}` |
| 시설 이미지 삭제 | DELETE | `/api/v1/manager/facilities/{facilityId}/images` |
| 시설 슬롯 설정 | POST | `/api/v1/manager/facilities/{facilityId}/slots` |
| 시설 슬롯 수정 | PATCH | `/api/v1/manager/facilities/{facilityId}/slots/{slotId}` |

### Match API

| 기능 | Method | URL |
|---|---|---|
| 매칭방 생성 | POST | `/api/v1/matches` |
| 매칭 목록 조회 | GET | `/api/v1/matches` |
| 매칭 추천 목록 조회 | GET | `/api/v1/matches/recommendations` |
| 매칭 상세 조회 | GET | `/api/v1/matches/{matchId}` |
| 참가자 목록 조회 | GET | `/api/v1/matches/{matchId}/participants` |
| 매칭 참가 신청 | POST | `/api/v1/matches/{matchId}/participants` |
| 내 참가 취소 | DELETE | `/api/v1/matches/{matchId}/participants/me` |
| 방장 수동 확정 | PATCH | `/api/v1/matches/{matchId}/confirm` |
| 방장 매칭 취소 | DELETE | `/api/v1/matches/{matchId}` |

### Payment API

| 기능 | Method | URL |
|---|---|---|
| 결제 사전 검증 | POST | `/api/v1/payments/prepare` |
| TossPayments 결제 승인 | POST | `/api/v1/payments/confirm` |
| TossPayments 웹훅 수신 | POST | `/api/v1/payments/webhook` |
| TossPayments 웹훅 수신 | POST | `/api/v1/payments/webhook/tosspayments` |

### Queue API

| 기능 | Method | URL |
|---|---|---|
| 대기열 토큰 발급 | POST | `/api/v1/queue/facility-slots/{facilitySlotId}/tokens` |
| 대기열 토큰 상태 조회 | GET | `/api/v1/queue/tokens/{token}` |
| 대기열 토큰 소비 | POST | `/api/v1/queue/tokens/{token}/consume` |

### Notification API

| 기능 | Method | URL |
|---|---|---|
| 알림 목록 조회 | GET | `/api/v1/notifications` |
| SSE 알림 구독 | GET | `/api/v1/notifications/subscribe` |
| 알림 읽음 처리 | PATCH | `/api/v1/notifications/{notificationId}/read` |

### Review API

| 기능 | Method | URL |
|---|---|---|
| 경기 리뷰 작성 | POST | `/api/v1/matches/{matchId}/reviews` |
| 내 시설 리뷰 조회 | GET | `/api/v1/users/me/reviews/facilities` |

### MyPage API

| 기능 | Method | URL |
|---|---|---|
| 내 운동 기록 조회 | GET | `/api/v1/users/me/records` |
| 내 참여 매칭 조회 | GET | `/api/v1/users/me/matches` |
| 내 매칭 결제 내역 조회 | GET | `/api/v1/users/me/matches/{matchId}/payment` |

### Admin API

| 기능 | Method | URL |
|---|---|---|
| 플랫폼 시설 목록 조회 | GET | `/api/v1/admin/facilities` |
| 사용자 목록 조회 | GET | `/api/v1/admin/users` |
| 블랙리스트 조회 | GET | `/api/v1/admin/users/blacklist` |
| 블랙리스트 후보 조회 | GET | `/api/v1/admin/users/blacklist/candidates` |
| 사용자 이용 제한 설정 | PATCH | `/api/v1/admin/users/{userId}/restriction` |
| 정산 요약 조회 | GET | `/api/v1/admin/settlements/summary` |
| 정산 상세 조회 | GET | `/api/v1/admin/settlements` |

### System / Infra API

| 기능 | Method | URL |
|---|---|---|
| 헬스체크 | GET | `/api/v1/health` |
| S3 Presigned URL 발급 | GET | `/api/v1/s3/presigned-url` |

---

## 배치 / 스케줄러

| 배치 | 역할 |
|---|---|
| MatchDeadlineScheduler | 모집 마감 매칭을 확정 또는 취소 처리 |
| MatchCompletionScheduler | 경기 종료 시 매칭 완료 처리 |
| MatchReminderScheduler | 경기 전 리마인드 알림 발행 |
| PaymentPendingExpirationScheduler | 오래된 PENDING 결제 상태 보정 |
| RefundScheduler | 환불 큐 처리 및 TossPayments 환불 요청 |
| SettlementScheduler | 정산 대상 집계 및 정산 상태 처리 |

---

## ERD

주요 도메인 관계:

```text
users
  |-- matches.host_id
  |-- match_participants.user_id
  |-- payments.user_id
  |-- notifications.user_id

facilities
  |-- facility_slots
       |-- reservations
            |-- matches

matches
  |-- match_participants
  |-- payments
  |-- participant_reviews

payments
  |-- refunds
  |-- payment_webhook_events
  |-- settlements
```

---

## 로컬 실행

### 1. 인프라 실행

```bash
docker compose up -d
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 3. 헬스체크

```text
http://localhost:8090/api/v1/health
```

---

## 테스트

```bash
./gradlew clean test
```

SonarCloud 분석 포함:

```bash
./gradlew clean test sonar --no-daemon
```

주요 테스트 범위:

- Auth / JWT
- User / MyPage
- Facility / Reservation
- Match / Participant
- Payment / Webhook / Refund
- Redis Waiting Queue
- Kafka Notification / SSE
- Scheduler / Batch
- Settlement
- Admin
- Global Exception

---

## 성능 테스트

K6 스크립트:

```text
k6/match-join-test.js
k6/facility-cache-test.js
k6/availability-test.js
```

예시:

```bash
docker run --rm -v "${PWD}/k6:/scripts" grafana/k6 run /scripts/availability-test.js
```

---

## CI/CD 및 배포

```text
develop / main push
  -> GitHub Actions
  -> Gradle Test / SonarCloud
  -> Docker Image Build
  -> GHCR Push
  -> AWS SSM Parameter Store Secret Upload
  -> AWS EC2 Deploy Command
  -> Blue/Green Container Switch
  -> Nginx Reverse Proxy
```

배포 구성:

- AWS EC2
- Docker / Docker Compose
- Nginx
- MySQL 8.4
- Redis 7.4
- Apache Kafka 3.7.0
- GHCR
- AWS SSM Parameter Store

---

## 모니터링

```text
Prometheus -> Spring Boot Actuator Metrics
Grafana    -> Dashboard Visualization
Loki       -> Log Aggregation
Promtail   -> Container Log Shipping
```

관련 문서:

```text
docs/monitoring/prometheus-grafana-guide.md
```

---

## Commit Message Convention

| type | description |
|:-:|---|
| feat | 새로운 기능 추가 |
| fix | 버그 수정 |
| docs | 문서 수정 |
| style | 코드 포맷팅, 세미콜론 누락 등 코드 변경이 없는 작업 |
| refactor | 코드 리팩토링 |
| test | 테스트 코드 추가 또는 수정 |
| chore | 빌드 설정, 패키지 매니저 설정 등 기타 작업 |

---

## 팀원 및 담당 영역

| 이름 | 담당 영역 |
|---|---|
| 차우호 | 매칭 생성, 참가, 확정, 취소, 매칭 조회 |
| 박현준 | 결제, 웹훅, 환불, 대기열, 알림, 헬스체크 |
| 오상민 | 인증, 회원, 실시간 동기화, 시설 조회 |
| 김은영 | 시설 관리, 리뷰, 마이페이지, 정산, 관리자 기능 |
