# E2E report viewer

The e2e workflows publish each run's merged Playwright report to the private
S3 bucket `rstudio-playwright-reports` (`os-publish-e2e-report`), and the PR
comment links to it. This directory holds the stack that makes those links
open in a browser for Posit staff: a CloudFront distribution in front of the
bucket whose every request must carry a signed-in session, with Google
Workspace (posit.co) as the identity provider.

    browser --> CloudFront --> Lambda@Edge (viewer request)
                                 | no session: redirect to Cognito hosted UI
                                 |             --> Google sign-in --> back to /parseauth
                                 | session:    pass through
                               S3 (origin access control; bucket stays private)

The first click in a while bounces through Google (and Okta, if Workspace
sign-in is behind it) and lands back on the requested report; after that the
session cookie lasts `SessionDays` (30) and clicks go straight through. The
report's trace viewer works because everything is served same-origin over
HTTPS. Nothing here runs a server: CloudFront, Cognito and Lambda are all
managed, and the only ongoing maintenance is bumping the Lambda runtime when
AWS retires it.

    template.yaml      # SAM template: Cognito + Lambda@Edge + CloudFront + bucket policy
    lambda/            # the edge function (cognito-at-edge, bundled with esbuild)
    presignup/         # Cognito pre-sign-up trigger: posit.co accounts only
    deploy.sh          # package + deploy; reads the Google client secret from SSM

## One-time setup

All of this is done once, by someone with the `ide-build` AWS profile
(account 749683154838) and a posit.co Google account.

1. **Google OAuth client.** In the Google Cloud project `rstudio-e2e-reports`
   (organization posit.co), open *Google Auth Platform* (APIs & Services,
   OAuth consent screen) and configure the app: any name, a support email,
   and audience **Internal**. Internal is what makes Google refuse accounts
   outside posit.co; the stack's pre-sign-up trigger enforces the same rule
   again on the Cognito side. Then create a client of type *Web application*
   with this authorized redirect URI (the stack's `GoogleRedirectUri` output):

       https://rstudio-e2e-reports.auth.us-east-1.amazoncognito.com/oauth2/idpresponse

   Google exposes no API for this step, so it is console-only.

2. **Store the client credentials** where `deploy.sh` reads them. The deploy
   passes them to the stack as `NoEcho` parameters, so they never appear in
   the template, the repo, or stack events:

       aws ssm put-parameter --region us-east-1 --type SecureString \
         --name /rstudio-e2e-reports/google-oauth \
         --value '{"client_id":"<id>.apps.googleusercontent.com","client_secret":"<secret>"}'

3. **Deploy.**

       aws sso login --profile ide-build
       AWS_PROFILE=ide-build .github/e2e-reports/deploy.sh

   The first deploy takes several minutes (CloudFront). The `ReportsBaseUrl`
   output, `https://<id>.cloudfront.net`, is what the publish action prefixes
   report paths with; report URLs look like
   `<ReportsBaseUrl>/pr-<n>/<run>-<attempt>/all-platforms/index.html`. It is
   the `base-url` default in `.github/actions/os-publish-e2e-report/action.yml`,
   so if the distribution is ever recreated (a new stack, a deleted
   distribution), update that default to the new output.

## Operating it

- **Redeploy** after any change here with the same `deploy.sh` command. It
  re-bundles the function, uploads it, and updates the stack; CloudFront
  picks up the new function version automatically (`AutoPublishAlias`).
  Stack parameters other than the Google credentials can be overridden with
  `KEY=VALUE` arguments, e.g. `./deploy.sh SessionDays=14`.
- **Rotate the Google secret** by creating a new one in the Google console,
  updating the SSM parameter (`put-parameter --overwrite`), and redeploying.
- **Sign out** at `<ReportsBaseUrl>/signout`: tokens are revoked, the
  cookies cleared (every user's, should more than one have signed in from
  that browser), and Cognito's own session ended, finishing on a static
  "signed out" page. There is no sign-in page; any report URL signs you in.
  Report responses are sent with `Cache-Control: private, no-store`, so a
  signed-out browser cannot reopen a report from its cache.
- **Tests** for the edge function's own logic (query-string restoration,
  callback routing, the signed-out and sign-in-failed pages) and for the
  pre-sign-up trigger's domain rule run with `npm test` in `lambda/`; the
  library the edge function wraps is replaced by a fake there.
- **Who signed in** is visible in the Cognito user pool (one user per Google
  account, created on first sign-in). Nothing else is stored.
- **Remove it all** with `aws cloudformation delete-stack --stack-name
  rstudio-e2e-reports-viewer`, then delete the SSM parameter and the
  `rstudio-e2e-reports-viewer-deploy-<account>` bucket, and the Google Cloud
  project. The reports bucket is untouched apart from losing its bucket
  policy. Note that CloudFront keeps a deleted Lambda@Edge function's
  replicas for a while, so the stack deletion can take an hour to finish.

Cost at this traffic is a few cents a month: Cognito is free below 10,000
monthly users, and CloudFront and Lambda@Edge bill per request.

## Notes

- Lambda@Edge functions have no environment variables and must live in
  us-east-1, which is why the Cognito details travel through the SSM
  parameter `/rstudio-e2e-reports/viewer` (written by the stack, read once
  per function container) and why `deploy.sh` pins the region.
- Viewer-request functions are limited to 1 MB zipped; `cognito-at-edge`
  with its dependencies is 1.6 MB, so `lambda/` is bundled with esbuild
  (about 100 KB). The AWS SDK is left external; the Node runtime provides it.
- The hosted UI offers Google as the only identity provider, so it forwards
  to Google directly instead of showing a provider chooser.
- If Google Workspace's app access control is set to restrict internal apps,
  an admin would need to allow this one; by default internal apps are trusted.
