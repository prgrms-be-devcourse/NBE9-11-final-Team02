// 마이페이지 경기목록 조회 부하테스트 (인덱싱 before/after 비교용)
// 실행: BASE_URL=http://<서버IP> k6 run load-test/mypage-matches.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://3.36.243.212';

const myMatchesDuration = new Trend('my_matches_duration', true);
const errorRate = new Rate('error_rate');

export const options = {
  scenarios: {
    mypage: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 50  }, { duration: '50s', target: 50  },
        { duration: '10s', target: 100 }, { duration: '50s', target: 100 },
        { duration: '10s', target: 150 }, { duration: '50s', target: 150 },
        { duration: '10s', target: 0   },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed:     ['rate<0.01'],
    my_matches_duration: ['p(95)<1000'],
  },
};

// 테스트 시작 시 1회 로그인해서 토큰 공유
export function setup() {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email: 'fixture.host@seed.sportteam.local', password: 'test1234!!' }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  const token = res.json('data.accessToken');
  if (!token) throw new Error(`로그인 실패: ${res.status} ${res.body}`);
  return { token };
}

export default function (data) {
  const res = http.get(`${BASE_URL}/api/v1/users/me/matches?page=0&size=10`, {
    headers: { Authorization: `Bearer ${data.token}` },
  });
  const ok = check(res, { 'status 200': (r) => r.status === 200 });
  myMatchesDuration.add(res.timings.duration);
  errorRate.add(!ok);

  sleep(1 + Math.random());
}
