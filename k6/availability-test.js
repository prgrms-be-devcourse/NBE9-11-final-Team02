import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 22,
    duration: '300s',
};

const BASE_URL = __ENV.BASE_URL || 'http://3.36.243.212:8090';

export default function () {
    const res = http.get(`${BASE_URL}/actuator/health`);

    check(res, {
        'status is 200': (r) => r.status === 200,
    });

    sleep(0.5);
}
