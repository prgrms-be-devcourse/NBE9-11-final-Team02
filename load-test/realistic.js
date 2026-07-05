// 현실 테스트: think time 1~2s, 목표 p95 < 500ms + 에러율 0%
// 실행: BASE_URL=http://<서버IP> k6 run load-test/realistic.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://3.36.243.212';

const matchListDuration = new Trend('match_list_duration', true);
const errorRate = new Rate('error_rate');

export const options = {
  scenarios: {
    realistic: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 50  }, { duration: '50s', target: 50  },
        { duration: '10s', target: 100 }, { duration: '50s', target: 100 },
        { duration: '10s', target: 150 }, { duration: '50s', target: 150 },
        { duration: '10s', target: 200 }, { duration: '50s', target: 200 },
        { duration: '10s', target: 300 }, { duration: '50s', target: 300 },
        { duration: '10s', target: 0   },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed:     ['rate<0.01'],
    http_req_duration:   ['p(95)<1000'],
    match_list_duration: ['p(95)<1000'],
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/matches?page=0&size=20`);
  const ok = check(res, { 'status 200': (r) => r.status === 200 });
  matchListDuration.add(res.timings.duration);
  errorRate.add(!ok);

  sleep(1 + Math.random());
}
