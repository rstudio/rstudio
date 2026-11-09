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

# Parameter overrides: the caller's KEY=VALUE arguments, plus whatever they
# left out of VpcId, SubnetId, AmiId and Owner.
overrides=("$@")
given() { [[ " $* " == *" $1="* ]]; }
value_of() { for arg in "${@:2}"; do [[ $arg == "$1="* ]] && printf '%s' "${arg#*=}"; done; }

# The network is the account's shared VPC (an organization policy forbids
# creating one) and its private-0 subnet (template.yaml says why). Looked up
# by tag on every deploy, so a different subnet means a different instance.
if ! given VpcId "$@"; then
  VPC_ID=$(aws ec2 describe-vpcs --filters Name=tag:Name,Values=shared \
    --query 'Vpcs[0].VpcId' --output text)
  if [[ $VPC_ID == None ]]; then
    echo "Could not find the shared VPC; pass VpcId=..." >&2
    exit 1
  fi
  overrides+=("VpcId=$VPC_ID")
else
  VPC_ID=$(value_of VpcId "$@")
fi
if ! given SubnetId "$@"; then
  SUBNET_ID=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" Name=tag:Name,Values=private-0 \
    --query 'Subnets[0].SubnetId' --output text)
  if [[ $SUBNET_ID == None ]]; then
    echo "Could not find the private-0 subnet in $VPC_ID; pass SubnetId=..." >&2
    exit 1
  fi
  overrides+=("SubnetId=$SUBNET_ID")
fi

# The image: the newest Amazon Linux 2023 arm64 on the first deploy, then
# whatever the stack already runs, because a new image means a new instance
# that has to re-enroll in the tailnet (README.md). AmiId=ami-... forces one.
STACK_EXISTS=true
aws cloudformation describe-stacks --stack-name "$STACK_NAME" > /dev/null 2>&1 || STACK_EXISTS=false
if ! given AmiId "$@"; then
  if [[ $STACK_EXISTS == true ]]; then
    # ResolvedValue is set when the stack was created with the SSM alias as
    # the parameter type (the first version of this template); otherwise the
    # value is the concrete id already.
    AMI_ID=$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" \
      --query "Stacks[0].Parameters[?ParameterKey=='AmiId'] | [0].[ResolvedValue, ParameterValue]" --output text \
      | tr '\t' '\n' | grep -m1 '^ami-')
  else
    AMI_ID=$(aws ssm get-parameter --name /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-arm64 \
      --query Parameter.Value --output text)
  fi
  overrides+=("AmiId=$AMI_ID")
fi

# rs:owner is the deploying SSO identity (the last path segment of the
# assumed-role ARN is the user's email) unless given as Owner=...; kept
# afterwards, since the tag is about who set the host up.
if ! given Owner "$@"; then
  if [[ $STACK_EXISTS == true ]]; then
    OWNER=$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" \
      --query "Stacks[0].Parameters[?ParameterKey=='Owner'].ParameterValue" --output text)
  else
    OWNER=$(aws sts get-caller-identity --query Arn --output text)
    OWNER=${OWNER##*/}
    if [[ $OWNER != *@* ]]; then
      echo "Could not derive an email from the caller identity ($OWNER); pass Owner=you@posit.co" >&2
      exit 1
    fi
  fi
  overrides+=("Owner=$OWNER")
fi

[[ $STACK_EXISTS == true ]] || echo "Creating $STACK_NAME..."
echo "Deploying $STACK_NAME..."
aws cloudformation deploy \
  --stack-name "$STACK_NAME" \
  --template-file template.yaml \
  --capabilities CAPABILITY_IAM \
  --no-fail-on-empty-changeset \
  --parameter-overrides "${overrides[@]}"

aws cloudformation describe-stacks \
  --stack-name "$STACK_NAME" \
  --query 'Stacks[0].Outputs[].[OutputKey,OutputValue]' \
  --output table
