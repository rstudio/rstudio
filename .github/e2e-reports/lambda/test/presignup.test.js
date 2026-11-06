// Tests for the Cognito pre-sign-up trigger (../../presignup/index.js): the
// posit.co-only admission rule and the auto-confirmation of federated users.
const { test, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const { handler } = require('../../presignup/index.js');

const EXTERNAL = 'PreSignUp_ExternalProvider';

function signUp(email, triggerSource = EXTERNAL) {
  const attributes = email === undefined ? {} : { email };
  return { triggerSource, request: { userAttributes: attributes }, response: {} };
}

beforeEach(() => {
  process.env.ALLOWED_EMAIL_DOMAIN = 'posit.co';
});

test('admits an address in the allowed domain, whatever its case', async () => {
  for (const email of ['kevin@posit.co', 'Kevin@Posit.CO', 'KEVIN@POSIT.CO']) {
    const result = await handler(signUp(email));
    assert.equal(result.response.autoConfirmUser, true, email);
    assert.equal(result.response.autoVerifyEmail, true, email);
  }
});

test('rejects other domains, lookalikes included', async () => {
  for (const email of [
    'kevin@gmail.com',
    'kevin@notposit.co',
    'kevin@posit.co.evil.com',
    'kevin@posit.com',
    'kevin@posit.co@evil.com',
    'posit.co',
    '',
  ]) {
    await assert.rejects(handler(signUp(email)), /limited to posit\.co accounts/, email);
  }
});

test('rejects a sign-up with no email attribute', async () => {
  await assert.rejects(handler(signUp(undefined)), /limited to posit\.co accounts/);
});

test('matches the domain case-insensitively from the environment', async () => {
  process.env.ALLOWED_EMAIL_DOMAIN = 'Posit.Co';

  const result = await handler(signUp('kevin@posit.co'));

  assert.equal(result.response.autoConfirmUser, true);
});

test('auto-confirms only federated sign-ups', async () => {
  // A non-federated sign-up cannot happen with the pool's settings, but if
  // it did, Cognito's own confirmation should still apply.
  const result = await handler(signUp('kevin@posit.co', 'PreSignUp_SignUp'));

  assert.equal(result.response.autoConfirmUser, undefined);
  assert.equal(result.response.autoVerifyEmail, undefined);
});
