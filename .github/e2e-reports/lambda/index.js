// Viewer-request handler for the e2e reports CloudFront distribution. Every
// request must carry a valid Cognito session (cookies set after Google
// sign-in); anything else is redirected to the hosted UI, which sends the
// browser to Google and back. cognito-at-edge does all of that; this file
// only configures it.
const { Authenticator } = require('cognito-at-edge');
const { SSMClient, GetParameterCommand } = require('@aws-sdk/client-ssm');

// Lambda@Edge functions cannot carry environment variables, so the stack
// publishes the pool details under this name (ViewerConfig in template.yaml).
const CONFIG_PARAMETER = '/rstudio-e2e-reports/viewer';
const REGION = 'us-east-1';

let authenticator;

async function createAuthenticator() {
  const ssm = new SSMClient({ region: REGION });
  const { Parameter } = await ssm.send(new GetParameterCommand({ Name: CONFIG_PARAMETER }));
  const config = JSON.parse(Parameter.Value);

  return new Authenticator({
    region: REGION,
    userPoolId: config.userPoolId,
    userPoolAppId: config.userPoolAppId,
    userPoolDomain: config.userPoolDomain,
    cookieExpirationDays: config.sessionDays,
    httpOnly: true,
    sameSite: 'Lax',
    // Fixed callback path, so the app client's callback URL can be exact. The
    // originally requested URL rides along in the OAuth state and is restored
    // after sign-in.
    parseAuthPath: '/parseauth',
    cookiePath: '/',
    logoutConfiguration: {
      logoutUri: '/signout',
      logoutRedirectUri: config.baseUrl + '/',
    },
    logLevel: 'warn',
  });
}

exports.handler = async (event) => {
  // Once per container; a failed load is retried on the next request.
  if (!authenticator)
    authenticator = await createAuthenticator();

  return authenticator.handle(event);
};
