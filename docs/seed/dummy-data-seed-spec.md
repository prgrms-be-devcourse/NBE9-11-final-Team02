# 더미 데이터 시더 스펙 (최종)

> 운영(prod) DB에 **추가**로 쌓는 더미데이터 시더의 설계 문서.
> 목적은 (1) 발표/시연용 의미 있는 고정 데이터(픽스처)와 (2) 현실적 규모의 통계/조회 데이터(벌크) 확보.
> 본 문서는 실제 엔티티 코드를 검증한 결과를 반영한 확정본

---

## 0. 공통 원칙

- **날짜는 실행일(`LocalDate.now`) 기준 상대값**으로 생성한다.
  - "오늘"은 시더를 돌리는 날이며, 모집마감·경기일·정산대기를 거기서 상대적으로 계산한다.
  - 따라서 팀원이 각자 서버를 아무 날에 띄워도 데모가 동일하게 동작한다.
  - (랜덤 구조는 고정 시드로 재현되고 날짜만 실행일에 맞춰 이동)
- **데이터 기간: `[오늘 - 2개월, 오늘 + 1개월]`**
  - 과거 2개월: COMPLETED / CANCELLED / 정산완료 (운영 이력)
  - 미래 1개월: RECRUITING / CONFIRMED (현재 모집 중)
  - 정산 대기(J5) = 경기일이 **오늘**
- 시간대 Asia/Seoul / 모든 PK는 CHAR(36) UUID
- 플랫폼 수수료율 **7%(0.07)** — `application.yaml`의 `platform-fee-rate` 기본값과 일치
- **매칭(Match)이 마스터 데이터.** 매칭을 "여정(Journey) 단위"로 묶어 부속 데이터를 한 번에 정합하게 생성한다.
- 기존 팀원 더미데이터는 **삭제/수정 없이 보존, 추가만** 한다.
- **재현성**: 모든 랜덤은 고정 시드 `new Random(20260630L)`. 같은 입력이면 항상 같은 결과.
- **멱등성**:
  - 픽스처 → 고정 UUID 기반 `existsById`, 있으면 전체 skip
  - 벌크 → `seed_run` 마커 테이블 기록, 이미 돌았으면 통째로 skip (부분 재실행 미지원)
- **프로파일 분리**: `prod`(DB 접속정보) 위에 `seed-fixture` / `seed-bulk`를 **추가로** 켜야만 동작.
  켜지 않으면 시더 빈 자체가 등록되지 않아 절대 실행되지 않는다. (예: `SPRING_PROFILES_ACTIVE=prod,seed-bulk`)

---

## 1. 활용 목적

- 데이터의 목적은 **데모 + 현실적 API 검증**
- 부하는 **데이터 양이 아니라 k6의 VU(동시 요청) 증가**로 만든다. 이 데이터의 역할은 "빈 테이블이 아니라 현실적 크기의 테이블을 쿼리가 때리게 하는 것".
- 부하테스트 시 매칭 수만 옵션으로 늘린다: `bulkSeeder.generate(users=100, matches=155)`(데모 기본) / `matches=500`(부하 프로파일)

---

## 2. 벌크 데이터 (랜덤 분포)

> **매칭 155개는 벌크 전용**이며, 픽스처 매칭(4장: 4개 + 방장 Journey 10개 = 14개)은 별도. 실제 추가 매칭 ≈ 169개.

### 2.1 유저 / 매니저 / 시설
| 항목 | 수량 | 비고 |
|---|---|---|
| 일반 유저 | 100명 | 픽스처 6명 별도 |
| 시설 매니저 | 7명 | |
| 시설 | 10개 | 매니저 4명×1 + 3명×2 |

- 블랙리스트(`User.restricted=true`) 유저 3명 (`restrict(reason)` 메서드로 설정)
- 시설 상태: ACTIVE 다수, **CLOSED 2개** 포함

### 2.2 UserSportStat (실력) — 엔티티 검증 반영
추천·참가제한·실력변화가 모두 이 엔티티에 의존하므로 별도 명시한다.

