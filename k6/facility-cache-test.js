import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// 커스텀 지표: 응답 시간 추세와 실패율
const latency = new Trend('facility_query_latency', true);
const failRate = new Rate('facility_query_failed');

// 서버 URL은 환경변수로 주입 (기본값: 로컬)
// 실행 예: k6 run -e BASE_URL=http://localhost:8090 facility-cache-test.js
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8090';

export const options = {
    // 워밍업 → 부하 유지 → 정리 단계로 나눠 캐시 효과를 안정적으로 측정
    stages: [
        { duration: '10s', target: 20 },  // 워밍업: 캐시 채우기
        { duration: '40s', target: 100 }, // 부하 유지
        { duration: '10s', target: 0 },   // 정리
    ],
    thresholds: {
        // 캐싱이 잘 동작하면 p95가 100ms 이하여야 함
        'http_req_duration': ['p(95)<100'],
        // 실패율 1% 미만
        'facility_query_failed': ['rate<0.01'],
    },
};

// 캐시 히트를 유도하려면 동일 조건 요청 비중이 높아야 함.
// 실제 캐싱 효과를 보려면 소수의 인기 조건에 요청을 집중시킨다.
// ※ 사전에 이 조건에 맞는 시설 더미 데이터가 DB에 있어야 캐싱이 동작함.
const POPULAR_QUERIES = [
    { sportType: 'FUTSAL', region: '서울', date: '2026-07-01' },
    { sportType: 'TENNIS', region: '경기', date: '2026-07-01' },
    { sportType: 'BASKETBALL', region: '서울', date: '2026-07-02' },
];

export default function () {
    // 80%는 인기 조건(캐시 히트 유도), 20%는 랜덤 조건(캐시 미스)
    let query;
    if (Math.random() < 0.8) {
        query = POPULAR_QUERIES[Math.floor(Math.random() * POPULAR_QUERIES.length)];
    } else {
        query = {
            sportType: 'FUTSAL',
            region: `region-${Math.floor(Math.random() * 1000)}`,
            date: '2026-07-01',
        };
    }

    const url = `${BASE_URL}/api/v1/facilities/available` +
        `?sportType=${query.sportType}` +
        `&region=${encodeURIComponent(query.region)}` +
        `&date=${query.date}` +
        `&page=0&size=20`;

    const res = http.get(url);

    latency.add(res.timings.duration);
    failRate.add(res.status !== 200);

    check(res, {
        'status 200': (r) => r.status === 200,
        'has body': (r) => r.body && r.body.length > 0,
    });

    sleep(1);
}

// 테스트 종료 후 요약 출력
export function handleSummary(data) {
    const p95 = data.metrics.http_req_duration.values['p(95)'];
    const avg = data.metrics.http_req_duration.values.avg;
    const failed = data.metrics.facility_query_failed
        ? (data.metrics.facility_query_failed.values.rate * 100).toFixed(2)
        : 'N/A';

    console.log('\n========== FAC-01 결과 요약 ==========');
    console.log(`평균 응답시간: ${avg.toFixed(2)}ms`);
    console.log(`p95 응답시간 : ${p95.toFixed(2)}ms`);
    console.log(`실패율       : ${failed}%`);
    console.log('=====================================\n');
    console.log('캐시 ON/OFF로 각각 실행 후 위 수치를 비교하세요.');

    return {
        'stdout': '',
    };
}