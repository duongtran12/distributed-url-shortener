import http from 'k6/http';
import { check } from 'k6';

const baseUrl = (__ENV.BASE_URL || 'http://nginx').replace(/\/$/, '');
const rate = positiveInteger(__ENV.RATE, 1, 'RATE');
const preAllocatedVUs = positiveInteger(
  __ENV.PRE_ALLOCATED_VUS,
  5,
  'PRE_ALLOCATED_VUS',
);
const maxVUs = positiveInteger(__ENV.MAX_VUS, 20, 'MAX_VUS');
const p95Milliseconds = positiveInteger(__ENV.P95_MS, 250, 'P95_MS');

if (maxVUs < preAllocatedVUs) {
  throw new Error('MAX_VUS must be greater than or equal to PRE_ALLOCATED_VUS');
}

http.setResponseCallback(http.expectedStatuses(302));

export const options = {
  scenarios: {
    redirect: {
      executor: 'constant-arrival-rate',
      rate,
      timeUnit: '1s',
      duration: __ENV.DURATION || '30s',
      preAllocatedVUs,
      maxVUs,
    },
  },
  thresholds: {
    checks: ['rate>0.99'],
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:short-link-redirect}': [
      `p(95)<${p95Milliseconds}`,
    ],
  },
};

export function setup() {
  const shortCode = (__ENV.SHORT_CODE || '').trim();

  if (!shortCode) {
    throw new Error(
      'SHORT_CODE is required. Set LOAD_TEST_SHORT_CODE before running Compose.',
    );
  }

  return { shortCode };
}

export default function ({ shortCode }) {
  const response = http.get(`${baseUrl}/${encodeURIComponent(shortCode)}`, {
    redirects: 0,
    tags: { name: 'short-link-redirect' },
  });

  check(response, {
    'returns HTTP 302': (result) => result.status === 302,
    'includes redirect location': (result) => Boolean(result.headers.Location),
    'includes request ID': (result) => Boolean(result.headers['X-Request-Id']),
  });
}

function positiveInteger(rawValue, fallback, name) {
  const value = rawValue === undefined || rawValue === ''
    ? fallback
    : Number(rawValue);

  if (!Number.isInteger(value) || value <= 0) {
    throw new Error(`${name} must be a positive integer`);
  }

  return value;
}
