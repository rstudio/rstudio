// Unit tests for the handler's additions around cognito-at-edge (index.js):
// query-string restoration on refresh redirects, /parseauth routing, and the
// signed-out page. The library itself is replaced by a fake; its behaviour is
// what the fake reproduces, pinned to the cases the handler compensates for.
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { createHandler } = require('../index.js');

const HOST = 'd2pwmi0fbc7uxp.cloudfront.net';

function event(uri, querystring = '') {
  return {
    Records: [{
      cf: {
        request: {
          uri,
          querystring,
          method: 'GET',
          headers: { host: [{ key: 'Host', value: HOST }] },
        },
      },
    }],
  };
}

function redirectTo(url) {
  return { status: '302', headers: { location: [{ key: 'Location', value: url }] } };
}

// A fake Authenticator: handle() answers from a script, handleParseAuth()
// records that it was used.
function fakeAuthenticator(handleResult) {
  const calls = { handle: 0, handleParseAuth: 0 };
  const authenticator = {
    async handle() {
      calls.handle++;
      return handleResult;
    },
    async handleParseAuth() {
      calls.handleParseAuth++;
      return redirectTo(`https://${HOST}/pr-1/index.html`);
    },
  };
  return { authenticator, calls, handler: createHandler(async () => ({ authenticator })) };
}

test('refresh redirect to the request path keeps the query string', async () => {
  // Expired ID token with a valid refresh token: the library redirects to
  // request.uri with new cookies and no query string.
  const { handler } = fakeAuthenticator(redirectTo(`https://${HOST}/pr-1/trace/index.html`));

  const result = await handler(event('/pr-1/trace/index.html', 'trace=https%3A%2F%2Fx%2Fdata%2Fa.zip'));

  assert.equal(result.status, '302');
  assert.equal(result.headers.location[0].value,
    `https://${HOST}/pr-1/trace/index.html?trace=https%3A%2F%2Fx%2Fdata%2Fa.zip`);
});

test('redirects elsewhere are left alone', async () => {
  // Unauthenticated: the library redirects to the hosted UI.
  const signIn = `https://pool.auth.us-east-1.amazoncognito.com/authorize?client_id=x&state=%2Fpr-1%2Findex.html%3Fa%3D1`;
  const { handler } = fakeAuthenticator(redirectTo(signIn));

  const result = await handler(event('/pr-1/index.html', 'a=1'));

  assert.equal(result.headers.location[0].value, signIn);
});

test('a redirect to the bare path without a query string is unchanged', async () => {
  const { handler } = fakeAuthenticator(redirectTo(`https://${HOST}/pr-1/index.html`));

  const result = await handler(event('/pr-1/index.html'));

  assert.equal(result.headers.location[0].value, `https://${HOST}/pr-1/index.html`);
});

test('an authenticated request passes through untouched', async () => {
  const request = event('/pr-1/index.html', 'a=1').Records[0].cf.request;
  const { handler } = fakeAuthenticator(request);

  const result = await handler(event('/pr-1/index.html', 'a=1'));

  assert.equal(result, request);
});

test('/parseauth with a code is exchanged even when a session already exists', async () => {
  // handle() would let an already-authenticated /parseauth request through
  // to S3; the handler routes it to the code exchange instead.
  const { handler, calls } = fakeAuthenticator(event('/parseauth').Records[0].cf.request);

  const result = await handler(event('/parseauth', 'code=abc&state=%2Fpr-1%2Findex.html'));

  assert.equal(calls.handleParseAuth, 1);
  assert.equal(calls.handle, 0);
  assert.equal(result.status, '302');
});

test('/parseauth without a code goes through the normal flow', async () => {
  const { handler, calls } = fakeAuthenticator(redirectTo('https://pool.auth.us-east-1.amazoncognito.com/authorize'));

  await handler(event('/parseauth'));

  assert.equal(calls.handleParseAuth, 0);
  assert.equal(calls.handle, 1);
});

test('/signed-out is served without consulting the authenticator', async () => {
  const { handler, calls } = fakeAuthenticator(redirectTo('https://pool.auth.us-east-1.amazoncognito.com/authorize'));

  const result = await handler(event('/signed-out'));

  assert.equal(result.status, '200');
  assert.match(result.body, /signed out/i);
  assert.equal(calls.handle, 0);
});
