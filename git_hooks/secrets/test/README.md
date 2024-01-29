# Test

Here is an informal, manually run and verified "test" to confirm that the secrets scanning is working as expected.

1. Ensure `detect-secrets` is already installed. See the [secrets README](../README.md#installation) for installation instructions.
2. From this directory, run `detect-secrets scan --no-verify --all-files --exclude-files 'secrets_report_test.json' > secrets.baseline_test`.
3. Confirm that all of the secrets below are flagged as secrets in the generated `secrets.baseline_test` file, with the exception of the `AWS_ACCESS_KEY_SECRET` secret. You can confirm this by doing one of the following:
    - Manually inspecting the `secrets.baseline_test` file to confirm that the expected secrets are listed
    - Running `detect-secrets audit secrets.baseline_test` and confirming that the expected secrets are listed
    - Running `detect-secrets audit --report secrets.baseline_test > secrets_report_test.json` and confirming that the expected secrets are listed in the `secrets_report_test.json` file

---

## Example secrets for secret scanning
All of the secrets below are randomly generated strings that match the expected string format for each secret type.

### Github secrets https://github.blog/2021-04-05-behind-githubs-new-authentication-token-formats
GITHUB_PERSONAL=ghp_65CCDtH2W4OzBcxpUSr31wgbzR73sba1IIxA
GITHUB_OAUTH=gho_AuVpltgWc8g1gi6AZpuSvLiWBH7FDSNZtPZl
GITHUB_USER_TO_SERVER=ghu_TNnUL0oB4edIzcnV5tZDMIFclzxmGATTKgHY
GITHUB_SERVER_TO_USER=ghs_i1kVrdswVFCGSSDvsPZp3C85nhQzifqsTqAS
GITHUB_REFRESH=ghr_wXjrwkALbBMusNhbrN8Kfx8PiW8a6GevVlkA

### AWS secrets https://docs.aws.amazon.com/IAM/latest/UserGuide/id_credentials_access-keys.html
AWS_ACCESS_KEY_ID=AKIA4GD12P9SQQZW8CIN
AWS_ACCESS_KEY_SECRET=BKucTwucNPxCa/I1WDGRG/73aJOHWfQE94HEMZNV
- Note that the `AWS_ACCESS_KEY_SECRET` will only be flagged as an AWS secret if network verification is enabled and the secret is the matching secret for a valid access key id `AWS_ACCESS_KEY_ID`. As such, this secret will not be flagged.