- 유저당 **1~3개 종목**(`SportType`: FUTSAL/SOCCER/BASKETBALL/TENNIS/BADMINTON) 보유, 종목 쏠림 없이 분산
- 자기신고 레벨(`SelfReportedLevel`)은 **BEGINNER / INTERMEDIATE / ADVANCED 3종뿐**이며, 각각 초기 별점을 부여:
  - BEGINNER → skillRating **1.5**
  - INTERMEDIATE → **3.0**
  - ADVANCED → **4.5**
- 초기 분포: **BEGINNER 40% / INTERMEDIATE 45% / ADVANCED 15%**
- 레벨은 종목마다 독립 (같은 유저가 풋살 INTERMEDIATE, 농구 BEGINNER 가능)
- **별점(`skillRating`)은 0~5 연속값**이고, 매칭 참가 제한은 이 값과 매칭의 `SkillLevel`(LEVEL_1~5 = 정수 경계 1~5, ANY=무제한)을 비교한다.
  - 검증식: `매칭 minScore ≤ 유저 skillRating ≤ 매칭 maxScore`
- 리뷰의 실력 평가가 쌓이면 `UserSportStat.addSkillRating()` 내부의 이동평균/decay로 `skillRating`이 변동 → 추천·참가제한에 반영
- ⚠️ 시더는 점수를 직접 set하지 말고 **리뷰 생성 후 `addSkillRating()` 호출**로 파생값을 갱신한다

### 2.3 매칭 (총 155개)
| 상태 | 개수 |
|---|---|
| COMPLETED | 90 |
| RECRUITING | 35 |
| CANCELLED | 20 |
| CONFIRMED | 10 |
| **합계** | **155** |

### 2.4 리뷰 (`ParticipantReview`)
- **매칭당 참가자 1명이 같은 경기 참가자 중 0~2명에게 리뷰** (참가자 쌍 기반은 인원 제곱으로 폭증)
- 내용 비율: 매너+실력 둘 다 70% / 매너만 15% / 실력만 15%
  - 매너 평가 → `User.addMannerRating()` 호출, 실력 평가 → `UserSportStat.addSkillRating()` 호출
- 유저별 누적 분포: **콜드스타트 0건 30% / 혼합 1~9건 50% / 성숙 10건+ 20%**

### 2.5 매칭 생성 체인 (엔티티 제약)
`Match.reservationId`는 **NOT NULL + unique**. 따라서 매칭 1개마다 다음 체인이 반드시 함께 생성된다:

```
Facility → FacilitySlot → Reservation → Match → MatchParticipant → Payment(참가비) → (완료 시) Settlement
```

---

## 3. 여정(Journey) 배분 + Invariant

| Journey | 개수 | 상태 | 합계 검증 |
|---|---|---|---|
| J1 모집중(미달) | 20 | RECRUITING | |
| J2 마감임박(잔여1) | 15 | RECRUITING | 35 ✓ |
| J3 정원마감 | 10 | CONFIRMED | 10 ✓ |
| J4 완료+정산끝 | 85 | COMPLETED | |
| J5 완료+정산대기 | 5 | COMPLETED | 90 ✓ |
| J6 방장 자진취소 | 12 | CANCELLED | |
| J7 미달 자동취소 | 8 | CANCELLED | 20 ✓ |
| **합계** | **155** | | ✓ |

**Invariant (각 Journey가 반드시 만족 — 시더 검증식 = 테스트 코드)**

- **J3 (CONFIRMED)**: `currentCount == capacity` / 종료일시 > 현재 / 슬롯 RESERVED · 예약 CONFIRMED
- **J4 (완료+정산끝)**: 참가자 ≥ 2 / `matchDate < today` / 참가자 결제 PAID 존재 / **`Settlement.totalParticipantFee == Σ Payment(PARTICIPATION, PAID)`** / Settlement 존재 / 슬롯 RESERVED · 예약 COMPLETED
- **J5 (완료+정산대기)**: 참가자 ≥ 2 / **`matchDate == 실행일(LocalDate.now)`** / Settlement **없음** (정산 배치 쿼리가 `matchDate < today`만 처리하므로 오늘 경기는 아직 미정산. 시더를 돌린 그날 데모하면 정산 대기가 보이고, 다음 날 새벽 2시 배치가 정산함)
- **J6 (방장 취소)**: `status=CANCELLED` / `cancelledAt` 존재 / 슬롯·예약 CANCELLED / 참가자 결제 환불 처리
- **J7 (자동 취소)**: `status=CANCELLED` / `currentCount < capacity` / `recruitDeadline` 경과

