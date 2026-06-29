import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const latency = new Trend('facility_query_latency', true);
const failRate = new Rate('facility_query_failed');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8090';

export const options = {
  stages: [
    { duration: '10s', target: 50 },   // 워밍업: 캐시 채우기
    { duration: '40s', target: 300 },  // 트래픽 급증 (인기 시간대 모사)
    { duration: '10s', target: 0 },    // 정리
  ],
  thresholds: {
    'http_req_duration': ['p(95)<150'],
    'facility_query_failed': ['rate<0.01'],
  },
};

const POPULAR_QUERIES = [
  { sportType: 'FUTSAL',     region: '서울', date: '2026-07-01' },
  { sportType: 'FUTSAL',     region: '서울', date: '2026-07-02' },
  { sportType: 'BASKETBALL', region: '서울', date: '2026-07-01' },
  { sportType: 'TENNIS',     region: '서울', date: '2026-07-01' },
  { sportType: 'TENNIS',     region: '경기', date: '2026-07-01' },
];

export default function () {
  let query;
  if (Math.random() < 0.95) {
    // 95%: 인기 조합 (캐시 히트)
    query = POPULAR_QUERIES[Math.floor(Math.random() * POPULAR_QUERIES.length)];
  } else {
    // 5%: 롱테일 랜덤 (캐시 미스)
    query = {
      sportType: 'FUTSAL',
      region: `외곽-${Math.floor(Math.random() * 500)}`,
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
  });

  sleep(0.5);
}

export function handleSummary(data) {
  const p95 = data.metrics.http_req_duration.values['p(95)'];
  const avg = data.metrics.http_req_duration.values.avg;
  const reqs = data.metrics.http_reqs.values.count;
  const failed = data.metrics.facility_query_failed
    ? (data.metrics.facility_query_failed.values.rate * 100).toFixed(2)
    : 'N/A';

  console.log('\n========== 캐싱 테스트 결과 요약 (500건 / VUs 300) ==========');
  console.log(`총 요청 수   : ${reqs}`);
  console.log(`평균 응답시간: ${avg.toFixed(2)}ms`);
  console.log(`p95 응답시간 : ${p95.toFixed(2)}ms`);
  console.log(`실패율       : ${failed}%`);
  console.log('======================================================');
  console.log('  캐시 ON: 인기조건 5개만 DB 조회 → 쿼리 수 적음');
  console.log('  캐시 OFF: 매 요청 DB 조회 → 쿼리 수 = 총 요청 수에 근접\n');

  return { 'stdout': '' };
}