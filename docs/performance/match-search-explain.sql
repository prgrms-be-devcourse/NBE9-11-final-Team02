-- 매칭 목록 조회 성능 테스트 대표 쿼리

EXPLAIN ANALYZE
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

EXPLAIN ANALYZE
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
LIMIT 20 OFFSET 1000;