---

## 4. 픽스처 데이터 (고정 UUID, 시연용)

### 4.1 유저 6명

> 닉네임은 실제 가입 닉네임(자연스러운 표기), 설계 라벨은 본 문서 내 역할 식별용 별칭이다.

| 닉네임 | 설계 라벨 | 역할 | 자기신고 | 초기 별점 | 리뷰 후 별점 | 스토리 |
|---|-------|---|---|---|---|---|
| 박매니저 | 매니저   | MANAGER | - | - | - | 1호점 보유(RESERVED 슬롯 有). 시연 중 2호점 라이브 개설 |
| 김방장 | 방장    | USER(헤비) | INTERMEDIATE | 3.0 | 3.0대 | 매칭 다수 생성(정산내역). **방장 전용 Journey 10개**로 4종목 COMPLETED 운동기록 |
| 이중수 | 중수    | USER | INTERMEDIATE | 3.0 | 3.0대 | 참가 성공 데모 |
| 최초보 | 초보    | USER(신규) | BEGINNER | 1.5 | 1.5대 | 실력 제한 참가 실패 데모 |
| 정성장 | 실력상승  | USER | INTERMEDIATE | 3.0 | **→ 4.2 상승** | 추천 API 테스트 (실력 ↑ 반영) |
| 한고수 | 실력하락  | USER(헤비) | ADVANCED | 4.5 | **→ 2.0 하락** (풋살 단일) | 자진취소 1건(참여이력+취소목록 데모) |

- 방장(김방장) 운동기록은 **픽스처 전용 Journey 10개**로 생성
- 다종목 부담은 방장(김방장)이 진다. 실력하락(한고수)은 풋살 단일 종목 별점 변화가 핵심.
- 정성장: INTERMEDIATE(3.0) 시작 → 실력 리뷰 10건+ 평균 ~4.2 → skillRating 4.2
- 한고수: ADVANCED(4.5) 시작 → 실력 리뷰 10건+ 평균 ~2.0 → skillRating 2.0.

### 4.2 매칭 4개 (모두 풋살, `SkillLevel`=별점 정수 경계)
| 이름 | 실력제한 | 정원 | 상태 |
|---|---|---|---|
| 강남 퇴근후풋살 | LEVEL_2~4 (별점 2.0~4.0) | 6 | RECRUITING (방장 생성, 매니저 1호점 슬롯 연결) |
| 마감임박 인기매칭 | ANY | 5 (잔여1) | RECRUITING (마감 24h 이내) |
| 모집마감된 매칭 | ANY | - | recruitDeadline 경과 |
| 정원가득찬 매칭 | ANY | 5 (현재5) | currentCount==capacity |

> "강남 퇴근후풋살"(2.0~4.0): 초보(1.5) **실패**, 중수·방장(3.0) 통과. 의도대로 동작.

### 4.3 시연 시나리오 8단계
1. 매니저 시설 관리 흐름 (1호점 기존 / 2호점 신규 생성)
2. 방장이 "강남 퇴근후풋살" 생성
3. 중수 참가 성공 (별점 3.0 ∈ [2,4])
4. 초보 참가 시도 → 실력 제한 실패 (별점 1.5 < 2.0)
5. "마감임박 인기매칭" 4/5 구성 노출 확인
6. 사람이 추천 API 조회 → 실력 상승(별점 4.2) 반영 확인
7. "모집마감된 매칭" 참가 시도 → 마감 실패
8. "정원가득찬 매칭" 참가 시도 → 정원 초과 실패

