# E2E report host

The e2e workflows publish each run's merged Playwright report to the private
S3 bucket `rstudio-playwright-reports` (`os-publish-e2e-report`), and the PR
comment links to it. This directory holds the stack that makes those links
open in a browser for Posit staff: one small EC2 instance on the Posit
tailnet that mounts the bucket read-only and serves it with nginx behind
`tailscale serve`. Only tailnet members can resolve or reach it; nothing is
exposed on the public internet, and no AWS service in front of the bucket
(CloudFront, API Gateway) is involved.

    browser (on Tailscale) --> https://e2e-reports.tail00da2.ts.net/
                                 | tailscale serve: TLS with a Tailscale-issued certificate
                               nginx (127.0.0.1:8080)
                                 | static files, directory listings
                               /srv/reports  = the bucket, mounted read-only (Mountpoint for Amazon S3)

Clicking a report link works as long as the Tailscale client is running;
without it the hostname does not even resolve. The report's trace viewer works
because everything is served same-origin over HTTPS.

    template.yaml      # CloudFormation: instance + role + security group; the host configures itself from user data
    deploy.sh          # create or update the stack

## One-time setup

All of this is done once, by someone with the `ide-build` AWS profile
(account 749683154838) and a Posit Tailscale account.

1. **Tailscale enrollment.** Two ways, depending on who you are in the
   tailnet:

   - *An admin generates an auth key* (admin console, *Settings*, *Keys*):
     reusable, pre-approved, tagged `tag:e2e-reports`, the longest expiry
     offered. Store it where the host reads it at first boot: an SSM
     SecureString, read by the instance role, never part of the template,
     the repo, the user data, or the stack events:

         aws ssm put-parameter --region us-east-1 --type SecureString \
           --name /rstudio-e2e-reports/tailscale-authkey \
           --value tskey-auth-...

     A tagged node has no key expiry, and a stored key lets a replacement
     instance enroll unattended. This is the way to run it long term.

   - *No admin at hand*: skip the parameter. The host then starts a login
     and prints the URL to authorize it (`/var/log/e2e-reports-setup.log`,
     or the Session Manager command below). Opening it in a browser as any
     posit.co tailnet member adds the node under that account; the setup
     script finishes on the next boot, or right away with the second command:

         aws ssm send-command --instance-ids <InstanceId> --document-name AWS-RunShellScript \
           --parameters 'commands=["tailscale status --json | jq -r .AuthURL"]'
         aws ssm send-command --instance-ids <InstanceId> --document-name AWS-RunShellScript \
           --parameters 'commands=["tailscale set --auto-update","tailscale serve --bg 8080"]'

     (`aws ssm get-command-invocation` shows a command's output.) A node
     enrolled this way is owned by whoever signed in and expires after 180
     days unless an admin disables key expiry for it, so ask for that, or
     for the tag.

2. **Tailscale ACLs.** The default policy lets every member reach every
   node; nothing to do unless the tailnet's policy is stricter, in which
   case the node (or its tag) needs a rule allowing tcp/443 from members.

3. **Deploy.**

       aws sso login --profile ide-build
       AWS_PROFILE=ide-build .github/e2e-reports/deploy.sh

   The stack is ready in about two minutes, the host a minute or two after
   that (package installs, enrollment). The `ReportsBaseUrl` output is what
   the publish action prefixes report paths with; report URLs look like
   `<ReportsBaseUrl>/pr-<n>/<run>-<attempt>/all-platforms/index.html`. It is
   the `base-url` default in `.github/actions/os-publish-e2e-report/action.yml`,
   so if the hostname or tailnet ever changes, update that default too.

## Operating it

- **Redeploy** after a template change with the same `deploy.sh` command.
  Stack parameters can be overridden with `KEY=VALUE` arguments, e.g.
  `./deploy.sh InstanceType=t4g.small`. The AMI is pinned to what the stack
  was created with (a new image would replace the instance); `AmiId=<ami>`
  forces a new one.
- **User data runs on every boot** and is idempotent, so a change to the
  host's configuration (nginx, the mount, Tailscale settings) is a template
  change followed by `deploy.sh` and a stop/start of the instance
  (`aws ec2 stop-instances` / `start-instances` with the `InstanceId`
  output). No replacement, no re-enrollment.
- **Replacing the instance** (new AMI, or a terminated host) creates a new
  tailnet node. Remove the old node first (admin console, or the Tailscale
  app's machine list for a node you own), or Tailscale gives the new one the
  name `e2e-reports-1` and the links break. The new host enrolls with the
  SSM key if one is stored, else it waits to be authorized as at setup.
- **Shell access** is through Session Manager, there is no SSH:
  `aws ssm start-session --target <InstanceId>`. The setup log is
  `/var/log/e2e-reports-setup.log`; the services are `reports-mount`,
  `nginx` and `tailscaled`; `tailscale serve status` shows the mapping.
- **Updates**: `dnf-automatic` applies security updates daily and Tailscale
  updates itself (`tailscale set --auto-update`). A kernel update takes
  effect at the next stop/start.
- **Who can see the reports** is whoever the tailnet ACLs let reach this
  node; the host does no authentication of its own. Sign-in and offboarding
  are Tailscale's.
- **Remove it all** with `aws cloudformation delete-stack --stack-name
  rstudio-e2e-reports-host`, then delete the SSM parameter and the node in
  the Tailscale admin console. The reports bucket is untouched.

Cost: the instance (`t4g.micro`, about $6 a month), its 8 GB volume, and
the shared VPC's NAT gateway processing charge on what the host sends to
browsers. Reads from the bucket go through the VPC's S3 gateway endpoint in
the same region, so they are free.

## Notes

- The bucket is mounted with [Mountpoint for Amazon S3](https://github.com/awslabs/mountpoint-s3)
  rather than proxied, so nginx serves it like any directory: `index.html`
  for directories, `autoindex` listings elsewhere, content types from file
  extensions. New reports appear as soon as the upload finishes; Mountpoint
  caches directory listings only briefly.
- The host lives in a private subnet of the account's shared VPC (an
  organization policy forbids creating VPCs): no public address, a security
  group with no inbound rules, outbound through the VPC's NAT gateway. nginx
  binds to localhost and `tailscale serve` is the only way in; Tailscale
  reaches it through NAT traversal or its relays.
- The user data is a cloud-init multipart document whose `cloud_final_modules`
  setting makes the script run on every boot. It is written without `${...}`
  shell syntax because the template's `Fn::Sub` would try to resolve it.
- The instance uses IMDSv2 only, an encrypted gp3 root volume, and an
  instance role limited to reading the bucket and the auth key parameter,
  plus Session Manager.
- The account's organization policy refuses `ec2:RunInstances` without
  `rs:owner` and `rs:project` tags on the instance, and `ec2:CreateVpc`
  altogether. The template tags everything with the account's `rs:*`
  convention (`rs:owner` is whoever ran the first deploy, or `Owner=...`).
