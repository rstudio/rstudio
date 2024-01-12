# Secrets Scanning

We use [detect-secrets](https://github.com/Yelp/detect-secrets) to scan for possible secrets in staged files.

For more information on how to use detect-secrets, see the [detect-secrets documentation](https://github.com/Yelp/detect-secrets).

## Pre-commit hook
The pre-commit hook will run `detect-secrets scan` on staged files and fail if any secrets are found (if the secrets are not already in the baseline secrets file).

If you're committing changes that change the line number of a previously detected secret in the baseline file, `detect-secrets` will automatically update the baseline file with the new line number and fail the commit so you can add the updated baseline file to your commit.

### Example

`my_secret` on line 2 is already captured in the baseline secrets file.
```js
const hello = "hello";         // line 1
const my_secret = "my_secret"  // line 2
```

If `puppies` is inserted at line 2, `detect-secrets` will fail the commit and update the baseline secrets file to list `my_secret` on line 3. You can then add the updated baseline secrets file to your commit.
```diff
const hello = "hello";         // line 1
+ const puppies = "puppies";   // line 2
const my_secret = "my_secret"  // line 3
```

## False positives
If you are receiving false positives from the pre-commit hook, you can update the baseline secrets file to mark the detected "secrets" as okay to commit.

### Updating the baseline secrets file
From the root of the project:
1. Run `detect-secrets scan --no-verify --no-verify --exclude-files 'src/cpp/ext/.*' --exclude-files 'dependencies/submodules/.*' --baseline git_hooks/secrets/.secrets.baseline` to scan for new secrets and update the baseline secrets file:
```bash
detect-secrets scan --no-verify --no-verify --exclude-files src/cpp/ext/.* --exclude-files dependencies/submodules/.* --baseline git_hooks/secrets/.secrets.baseline
``` 
2. Run `detect-secrets audit git_hooks/secrets/.secrets.baseline` to audit the baseline secrets file (flag each secret as either true or false positive)
3. Commit the updated baseline secrets file

### Additional ways to handle false positives
Aside from adding false positives to the baseline secrets file, inline comments can be added to ignore secrets in specific lines of code. Additionally, more advanced configuration and filtering on words is possible. See the [detect-secrets documentation](https://github.com/Yelp/detect-secrets/tree/master?tab=readme-ov-file#inline-allowlisting) for more information.

## Report of secrets found

To generate a report of secrets found, run `detect-secrets audit --report git_hooks/secrets/.secrets.baseline > git_hooks/secrets/secrets_report.json`.
- `secrets_report.json` will not be committed as it is `.gitignore`d

## Filtering secrets

We currently only use the built-in filtering mechanism `--exclude-files` to filter out secrets in the `src/cpp/ext` and `dependencies/submodules` directories. These directories contain third-party code that we do not want to scan for secrets.

For more on filters, see the [detect-secrets README](https://github.com/Yelp/detect-secrets/tree/master?tab=readme-ov-file#filters) or further details on writing [custom filters](https://github.com/Yelp/detect-secrets/blob/master/docs/filters.md#Using-Your-Own-Filters).

---

<details>
<summary>Initial Setup (only needed once)</summary>

It's best to refer to [detect-secrets](https://github.com/Yelp/detect-secrets) for the most up-to-date instructions, but here are the steps that were used to set up the initial baseline secrets file:
1. Install detect-secrets via `pip install detect-secrets` (Python and pip installed already) or `brew install detect-secrets` (MacOS)
2. Run `detect-secrets scan --no-verify --exclude-files 'src/cpp/ext/.*' --exclude-files 'dependencies/submodules/.*' > git_hooks/secrets/.secrets.baseline` to generate the initial baseline secrets file
    - `--no-verify` is used to skip additional secret verification via a network call
3. Run `detect-secrets audit git_hooks/secrets/.secrets.baseline` to audit the baseline secrets file (flag each secret as either true or false positive)
4. Commit the baseline secrets file

</details>
