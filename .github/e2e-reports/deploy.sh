#!/usr/bin/env bash
#
# Deploys (or updates) the e2e reports host stack. See README.md for the
# one-time Tailscale auth key setup this expects. Arguments are stack
# parameter overrides, e.g. InstanceType=t4g.small.
#
# Usage: AWS_PROFILE=ide-build ./deploy.sh [KEY=VALUE ...]

set -euo pipefail
cd "$(dirname "$0")"

for arg in "$@"; do
  case "$arg" in
    ?*=*) ;;
    *)
      echo "usage: $0 [KEY=VALUE ...]   (stack parameter overrides)" >&2
      exit 2
      ;;
  esac
done

STACK_NAME=${STACK_NAME:-rstudio-e2e-reports-host}

# The reports bucket is in us-east-1; keeping the host there makes the S3
# reads free. Both variables: CLI v2 reads AWS_REGION first, older tooling
# AWS_DEFAULT_REGION.
export AWS_REGION=us-east-1
export AWS_DEFAULT_REGION=us-east-1

# The AMI parameter resolves to the newest Amazon Linux 2023 image, and a new
# image means a new instance (which must re-enroll in the tailnet, see
# README.md). So it is sent only when the stack is first created, or when the
# caller asks for it with AmiId=...; otherwise the stack keeps its current
# value and a redeploy only changes what the template changed.
overrides=("$@")
if ! aws cloudformation describe-stacks --stack-name "$STACK_NAME" > /dev/null 2>&1; then
  echo "Creating $STACK_NAME..."
elif [[ ! " $* " =~ " AmiId=" ]]; then
  overrides+=("AmiId=$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" \
    --query "Stacks[0].Parameters[?ParameterKey=='AmiId'].ParameterValue" --output text)")
fi

echo "Deploying $STACK_NAME..."
aws cloudformation deploy \
  --stack-name "$STACK_NAME" \
  --template-file template.yaml \
  --capabilities CAPABILITY_IAM \
  --no-fail-on-empty-changeset \
  ${overrides[@]+--parameter-overrides "${overrides[@]}"}

aws cloudformation describe-stacks \
  --stack-name "$STACK_NAME" \
  --query 'Stacks[0].Outputs[].[OutputKey,OutputValue]' \
  --output table
