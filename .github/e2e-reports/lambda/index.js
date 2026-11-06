// Viewer-request handler for the e2e reports CloudFront distribution. Every
// request must carry a valid Cognito session (cookies set after Google
// sign-in); anything else is redirected to the hosted UI, which sends the
// browser to Google and back. cognito-at-edge does all of that; this file
// configures it and papers over the gaps in its handle() (see createHandler).
const { Authenticator } = require('cognito-at-edge');
const { parseCookies, serializeCookie, getCookieDomain } = require('cognito-at-edge/dist/util/cookie');

// Lambda@Edge functions cannot carry environment variables, so the stack
// publishes the pool details under this name (ViewerConfig in template.yaml).
const CONFIG_PARAMETER = '/rstudio-e2e-reports/viewer';
const REGION = 'us-east-1';

// Fixed callback path, so the app client's callback URL can be exact. The
// originally requested URL rides along in the OAuth state and is restored
// after sign-in.
const PARSE_AUTH_PATH = '/parseauth';
const SIGN_OUT_PATH = '/signout';
// Where sign-out ends: a static page served by this function with no session
// check, so it does not start the next sign-in. Must be an app client logout
// URL (template.yaml).
const SIGNED_OUT_PATH = '/signed-out';

// Cookie attributes must match the library's for a browser to expire its
// cookies; these are the values it is configured with below.
const COOKIE_PATH = '/';
const COOKIE_SAME_SITE = 'Lax';

async function loadConfig() {
  // The Lambda runtime provides the SDK (esbuild leaves it external); the
  // local install does not, so load it here rather than at module top to
  // keep index.js requirable under test.
  const { SSMClient, GetParameterCommand } = require('@aws-sdk/client-ssm');
  const ssm = new SSMClient({ region: REGION });
  const { Parameter } = await ssm.send(new GetParameterCommand({ Name: CONFIG_PARAMETER }));
  return JSON.parse(Parameter.Value);
}

function createAuthenticator(config) {
  // Sign-out goes through Cognito's own logout endpoint, which ends the
  // hosted-UI session; cognito-at-edge alone only revokes tokens and clears
  // this origin's cookies, after which the next request would sign the user
  // straight back in.
  const cognitoLogout = `https://${config.userPoolDomain}/logout?client_id=${config.userPoolAppId}` +
    `&logout_uri=${encodeURIComponent(config.baseUrl + SIGNED_OUT_PATH)}`;

  return new Authenticator({
    region: REGION,
    userPoolId: config.userPoolId,
    userPoolAppId: config.userPoolAppId,
    userPoolDomain: config.userPoolDomain,
    cookieExpirationDays: config.sessionDays,
    httpOnly: true,
    sameSite: COOKIE_SAME_SITE,
    parseAuthPath: PARSE_AUTH_PATH,
    cookiePath: COOKIE_PATH,
    logoutConfiguration: {
      logoutUri: SIGN_OUT_PATH,
      logoutRedirectUri: cognitoLogout,
    },
    // The library logs request bodies on failures, refresh token included;
    // nothing it would say is worth that in CloudWatch.
    logLevel: 'silent',
  });
}

function signedOutResponse() {
  return {
    status: '200',
    statusDescription: 'OK',
    headers: {
      'content-type': [{ key: 'Content-Type', value: 'text/html; charset=utf-8' }],
      'cache-control': [{ key: 'Cache-Control', value: 'no-store' }],
    },
    body: '<!doctype html><title>Signed out</title>' +
      '<p>You are signed out of the e2e reports. Open any report link to sign in again.</p>',
  };
}

function escapeHtml(text) {
  return text.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
}

// The return path the library put in the OAuth state (the originally
// requested path and query, since no CSRF layer wraps it), if it is one this
// origin can serve: an absolute path, not protocol-relative, not the callback
// itself, no control characters.
function safeReturnPath(state) {
  if (typeof state !== 'string' || !state.startsWith('/') || state.startsWith('//'))
    return null;
  if (state.startsWith(PARSE_AUTH_PATH) || /[\x00-\x1f\x7f\\]/.test(state))
    return null;
  return state;
}

// The callback path reached without an authorization code: an OAuth error
// callback (error=..., e.g. after a timeout at Google), or a stray visit.
// Say so and offer a retry at the report that was being opened, which is
// protected, so following the link begins a new sign-in that returns there.
// The root serves nothing (reports live under per-run prefixes), so without
// a usable state there is no link to offer.
function signInFailedResponse(params) {
  const detail = [params.get('error'), params.get('error_description')].filter(Boolean).join(': ');
  const retry = safeReturnPath(params.get('state'));
  return {
    status: '400',
    statusDescription: 'Bad Request',
    headers: {
      'content-type': [{ key: 'Content-Type', value: 'text/html; charset=utf-8' }],
      'cache-control': [{ key: 'Cache-Control', value: 'no-store' }],
    },
    body: '<!doctype html><title>Sign-in did not complete</title>' +
      '<p>Sign-in did not complete' + (detail ? ' (' + escapeHtml(detail) + ')' : '') + '. ' +
      (retry
        ? '<a href="' + escapeHtml(retry) + '">Try again</a>.'
        : 'Reopen the report link to try again.') +
      '</p>',
  };
}

