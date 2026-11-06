// Viewer-request handler for the e2e reports CloudFront distribution. Every
// request must carry a valid Cognito session (cookies set after Google
// sign-in); anything else is redirected to the hosted UI, which sends the
// browser to Google and back. cognito-at-edge does all of that; this file
// configures it and papers over three gaps in its handle() (see createHandler).
const { Authenticator } = require('cognito-at-edge');

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
    sameSite: 'Lax',
    parseAuthPath: PARSE_AUTH_PATH,
    cookiePath: '/',
    logoutConfiguration: {
      logoutUri: SIGN_OUT_PATH,
      logoutRedirectUri: cognitoLogout,
    },
    logLevel: 'warn',
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

function hasAuthCode(querystring) {
  return new URLSearchParams(querystring || '').has('code');
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

/**
 * Build the handler around an authenticator supplier (a function returning a
 * Promise of { authenticator }), so tests can substitute a fake.
 */
function createHandler(getAuthenticator) {
  return async (event) => {
    const request = event.Records[0].cf.request;
    if (request.uri === SIGNED_OUT_PATH)
      return signedOutResponse();

    const { authenticator } = await getAuthenticator();

    // handle() consults the cookies before the path, so a callback that
    // arrives after another tab already completed sign-in would be passed to
    // S3 as a request for /parseauth. Exchange the code regardless.
    if (request.uri === PARSE_AUTH_PATH && hasAuthCode(request.querystring))
      return authenticator.handleParseAuth(event);

    return restoreQueryString(request, await authenticator.handle(event));
  };
}

// Once per container; a failed load is retried on the next request.
let loaded;

function getAuthenticator() {
  if (!loaded) {
    loaded = loadConfig()
      .then((config) => ({ authenticator: createAuthenticator(config) }))
      .catch((err) => {
        loaded = undefined;
        throw err;
      });
  }

  return loaded;
}

exports.handler = createHandler(getAuthenticator);
exports.createHandler = createHandler;
exports.restoreQueryString = restoreQueryString;
