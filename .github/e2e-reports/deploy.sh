#!/usr/bin/env bash
#
# Deploys (or updates) the e2e reports viewer stack. See README.md for the
# one-time Google and SSM setup this expects. Arguments are extra stack
# parameter overrides, e.g. SessionDays=14; they are merged with the Google
# credentials below into the one --parameter-overrides the CLI honors.
#
# Usage: AWS_PROFILE=ide-build ./deploy.sh [KEY=VALUE ...]

set -euo pipefail
cd "$(dirname "$0")"

for arg in "$@"; do
  case "$arg" in
    *=*) ;;
    *)
      echo "usage: $0 [KEY=VALUE ...]   (extra stack parameter overrides)" >&2
      exit 2
      ;;
  esac
done

STACK_NAME=${STACK_NAME:-rstudio-e2e-reports-viewer}
GOOGLE_OAUTH_PARAMETER=${GOOGLE_OAUTH_PARAMETER:-/rstudio-e2e-reports/google-oauth}

# Lambda@Edge functions must be created in us-east-1, whatever the viewer's
# region; the reports bucket is there too. Both variables: CLI v2 reads
# AWS_REGION first, older tooling AWS_DEFAULT_REGION.
export AWS_REGION=us-east-1
export AWS_DEFAULT_REGION=us-east-1

ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
DEPLOY_BUCKET=${DEPLOY_BUCKET:-${STACK_NAME}-deploy-${ACCOUNT}}

# The Google client credentials are kept in one SecureString parameter (JSON
# with client_id and client_secret) and handed to the stack as NoEcho
# parameters, so a rotated secret takes effect on the next deploy.
GOOGLE_OAUTH=$(aws ssm get-parameter --name "$GOOGLE_OAUTH_PARAMETER" --with-decryption --query Parameter.Value --output text)
GOOGLE_CLIENT_ID=$(jq -er .client_id <<< "$GOOGLE_OAUTH")
GOOGLE_CLIENT_SECRET=$(jq -er .client_secret <<< "$GOOGLE_OAUTH")

# Viewer-request functions are capped at 1 MB zipped; cognito-at-edge with its
# dependencies is 1.6 MB, bundled it is about 100 KB.
echo "Bundling the edge function..."
(cd lambda && npm ci --no-audit --no-fund --silent && npm run --silent build)

# `cloudformation package` needs a bucket for the function code.
if ! aws s3api head-bucket --bucket "$DEPLOY_BUCKET" > /dev/null 2>&1; then
  echo "Creating deployment bucket $DEPLOY_BUCKET..."
  aws s3api create-bucket --bucket "$DEPLOY_BUCKET" > /dev/null
  aws s3api put-public-access-block --bucket "$DEPLOY_BUCKET" \
    --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
fi

PACKAGED=$(mktemp -t e2e-reports-packaged.XXXXXX)
trap 'rm -f "$PACKAGED"' EXIT
aws cloudformation package \
  --template-file template.yaml \
  --s3-bucket "$DEPLOY_BUCKET" \
  --output-template-file "$PACKAGED" > /dev/null

echo "Deploying $STACK_NAME..."
aws cloudformation deploy \
  --stack-name "$STACK_NAME" \
  --template-file "$PACKAGED" \
  --capabilities CAPABILITY_IAM CAPABILITY_AUTO_EXPAND \
  --no-fail-on-empty-changeset \
  --parameter-overrides \
    "GoogleClientId=$GOOGLE_CLIENT_ID" \
    "GoogleClientSecret=$GOOGLE_CLIENT_SECRET" \
    "$@"
# (The caller's KEY=VALUE arguments extend that one list; a second
# --parameter-overrides would replace it, credentials included.)

aws cloudformation describe-stacks \
  --stack-name "$STACK_NAME" \
  --query 'Stacks[0].Outputs[].[OutputKey,OutputValue]' \
  --output table
