// Tests of the production Authenticator configuration (createAuthenticator in
// ../index.js), driving the real cognito-at-edge along its paths that need no
// network: the redirect to the hosted UI, the sign-out redirect, and the
// settings the rest of the handler relies on. handler.test.js covers the
// handler's own logic against a fake; this file is what catches a changed
// logout destination or a logging level that would put tokens in CloudWatch.
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { createAuthenticator, createHandler } = require('../index.js');

const HOST = 'd2pwmi0fbc7uxp.cloudfront.net';
const CONFIG = {
  userPoolId: 'us-east-1_0zXS4ejj0',
  userPoolAppId: 'app1',
  userPoolDomain: 'rstudio-e2e-reports.auth.us-east-1.amazoncognito.com',
  baseUrl: `https://${HOST}`,
  sessionDays: 30,
};

function event(uri, querystring = '', cookie) {
  const headers = { host: [{ key: 'Host', value: HOST }] };
  if (cookie !== undefined)
    headers.cookie = cookie;
  return { Records: [{ cf: { request: { uri, querystring, method: 'GET', headers } } }] };
}

function handlerWithProductionConfig() {
  const authenticator = createAuthenticator(CONFIG);
  return { authenticator, handler: createHandler(async () => ({ authenticator, config: CONFIG })) };
}

test('an unauthenticated request is sent to the hosted UI with our callback and return path', async () => {
  const { handler } = handlerWithProductionConfig();

  const result = await handler(event('/pr-1/trace/index.html', 'trace=a'));

  assert.equal(result.status, '302');
  const location = new URL(result.headers.location[0].value);
  assert.equal(location.origin, `https://${CONFIG.userPoolDomain}`);
  assert.equal(location.pathname, '/authorize');
  assert.equal(location.searchParams.get('client_id'), CONFIG.userPoolAppId);
  assert.equal(location.searchParams.get('response_type'), 'code');
  assert.equal(location.searchParams.get('redirect_uri'), `https://${HOST}/parseauth`);
  // The library keeps the query string on this path (see restoreQueryString
  // for the one path where it does not).
  assert.equal(location.searchParams.get('state'), '/pr-1/trace/index.html?trace=a');
});

test('sign-out redirects to Cognito\'s logout endpoint, ending at the signed-out page', async () => {
  // No session at all: the library clears cookies and redirects without any
  // network call, which is what makes this testable here.
  const { handler } = handlerWithProductionConfig();

  const result = await handler(event('/signout'));

  assert.equal(result.status, '302');
  assert.equal(result.headers.location[0].value,
    `https://${CONFIG.userPoolDomain}/logout?client_id=${CONFIG.userPoolAppId}` +
    `&logout_uri=${encodeURIComponent(`https://${HOST}/signed-out`)}`);
});

test('the signed-out page is served without a session', async () => {
  const { handler } = handlerWithProductionConfig();

  const result = await handler(event('/signed-out'));

  assert.equal(result.status, '200');
});

test('the library is configured not to log: a failed refresh would log the refresh token', () => {
  const { authenticator } = handlerWithProductionConfig();

  assert.equal(authenticator._logger.level, 'silent');
});

test('cookie settings match what the stale-cookie expiry reproduces', () => {
  // Private fields of cognito-at-edge, pinned to the exact version in
  // package.json; a bump that renames them fails here rather than silently.
  const { authenticator } = handlerWithProductionConfig();

  assert.equal(authenticator._cookiePath, '/');
  assert.equal(authenticator._httpOnly, true);
  assert.equal(authenticator._sameSite, 'Lax');
  assert.equal(authenticator._cookieExpirationDays, CONFIG.sessionDays);
  assert.equal(authenticator._disableCookieDomain, false);
  assert.equal(authenticator._cookieDomain, undefined);
  // Stored without its leading slash; the library prepends one when it
  // builds the callback URL (asserted on the hosted-UI redirect above).
  assert.equal(authenticator._parseAuthPath, 'parseauth');
  assert.equal(authenticator._cookieBase, `CognitoIdentityServiceProvider.${CONFIG.userPoolAppId}`);
});
