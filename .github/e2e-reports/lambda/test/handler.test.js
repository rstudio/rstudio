// Unit tests for the handler's additions around cognito-at-edge (index.js):
// query-string restoration on refresh redirects, /parseauth routing, and the
// signed-out page. The library itself is replaced by a fake; its behaviour is
// what the fake reproduces, pinned to the cases the handler compensates for.
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { createHandler } = require('../index.js');

const HOST = 'd2pwmi0fbc7uxp.cloudfront.net';
const APP = 'app1';
const CONFIG = { userPoolAppId: APP };
const COOKIE = (name) => `CognitoIdentityServiceProvider.${APP}.${name}`;

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
  return { authenticator, calls, handler: createHandler(async () => ({ authenticator, config: CONFIG })) };
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

test('/parseauth with an OAuth error and no code reports it without redirecting', async () => {
  // Left to handle(), the error callback would become the return address of
  // a fresh sign-in, and a successful retry would then land on S3.
  const { handler, calls } = fakeAuthenticator(redirectTo('https://pool.auth.us-east-1.amazoncognito.com/authorize'));

  const result = await handler(event('/parseauth',
    'error=invalid_request&error_description=Timeout+%3Cx%3E&state=%2Fpr-1%2Ftrace%2Findex.html%3Ftrace%3Da%26b%3D1'));

  assert.equal(calls.handle, 0);
  assert.equal(calls.handleParseAuth, 0);
  assert.equal(result.status, '400');
  assert.match(result.body, /Sign-in did not complete \(invalid_request: Timeout &lt;x&gt;\)/);
  // Retry at the report that was being opened (the OAuth state), escaped.
  assert.match(result.body, /href="\/pr-1\/trace\/index\.html\?trace=a&amp;b=1">Try again<\/a>/);
  assert.equal(result.headers['cache-control'][0].value, 'no-store');
});

test('the retry link is omitted when the state is not a path on this origin', async () => {
  for (const state of [
    'https://evil.example/x',
    '//evil.example/x',
    '/parseauth?error=x',
    'pr-1/index.html',
    '/pr-1/\r\ninjected',
    '/pr-1\\..\\x',
  ]) {
    const { handler } = fakeAuthenticator(redirectTo('https://pool.auth.us-east-1.amazoncognito.com/authorize'));
    const result = await handler(event('/parseauth', 'error=access_denied&state=' + encodeURIComponent(state)));
    assert.equal(result.status, '400', state);
    assert.doesNotMatch(result.body, /href=/, state);
    assert.match(result.body, /Reopen the report link/, state);
  }
});

test('/parseauth with neither code nor error gets the same page', async () => {
  const { handler, calls } = fakeAuthenticator(redirectTo('https://pool.auth.us-east-1.amazoncognito.com/authorize'));

  const result = await handler(event('/parseauth'));

  assert.equal(calls.handle, 0);
  assert.equal(result.status, '400');
  assert.match(result.body, /Sign-in did not complete\. Reopen the report link/);
});

test('/signed-out is served without consulting the authenticator', async () => {
  const { handler, calls } = fakeAuthenticator(redirectTo('https://pool.auth.us-east-1.amazoncognito.com/authorize'));

  const result = await handler(event('/signed-out'));

  assert.equal(result.status, '200');
  assert.match(result.body, /signed out/i);
  assert.equal(calls.handle, 0);
});

test('/signout with no Cookie header reaches the library with an empty one', async () => {
  // The library's cookie clearing calls request.headers.cookie.flatMap(),
  // which throws on an absent header, i.e. on a repeat visit to /signout.
  let seen;
  const authenticator = {
    async handle(event) {
      seen = event.Records[0].cf.request.headers.cookie;
      return redirectTo('https://pool.auth.us-east-1.amazoncognito.com/logout');
    },
    async handleParseAuth() {
      throw new Error('not expected');
    },
  };
  const handler = createHandler(async () => ({ authenticator, config: CONFIG }));

  const result = await handler(event('/signout'));

  assert.deepEqual(seen, []);
  assert.equal(result.status, '302');
});

test('/signout/ and other sign-out prefixes are normalized the same way', async () => {
  // The library recognizes sign-out by prefix, so the guard has to as well.
  for (const uri of ['/signout/', '/signout?x=1']) {
    let seen;
    const authenticator = {
      async handle(event) {
        seen = event.Records[0].cf.request.headers.cookie;
        return redirectTo('https://pool.auth.us-east-1.amazoncognito.com/logout');
      },
    };
    const handler = createHandler(async () => ({ authenticator, config: CONFIG }));

    await handler(event(uri));

    assert.deepEqual(seen, [], uri);
  }
});

test('a request that carries cookies keeps them as they are', async () => {
  let seen;
  const authenticator = {
    async handle(event) {
      seen = event.Records[0].cf.request.headers.cookie;
      return event.Records[0].cf.request;
    },
  };
  const handler = createHandler(async () => ({ authenticator, config: CONFIG }));
  const withCookie = event('/signout');
  withCookie.Records[0].cf.request.headers.cookie = [{ key: 'Cookie', value: 'a=1' }];

  await handler(withCookie);

  assert.deepEqual(seen, [{ key: 'Cookie', value: 'a=1' }]);
});

