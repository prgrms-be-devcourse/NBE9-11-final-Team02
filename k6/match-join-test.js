import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const VUS = Number(__ENV.VUS || 100);
const DURATION = __ENV.DURATION || '30s';
const ITERATIONS = __ENV.ITERATIONS ? Number(__ENV.ITERATIONS) : null;

export const options = ITERATIONS
    ? {
        scenarios: {
            oneShot: {
                executor: 'per-vu-iterations',
                vus: VUS,
                iterations: Math.ceil(ITERATIONS / VUS),
                maxDuration: '30s',
            },
        },
    }
    : {
        vus: VUS,
        duration: DURATION,
    };

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8090';
const MATCH_ID = __ENV.MATCH_ID || '여기에_MATCH_ID_넣기';
const JOIN_PATH = `/api/v1/matches/${MATCH_ID}/participants`;

const created = new Counter('join_created');
const conflict = new Counter('join_conflict');
const unexpected = new Counter('join_unexpected');

export default function () {
    const userId = `k6-user-${__VU}-${__ITER}`;

    const res = http.post(
        `${BASE_URL}${JOIN_PATH}`,
        null,
        {
            headers: {
                'X-USER-ID': userId,
            },
        }
    );

    check(res, {
        '201 or 409': (r) => r.status === 201 || r.status === 409,
    });

    if (res.status === 201) {
        created.add(1);
    } else if (res.status === 409) {
        conflict.add(1);
    } else {
        unexpected.add(1);
    }
}
