// Cognito pre-sign-up trigger: refuses any account outside the allowed email
// domain. Google already enforces this for an "Internal" OAuth client; this is
// the second lock, on the side we control.
exports.handler = async (event) => {
  const domain = process.env.ALLOWED_EMAIL_DOMAIN.toLowerCase();
  const email = (event.request.userAttributes.email || '').toLowerCase();
  if (!email.endsWith('@' + domain))
    throw new Error('Sign-in is limited to ' + domain + ' accounts');

  // Google has verified the address; skip Cognito's own confirmation.
  if (event.triggerSource === 'PreSignUp_ExternalProvider') {
    event.response.autoConfirmUser = true;
    event.response.autoVerifyEmail = true;
  }

  return event;
};
