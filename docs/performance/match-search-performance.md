## 매칭 목록 조회 성능 개선

### 1. 테스트 목적

매칭 목록 조회는 사용자가 현재 참여 가능한 방을 탐색하는 기능이다.
따라서 가장 빈번한 조회 조건은 `status = RECRUITING`이라고 판단했다.

또한 사용자는 전체 매칭 목록보다는 관심 있는 종목을 기준으로 탐색할 가능성이 높기 때문에
`sport_type`을 두 번째 조건으로 배치했다.

마지막으로 모집 마감이 임박한 방을 우선 노출하는 조회 패턴을 고려해
`recruit_deadline`, `id`를 정렬 컬럼으로 포함했다.

### 2. 대표 조회 쿼리

```sql
SELECT
    id,
    title,
    sport_type,
    current_count,
    capacity,
    fee_per_person,
    min_skill_level,
    max_skill_level,
    required_gender,
    recruit_deadline,
    status
FROM matches
WHERE status = 'RECRUITING'
  AND sport_type = 'FUTSAL'
ORDER BY recruit_deadline ASC, id ASC
LIMIT 20 OFFSET 0;
```

### 3. 복합 인덱스 설계

```sql
CREATE INDEX idx_matches_status_sport_deadline
ON matches (status, sport_type, recruit_deadline, id);
```

- `status`, `sport_type`은 동등 조건이므로 인덱스 앞쪽에 배치해 검색 범위를 먼저 줄인다.
- `recruit_deadline`, `id`는 정렬 조건이므로 동등 조건 뒤에 배치해 별도 정렬 비용을 줄인다.
- `id`는 동일한 `recruit_deadline`을 가진 데이터 사이에서 안정적인 정렬 순서를 보장한다.
- 모든 정렬 조건에 인덱스를 만들면 쓰기 비용과 저장 공간이 증가하므로, 우선 대표 조회 패턴인 "모집중 + 종목 + 마감임박순"에 맞춰 설계했다.

### 4. 테스트 데이터

- `matches` 테이블 기준 누적 매칭방 10,000건 생성
- 상태 분포
  - `RECRUITING`: 2,500건
  - `CONFIRMED`: 2,500건
  - `COMPLETED`: 2,500건
  - `CANCELLED`: 2,500건
- 종목 분포
  - `FUTSAL`: 2,000건
  - `SOCCER`: 2,000건
  - `TENNIS`: 2,000건
  - `BASKETBALL`: 2,000건
  - `BADMINTON`: 2,000건

### 5. EXPLAIN ANALYZE 결과

#### 인덱스 적용 전

```text
-> Limit: 20 row(s) (actual time=10..10 rows=20 loops=1)
    -> Sort: matches.recruit_deadline, matches.id (actual time=10..10 rows=20 loops=1)
        -> Filter: ((matches.sport_type = 'FUTSAL') and (matches.status = 'RECRUITING')) (actual time=0.234..9.7 rows=334 loops=1)
            -> Table scan on matches (actual time=0.194..8.53 rows=10000 loops=1)
```

#### 인덱스 적용 후

```text
-> Limit: 20 row(s) (actual time=1.36..1.36 rows=20 loops=1)
    -> Index lookup on matches using idx_matches_status_sport_deadline
       (status='RECRUITING', sport_type='FUTSAL')
       (actual time=1.33..1.33 rows=20 loops=1)
```

### 6. 성능 비교

| 항목 | 인덱스 적용 전 | 인덱스 적용 후 |
|---|---:|---:|
| 접근 방식 | Table Scan | Index Lookup |
| 사용 인덱스 | 없음 | idx_matches_status_sport_deadline |
| 실제 읽은 row | 10,000건 | 20건 |
| 조건 후보 row | 334건 | 334건 |
| 정렬 단계 | Sort 발생 | Sort 제거 |
| 반환 row | 20건 | 20건 |
| 실행 시간 | 약 10ms | 약 1.36ms |
| 개선율 | - | 약 86.4% 감소 |

### 7. 결과

인덱스 적용 전에는 `matches` 테이블 전체를 스캔한 뒤 조건 필터링과 정렬을 수행했다.

인덱스 적용 후에는 `status`, `sport_type` 조건으로 인덱스를 탐색하고,
`recruit_deadline`, `id` 순서대로 데이터를 읽어 별도 정렬 단계가 제거되었다.

그 결과 실행 시간은 약 10ms에서 약 1.36ms로 감소했다.
