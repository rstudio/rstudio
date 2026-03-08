# git hooks

## Implemented Hooks
- [pre-commit](./hooks/pre-commit)
    - runs linting and formatting on staged JavaScript and TypeScript files
- [pre-push](./hooks/pre-push): prevents pushes to the main branch

## Install Hooks
The `set-up-git-hooks` script will copy the implemented hooks in [git_hooks/hooks](./hooks/) to [.git/hooks](/.git/hooks/) and make the hooks executable.

💡 Running on Windows? Run the commands from a `Git Bash` terminal.

From this directory (`git_hooks`), use the commands below to make the script executable and then run [set-up-git-hooks](./set-up-git-hooks).

> [!NOTE]
> If you have any existing hooks with the same name in your local project, they will be renamed with the current timestamp appended to the end of the file name.
> Run `./set-up-git-hooks --overwrite` to overwrite existing hooks.

```sh
chmod +x ./set-up-git-hooks
./set-up-git-hooks
```

### Install Specific Hooks
Currently, all implemented hooks are installed by default. To install specific hooks, ensure you've already run the `set-up-git-hooks` script, then navigate to [.git/hooks](/.git/hooks/) and manually update the `pre-commit` and `pre-push` scripts to only run the hooks you want.

In the future, we should add a way to specify which hooks to install when running the `set-up-git-hooks` script.

## Uninstall Hooks
Go to [.git/hooks](/.git/hooks/) and delete the file for the hook you want to uninstall.
- eg. If you want to uninstall the `pre-commit` hook, delete the `.git/hooks/pre-commit` file.

> If you are using VSCode and don't see the `.git` folder in your file explorer, go to VSCode settings and search for "exclude". You may need to remove `.git` from the exclude list for the file explorer.

## Skip Hooks
Hooks can be skipped by appending `--no-verify` to the git command you want to run without its corresponding hook.
- eg. To skip the pre-commit hook, you'd use `git commit --no-verify`.