### 4.4 매니저 시설/슬롯 규칙 시연
- 1호점 슬롯 가격 수정 → **실패** (RESERVED 슬롯, `FacilitySlot.isManagerEditable()`은 AVAILABLE/CLOSED만 허용)
- 1호점 닫기 → **실패** (활성 예약 존재 → `FACILITY_HAS_ACTIVE_RESERVATIONS`)
- 2호점 라이브 생성 → **성공** (시더 미포함, 발표 당일 직접)

---

## 5. 코드로 확인된 비즈니스 규칙

1. **MatchCompletionScheduler**: 10분(`fixed-delay 600000ms`)마다, CONFIRMED & 종료일시 경과 매칭 → COMPLETED 전환
2. **SettlementScheduler**: 매일 02:00(Asia/Seoul), `matchDate < today`인 COMPLETED 미정산 매칭 → Settlement 생성
3. **SettlementProcessor**: `Settlement.totalParticipantFee = Σ Payment(PARTICIPATION, PAID)`, 수수료율은 정책 빈에서 주입(0.07)
4. **SettlementStatus**: `SETTLED` 하나뿐. "정산 대기"는 별도 상태가 아니라 **Settlement row 유무**로 구분 → J5는 `matchDate == today`로 재현
5. **FacilityService.deleteFacility()**: 하드삭제 아님, `facility.close()`(status=CLOSED) 소프트삭제. RESERVED/PENDING 슬롯 있으면 닫기 실패
6. **슬롯 가격 수정**: `FacilitySlot.update()`는 AVAILABLE/CLOSED 슬롯만 매니저 편집 허용
7. **파생 필드**: `User.mannerScore`, `UserSportStat.skillRating`, `Facility.ratingAvg`는 직접 set 불가 — 각각 `addMannerRating()` / `addSkillRating()` / `addRating()` 호출로만 갱신

---

## 6. 시더 코드 구조 설계

```
domain/seed/
├─ SeedRunner.java              ApplicationRunner + @Profile, 진입점
├─ SeedProperties.java          users=100, matches=155 파라미터
├─ marker/
│   ├─ SeedRun.java             @Entity seed_run (멱등성 마커)
│   └─ SeedRunRepository.java
├─ support/
│   ├─ SeedRandom.java          new Random(20260630L) 공유
│   ├─ SeedIds.java             픽스처 고정 UUID 상수
│   └─ JourneyFactory.java      슬롯~정산 한 묶음 조립 (Journey 단위 트랜잭션)
├─ fixture/
│   └─ FixtureSeeder.java       @Profile("seed-fixture"): 유저6 + 매칭4 + 방장 Journey10
└─ bulk/
    └─ BulkSeeder.java          @Profile("seed-bulk"): generate(users, matches), J1~J7 분배
```

**설계 결정**
1. **진입점**: `ApplicationRunner` + `@Profile`. 프로파일이 꺼져 있으면 빈 미등록 → 절대 실행 안 됨.
2. **멱등성**: 시작 시 `seed_run`의 `(type, version)` 존재 체크 → 있으면 return. 종료 시 마커 insert. 픽스처는 고정 UUID `existsById`로 이중 안전.
3. **트랜잭션 경계**: **Journey 1개 = 1 트랜잭션**. 중간 실패 시 해당 Journey만 롤백되어 정합 깨진 잔여 데이터가 안 남음.
4. **생성 순서**: 유저/매니저 → UserSportStat → 시설 → (Journey 루프: 슬롯→예약→매칭→참가자→결제→완료처리→리뷰→정산). 리뷰 직후 파생점수 갱신.
5. **파라미터화**: `BulkSeeder.generate(int users, int matches)` — matches만 155/500. Journey 비율은 matches에 비례 배분.
6. **고유 제약 회피**: email/nickname은 `seed_bulk_{i}@...`, `Payment.merchantUid`는 `SEED-{uuid}` 프리픽스로 기존 데이터와 충돌 회피.

### seed_run 마커 스키마
| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | CHAR(36) | PK |
| seed_type | VARCHAR | FIXTURE / BULK |
| seed_version | INT | 스펙 변경 시 증가 |
| executed_at | DATETIME | |

멱등성 체크 = `(seed_type, seed_version)` 존재 여부.
