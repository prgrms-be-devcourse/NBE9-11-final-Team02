-- 매칭 목록 조회 성능 테스트용 더미 데이터
-- 실행 전제: matches.reservation_id는 UNIQUE이므로 각 row마다 UUID를 생성한다.
-- 기본 생성 건수: 10,000건

INSERT INTO matches (
    id,
    reservation_id,
    host_id,
    title,
    sport_type,
    capacity,
    current_count,
    fee_per_person,
    min_skill_level,
    max_skill_level,
    required_gender,
    recruit_deadline,
    cancel_deadline,
    confirmed_at,
    cancelled_at,
    status,
    created_at,
    updated_at
)
WITH RECURSIVE numbers AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1
    FROM numbers
    WHERE n < 10000
)
SELECT
    UUID(),
    UUID(),
    UUID(),
    CONCAT('성능 테스트 매칭 ', n),
    CASE n % 5
        WHEN 0 THEN 'FUTSAL'
        WHEN 1 THEN 'SOCCER'
        WHEN 2 THEN 'TENNIS'
        WHEN 3 THEN 'BASKETBALL'
        ELSE 'BADMINTON'
    END,
    10 + (n % 11),
    1 + (n % 10),
    8000 + (n % 15) * 1000,
    CASE n % 6
        WHEN 0 THEN 'ANY'
        WHEN 1 THEN 'LEVEL_1'
        WHEN 2 THEN 'LEVEL_2'
        WHEN 3 THEN 'LEVEL_3'
        WHEN 4 THEN 'LEVEL_4'
        ELSE 'LEVEL_5'
    END,
    CASE n % 6
        WHEN 0 THEN 'ANY'
        WHEN 1 THEN 'LEVEL_1'
        WHEN 2 THEN 'LEVEL_2'
        WHEN 3 THEN 'LEVEL_3'
        WHEN 4 THEN 'LEVEL_4'
        ELSE 'LEVEL_5'
    END,
    CASE n % 4
        WHEN 0 THEN 'ANY'
        WHEN 1 THEN 'MALE'
        WHEN 2 THEN 'FEMALE'
        ELSE 'MIXED'
    END,
    DATE_ADD(NOW(6), INTERVAL (n % 30) DAY),
    DATE_ADD(NOW(6), INTERVAL ((n % 30) + 1) DAY),
    CASE
        WHEN n % 4 = 1 THEN DATE_SUB(NOW(6), INTERVAL (n % 20) DAY)
        ELSE NULL
    END,
    CASE
        WHEN n % 4 = 3 THEN DATE_SUB(NOW(6), INTERVAL (n % 20) DAY)
        ELSE NULL
    END,
    CASE n % 4
        WHEN 0 THEN 'RECRUITING'
        WHEN 1 THEN 'CONFIRMED'
        WHEN 2 THEN 'COMPLETED'
        ELSE 'CANCELLED'
    END,
    DATE_SUB(NOW(6), INTERVAL (n % 365) DAY),
    DATE_SUB(NOW(6), INTERVAL (n % 365) DAY)
FROM numbers;