// Lambda@Edge handlers return either the (possibly modified) request, to let
// it through to the origin, or a response.
function isRedirect(result) {
  return result && result.status === '302';
}

// handle() refreshes an expired ID token and then redirects to request.uri
// alone, which for a trace link (trace/index.html?trace=...) loses the trace.
// The library keeps the query string on the first sign-in, so this only
// concerns the refresh path; it is recognized by its redirect target being
// exactly the request's own path.
function restoreQueryString(request, result) {
  if (!request.querystring || !isRedirect(result))
    return result;

  const location = result.headers && result.headers.location && result.headers.location[0];
  const own = 'https://' + request.headers.host[0].value + request.uri;
  if (location && location.value === own)
    location.value = own + '?' + request.querystring;

  return result;
}

// The library keeps one set of token cookies per signed-in user, named
// CognitoIdentityServiceProvider.<app>.<user>.<kind>, and on sign-out clears
// only the last user's, so after two users completed sign-in in one browser
// the other's cookies would keep authorizing requests. Likewise a new
// sign-in leaves the previous user's cookies in place. This expires every
// cookie of this app client that the request carried and the response does
// not already set, with the same attributes the library clears cookies with.
function expireStaleCognitoCookies(request, result, appId) {
  if (!isRedirect(result))
    return result;

  const prefix = `CognitoIdentityServiceProvider.${appId}.`;
  const setCookies = (result.headers && result.headers['set-cookie']) || [];
  const alreadySet = new Set(setCookies.map((h) => h.value.split('=')[0]));
  const carried = (request.headers.cookie || []).flatMap((h) => parseCookies(h.value));
  const stale = [...new Set(carried.map((c) => c.name))]
    .filter((name) => name.startsWith(prefix) && !alreadySet.has(name));
  if (stale.length === 0)
    return result;

  const host = request.headers.host[0].value;
  const attributes = {
    domain: getCookieDomain(host, false, undefined),
    path: COOKIE_PATH,
    expires: new Date(0),
    secure: true,
    httpOnly: true,
    sameSite: COOKIE_SAME_SITE,
  };
  result.headers = result.headers || {};
  result.headers['set-cookie'] = setCookies.concat(
    stale.map((name) => ({ key: 'Set-Cookie', value: serializeCookie(name, '', attributes) })));

  return result;
}

/**
 * Build the handler around a supplier of { authenticator, config } (a
 * function returning a Promise), so tests can substitute a fake.
 */
function createHandler(getAuthenticator) {
  return async (event) => {
    const request = event.Records[0].cf.request;
    if (request.uri === SIGNED_OUT_PATH)
      return signedOutResponse();

    const { authenticator, config } = await getAuthenticator();

    // handle() consults the cookies before the path, so a callback that
    // arrives after another tab already completed sign-in would be passed to
    // S3 as a request for /parseauth. Exchange the code regardless. Without a
    // code, handle() would start a sign-in that returns here, to no code
    // again; answer directly instead.
    if (request.uri === PARSE_AUTH_PATH) {
      const params = new URLSearchParams(request.querystring || '');
      if (params.has('code'))
        return expireStaleCognitoCookies(request, await authenticator.handleParseAuth(event), config.userPoolAppId);
      return signInFailedResponse(params);
    }

    // Signing out with no cookies at all (a second visit to /signout) makes
    // the library's cookie clearing dereference the absent header; give it
    // an empty one. Same prefix match as the library's own sign-out check.
    const signingOut = request.uri.startsWith(SIGN_OUT_PATH);
    if (signingOut && !request.headers.cookie)
      request.headers.cookie = [];

    const result = restoreQueryString(request, await authenticator.handle(event));
    return signingOut ? expireStaleCognitoCookies(request, result, config.userPoolAppId) : result;
  };
}

// Once per container; a failed load is retried on the next request.
let loaded;

function getAuthenticator() {
  if (!loaded) {
    loaded = loadConfig()
      .then((config) => ({ authenticator: createAuthenticator(config), config }))
      .catch((err) => {
        loaded = undefined;
        throw err;
      });
  }

  return loaded;
}

exports.handler = createHandler(getAuthenticator);
exports.createHandler = createHandler;
exports.createAuthenticator = createAuthenticator;
exports.restoreQueryString = restoreQueryString;
exports.expireStaleCognitoCookies = expireStaleCognitoCookies;