// A Cookie header carrying the given name=value pairs, one header value.
function cookieHeader(pairs) {
  return [{ key: 'Cookie', value: pairs.map(([n, v]) => `${n}=${v}`).join('; ') }];
}

function setCookieNames(result) {
  return result.headers['set-cookie'].map((h) => h.value.split('=')[0]);
}

function expiring(result, name) {
  return result.headers['set-cookie'].find((h) => h.value.startsWith(name + '='));
}

test('sign-out also expires another signed-in user\'s cookies', async () => {
  // The library clears the last user's (alice) tokens; bob's would remain.
  const cleared = [COOKIE('alice.idToken'), COOKIE('alice.accessToken'), COOKIE('alice.refreshToken'), COOKIE('LastAuthUser')];
  const authenticator = {
    async handle() {
      return {
        status: '302',
        headers: {
          location: [{ key: 'Location', value: 'https://pool.auth.us-east-1.amazoncognito.com/logout' }],
          'set-cookie': cleared.map((n) => ({ key: 'Set-Cookie', value: `${n}=; Domain=${HOST}; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Secure; HttpOnly; SameSite=Lax` })),
        },
      };
    },
  };
  const handler = createHandler(async () => ({ authenticator, config: CONFIG }));
  const request = event('/signout');
  request.Records[0].cf.request.headers.cookie = cookieHeader([
    [COOKIE('LastAuthUser'), 'alice'],
    [COOKIE('alice.idToken'), 'a1'], [COOKIE('alice.accessToken'), 'a2'], [COOKIE('alice.refreshToken'), 'a3'],
    [COOKIE('bob.idToken'), 'b1'], [COOKIE('bob.accessToken'), 'b2'], [COOKIE('bob.refreshToken'), 'b3'],
    ['unrelated', 'x'],
  ]);

  const result = await handler(request);

  const names = setCookieNames(result);
  for (const n of ['bob.idToken', 'bob.accessToken', 'bob.refreshToken'])
    assert.ok(names.includes(COOKIE(n)), n);
  assert.ok(!names.includes('unrelated'));
  // Same attributes as the library's own clearing, so the browser honors it.
  assert.equal(expiring(result, COOKIE('bob.idToken')).value,
    `${COOKIE('bob.idToken')}=; Domain=${HOST}; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Secure; HttpOnly; SameSite=Lax`);
  // The library's own entries are kept once, not duplicated.
  assert.equal(names.filter((n) => n === COOKIE('alice.idToken')).length, 1);
});

test('a new sign-in expires the previous user\'s cookies', async () => {
  const authenticator = {
    async handleParseAuth() {
      const set = [COOKIE('bob.idToken'), COOKIE('bob.accessToken'), COOKIE('bob.refreshToken'), COOKIE('LastAuthUser')];
      return {
        status: '302',
        headers: {
          location: [{ key: 'Location', value: `https://${HOST}/pr-1/index.html` }],
          'set-cookie': set.map((n) => ({ key: 'Set-Cookie', value: `${n}=new; Domain=${HOST}; Path=/; Secure; HttpOnly; SameSite=Lax` })),
        },
      };
    },
    async handle() {
      throw new Error('not expected');
    },
  };
  const handler = createHandler(async () => ({ authenticator, config: CONFIG }));
  const request = event('/parseauth', 'code=abc&state=%2Fpr-1%2Findex.html');
  request.Records[0].cf.request.headers.cookie = cookieHeader([
    [COOKIE('LastAuthUser'), 'alice'],
    [COOKIE('alice.idToken'), 'a1'], [COOKIE('alice.accessToken'), 'a2'], [COOKIE('alice.refreshToken'), 'a3'],
  ]);

  const result = await handler(request);

  const names = setCookieNames(result);
  for (const n of ['alice.idToken', 'alice.accessToken', 'alice.refreshToken'])
    assert.ok(expiring(result, COOKIE(n)).value.includes('Expires=Thu, 01 Jan 1970'), n);
  // The new user's cookies and LastAuthUser come from the library, untouched.
  assert.equal(expiring(result, COOKIE('LastAuthUser')).value.startsWith(`${COOKIE('LastAuthUser')}=new`), true);
  assert.equal(names.filter((n) => n === COOKIE('LastAuthUser')).length, 1);
});

test('a pass-through or a redirect without stale cookies is left alone', async () => {
  const passThrough = event('/pr-1/index.html').Records[0].cf.request;
  passThrough.headers.cookie = cookieHeader([[COOKIE('alice.idToken'), 'a1']]);
  const { handler } = fakeAuthenticator(passThrough);
  const request = event('/signout');
  request.Records[0].cf.request.headers.cookie = cookieHeader([[COOKIE('alice.idToken'), 'a1']]);

  const result = await handler(request);

  assert.equal(result, passThrough);
});
