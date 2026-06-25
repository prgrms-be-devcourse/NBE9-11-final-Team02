-- ============================================================
-- FAC-01 k6 캐싱 테스트용 더미 시설 데이터 (ID 36자 UUID 형식 수정본)
-- k6 인기 조건: (FUTSAL/서울/2026-07-01), (TENNIS/경기/2026-07-01), (BASKETBALL/서울/2026-07-02)
-- 실행: Get-Content dummy-facility-data.sql -Encoding UTF8 | docker exec -i mysql_1 mysql --default-character-set=utf8mb4 -uroot -p"비번" team02_dev
-- ============================================================

USE team02_dev;

-- 매니저 유저 (facilities.manager_id FK)
INSERT IGNORE INTO users (
    id, email, nickname, password_hash, role, provider,
    manner_score, skill_score, manner_rating_sum, manner_review_count,
    created_at, updated_at
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    'dummy-manager@test.com', '더미매니저', 'x', 'MANAGER', 'LOCAL',
    0.0, 0.0, 0.0, 0,
    NOW(), NOW()
);

-- ---------- 시설 1: 강남 풋살장 (FUTSAL / 서울) ----------
INSERT INTO facilities (
    id, manager_id, name, address, phone, description,
    capacity, slot_duration_minutes, default_weekday_price, default_weekend_price,
    rating_avg, rating_sum, review_count, status, created_at, updated_at
) VALUES (
             'aaaaaaaa-0000-0000-0000-000000000001',
             '11111111-1111-1111-1111-111111111111',
             '강남 풋살장', '서울특별시 강남구 테헤란로 123', '02-1111-1111', '실내 풋살장',
             22, 60, 100000, 120000,
             0.00, 0.0, 0, 'ACTIVE', NOW(), NOW()
         );

INSERT INTO facilities_sports (facility_id, sport_type) VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001', 'FUTSAL');

INSERT INTO facility_slots (
    id, facility_id, slot_date, start_time, end_time, price, status, created_at, updated_at
) VALUES
      ('bbbbbbbb-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001',
       '2026-07-01', '10:00:00', '12:00:00', 100000, 'AVAILABLE', NOW(), NOW()),
      ('bbbbbbbb-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001',
       '2026-07-01', '14:00:00', '16:00:00', 100000, 'AVAILABLE', NOW(), NOW());

-- ---------- 시설 2: 수원 테니스장 (TENNIS / 경기) ----------
INSERT INTO facilities (
    id, manager_id, name, address, phone, description,
    capacity, slot_duration_minutes, default_weekday_price, default_weekend_price,
    rating_avg, rating_sum, review_count, status, created_at, updated_at
) VALUES (
             'aaaaaaaa-0000-0000-0000-000000000002',
             '11111111-1111-1111-1111-111111111111',
             '수원 테니스장', '경기도 수원시 영통구 월드컵로 456', '031-2222-2222', '야외 테니스 코트',
             4, 60, 40000, 50000,
             0.00, 0.0, 0, 'ACTIVE', NOW(), NOW()
         );

INSERT INTO facilities_sports (facility_id, sport_type) VALUES
    ('aaaaaaaa-0000-0000-0000-000000000002', 'TENNIS');

INSERT INTO facility_slots (
    id, facility_id, slot_date, start_time, end_time, price, status, created_at, updated_at
) VALUES
    ('bbbbbbbb-0000-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000002',
     '2026-07-01', '09:00:00', '10:00:00', 40000, 'AVAILABLE', NOW(), NOW());

-- ---------- 시설 3: 송파 농구장 (BASKETBALL / 서울) ----------
INSERT INTO facilities (
    id, manager_id, name, address, phone, description,
    capacity, slot_duration_minutes, default_weekday_price, default_weekend_price,
    rating_avg, rating_sum, review_count, status, created_at, updated_at
) VALUES (
             'aaaaaaaa-0000-0000-0000-000000000003',
             '11111111-1111-1111-1111-111111111111',
             '송파 농구장', '서울특별시 송파구 올림픽로 789', '02-3333-3333', '실내 농구 코트',
             10, 60, 80000, 90000,
             0.00, 0.0, 0, 'ACTIVE', NOW(), NOW()
         );

INSERT INTO facilities_sports (facility_id, sport_type) VALUES
    ('aaaaaaaa-0000-0000-0000-000000000003', 'BASKETBALL');

INSERT INTO facility_slots (
    id, facility_id, slot_date, start_time, end_time, price, status, created_at, updated_at
) VALUES
    ('bbbbbbbb-0000-0000-0000-000000000004', 'aaaaaaaa-0000-0000-0000-000000000003',
     '2026-07-02', '18:00:00', '20:00:00', 80000, 'AVAILABLE', NOW(), NOW());

-- ---------- 이미지 (썸네일 응답 확인용) ----------
INSERT INTO facility_images (facility_id, image_order, image_url) VALUES
                                                                      ('aaaaaaaa-0000-0000-0000-000000000001', 0, 'https://example.com/futsal1.jpg'),
                                                                      ('aaaaaaaa-0000-0000-0000-000000000002', 0, 'https://example.com/tennis1.jpg'),
                                                                      ('aaaaaaaa-0000-0000-0000-000000000003', 0, 'https://example.com/basket1.jpg');

-- ---------- 확인용 조회 ----------
SELECT f.name, f.address, f.status, fs.sport_type, sl.slot_date, sl.status AS slot_status
FROM facilities f
         JOIN facilities_sports fs ON fs.facility_id = f.id
         JOIN facility_slots sl ON sl.facility_id = f.id
ORDER BY f.name;