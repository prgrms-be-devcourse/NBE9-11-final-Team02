-- ============================================================
-- FAC-01 캐싱 효과 측정용 대량 더미 데이터 (시설 10,000건)
-- 각 시설에 종목 1개 + AVAILABLE 슬롯 1개를 함께 생성
-- 조회 조건(ACTIVE / sport_type / address LIKE / AVAILABLE 슬롯)을 모두 만족
--
-- 실행:
--   Get-Content bulk-facility-data.sql -Encoding UTF8 | docker exec -i mysql_1 mysql --default-character-set=utf8mb4 -uroot -p"devpassword" team02_dev
-- ============================================================

USE team02_dev;

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

DROP PROCEDURE IF EXISTS seed_facilities;

DELIMITER $$
CREATE PROCEDURE seed_facilities(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE fid CHAR(36);
    DECLARE sid CHAR(36);
    DECLARE sport VARCHAR(20);
    DECLARE region VARCHAR(20);
    DECLARE sdate DATE;

    WHILE i < total DO
        -- 36자 UUID 생성
        SET fid = UUID();
        SET sid = UUID();

        -- 종목/지역/날짜를 인기 조건 위주로 분포시킴 (캐시 히트 유도)
        SET sport = ELT(1 + (i MOD 3), 'FUTSAL', 'TENNIS', 'BASKETBALL');
        SET region = ELT(1 + (i MOD 2), '서울특별시', '경기도');
        SET sdate = ELT(1 + (i MOD 2), '2026-07-01', '2026-07-02');

        INSERT INTO facilities (
            id, manager_id, name, address, phone, description,
            capacity, slot_duration_minutes, default_weekday_price, default_weekend_price,
            rating_avg, rating_sum, review_count, status, created_at, updated_at
        ) VALUES (
            fid,
            '11111111-1111-1111-1111-111111111111',
            CONCAT('더미시설_', i),
            CONCAT(region, ' 어딘가 ', i, '번지'),
            '02-0000-0000', '부하테스트용 더미 시설',
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

-- 1만 건 생성
CALL seed_facilities(10000);

DROP PROCEDURE seed_facilities;

-- 확인
SELECT COUNT(*) AS total_facilities FROM facilities;
SELECT sport_type, COUNT(*) AS cnt FROM facilities_sports GROUP BY sport_type;
