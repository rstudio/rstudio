#!/usr/bin/env bash
rm -rf .prettier_and_git_ignores && 
cat <(echo '# Prettier Ignore') .prettierignore <(echo) <(echo)  .gitignore > .prettier_and_git_ignores