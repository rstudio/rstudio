// Utility methods for Jenkins pipelines

/**
  * Returns true if branch has changes in the specified path with the target branch.
  */
boolean hasChangesIn(String module) {
  sh "echo 'Comparing changes in ${module} with ${env.CHANGE_TARGET}..${env.BRANCH_NAME}'"
  return !env.CHANGE_TARGET ||
  sh(
    returnStatus: true,
    script: "git diff --name-only --quiet `git merge-base origin/${env.BRANCH_NAME} origin/${env.CHANGE_TARGET}`..origin/${env.BRANCH_NAME} -- ${module}") == 1
}

/**
  * Adds a remote reference to the specified branch.
  */
void addRemoteRef(String branchName) {
  sh "git config --add remote.origin.fetch +refs/heads/${branchName}:refs/remotes/origin/${branchName}"
  sh "git fetch --no-tags origin ${branchName}"
}

return this
