import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: Number(__ENV.VUS || 20),
    iterations: Number(__ENV.ITERATIONS || 20),
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const MATCH_ID = __ENV.MATCH_ID || '여기에_MATCH_ID_넣기';
const JOIN_PATH = __ENV.JOIN_PATH || `/api/v1/matches/${MATCH_ID}/participants/pessimistic-lock`;

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
}
