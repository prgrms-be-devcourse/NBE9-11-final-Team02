-- ============================================================
-- FAC-01 재설계: 현실적 규모 시설 500건
-- 목적: "인기 조건 트래픽 쏠림 시 캐시가 DB 부하를 줄이는가" 검증
-- 조건 분포를 소수 인기 조합에 집중시켜 캐시 히트를 유도
-- ============================================================

USE team02_dev;

-- 기존 더미 데이터 정리 (이전 1만 건 + 초기 3건)
DELETE FROM facility_images   WHERE facility_id IN (SELECT id FROM facilities WHERE manager_id = '11111111-1111-1111-1111-111111111111');
DELETE FROM facility_slots    WHERE facility_id IN (SELECT id FROM facilities WHERE manager_id = '11111111-1111-1111-1111-111111111111');
DELETE FROM facilities_sports WHERE facility_id IN (SELECT id FROM facilities WHERE manager_id = '11111111-1111-1111-1111-111111111111');
DELETE FROM facilities        WHERE manager_id = '11111111-1111-1111-1111-111111111111';

-- 매니저 유저 (FK)
INSERT IGNORE INTO users (
    id, email, nickname, password_hash, role, provider,
    manner_score, skill_score, manner_rating_sum, manner_review_count,
    created_at, updated_at
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    'dummy-manager@test.com', '더미매니저', 'x', 'MANAGER', 'LOCAL',
    0.0, 0.0, 0.0, 0, NOW(), NOW()
);

DROP PROCEDURE IF EXISTS seed_facilities_500;

DELIMITER $$
CREATE PROCEDURE seed_facilities_500(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE fid CHAR(36);
    DECLARE sid CHAR(36);
    DECLARE sport VARCHAR(20);
    DECLARE region VARCHAR(20);
    DECLARE sdate DATE;

    WHILE i < total DO
        SET fid = UUID();
        SET sid = UUID();

        -- 인기 조합에 쏠리도록 분포 (서울/풋살 비중을 높임)
        SET sport  = ELT(1 + (i MOD 3), 'FUTSAL', 'TENNIS', 'BASKETBALL');
        SET region = IF(i MOD 10 < 7, '서울특별시', '경기도');   -- 70% 서울
        SET sdate  = ELT(1 + (i MOD 2), '2026-07-01', '2026-07-02');

        INSERT INTO facilities (
            id, manager_id, name, address, phone, description,
            capacity, slot_duration_minutes, default_weekday_price, default_weekend_price,
            rating_avg, rating_sum, review_count, status, created_at, updated_at
        ) VALUES (
            fid, '11111111-1111-1111-1111-111111111111',
            CONCAT('시설_', i), CONCAT(region, ' 일대 ', i, '번지'),
            '02-0000-0000', '테스트 시설',
            20, 60, 100000, 120000,
            0.00, 0.0, 0, 'ACTIVE', NOW(), NOW()
        );

        INSERT INTO facilities_sports (facility_id, sport_type) VALUES (fid, sport);

        INSERT INTO facility_slots (
            id, facility_id, slot_date, start_time, end_time, price, status, created_at, updated_at
        ) VALUES (
            sid, fid, sdate, '10:00:00', '12:00:00', 100000, 'AVAILABLE', NOW(), NOW()
        );

        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

CALL seed_facilities_500(500);
DROP PROCEDURE seed_facilities_500;

-- 확인
SELECT COUNT(*) AS total_facilities FROM facilities;
SELECT sport_type, COUNT(*) AS cnt FROM facilities_sports GROUP BY sport_type;
