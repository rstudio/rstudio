/*
 * ViewVcsConstants.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.workbench.views.vcs;

public interface ViewVcsConstants extends com.google.gwt.i18n.client.Messages{
    /**
     * Translated "Add".
     *
     * @return translated "Add"
     */
    String addCapitalized();

    /**
     * Translated "Remote Name:".
     *
     * @return translated "Remote Name:"
     */
    String remoteNameColon();

    /**
     * Translated "Remote URL:".
     *
     * @return translated "Remote URL:"
     */
    String remoteUrlColon();

    /**
     * Translated "Switch branch".
     *
     * @return translated "Switch branch"
     */
    String switchBranch();

    /**
     * Translated "Search by branch name".
     *
     * @return translated "Search by branch name"
     */
    String searchByBranchName();

    /**
     * Translated "(Remote: {0})".
     *
     * @return translated "(Remote: {0})"
     */
    String remoteBranchCaption(String caption);

    /**
     * Translated "(no branch)".
     *
     * @return translated "(no branch)"
     */
    String noBranchParentheses();

    /**
     * Translated "(no branches available)".
     *
     * @return translated "(no branches available)"
     */
    String noBranchesAvailableParentheses();

    /**
     * Translated "(local branches)".
     *
     * @return translated "(local branches)"
     */
    String localBranchesParentheses();

    /**
     * Translated "Create".
     *
     * @return translated "Create"
     */
    String createCapitalized();

    /**
     * Translated "Remote:".
     *
     * @return translated "Remote:"
     */
    String remoteColon();

    /**
     * Translated "Add Remote...".
     *
     * @return translated "Add Remote..."
     */
    String addRemoteEllipses();

    /**
     * Translated "Add Remote".
     *
     * @return translated "Add Remote"
     */
    String addRemote();

    /**
     * Translated "Sync branch with remote".
     *
     * @return translated "Sync branch with remote"
     */
    String syncBranchWithRemote();

    /**
     * Translated "Branch Name:".
     *
     * @return translated "Branch Name:"
     */
    String branchNameColon();

    /**
     * Translated "New Branch".
     *
     * @return translated "New Branch"
     */
    String newBranchCapitalized();

    /**
     * Translated "A local branch named ''{0}'' already exists. Would you like to check out that branch, or overwrite it?".
     *
     * @return translated "A local branch named ''{0}'' already exists. Would you like to check out that branch, or overwrite it?"
     */
    String localBranchAlreadyExists(String branchName);

    /**
     * Translated "Checkout".
     *
     * @return translated "Checkout"
     */
    String checkoutCapitalized();

    /**
     * Translated "Overwrite".
     *
     * @return translated "Overwrite"
     */
    String overwriteCapitalized();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    String cancelCapitalized();

    /**
     * Translated "Local Branch Already Exists".
     *
     * @return translated "Local Branch Already Exists"
     */
    String localBranchAlreadyExistsCaption();

    /**
     * Translated "A remote branch named ''{0}'' already exists on the remote repository ''{1}''. Would you like to check out that branch?".
     *
     * @return translated "A remote branch named ''{0}'' already exists on the remote repository ''{1}''. Would you like to check out that branch?"
     */
    String remoteBranchNameAlreadyExists(String remoteBranch, String remote);

    /**
     * Translated "Remote Branch Already Exists".
     *
     * @return translated "Remote Branch Already Exists"
     */
    String remoteBranchAlreadyExistsCaption();

    /**
     * Translated "(all branches)".
     *
     * @return translated "(all branches)"
     */
    String allBranchesParentheses();

    /**
     * Translated "Close".
     *
     * @return translated "Close"
     */
    String closeCapitalized();

    /**
     * Translated "Stop".
     *
     * @return translated "Stop"
     */
    String stopCapitalized();

    /**
     * Translated "Progress details".
     *
     * @return translated "Progress details"
     */
    String progressDetails();

    /**
     * Translated "Commits {0}".
     *
     * @return translated "Commits {0}"
     */
    String commitsPager(String wordText);

    /**
     * Translated "{0} selection".
     * Empty String so there can be leading space
     * @return translated "{0} selection"
     */
    String selectionSuffix(String emptyString);

    /**
     * Translated "{0} line".
     * Empty String so there can be leading space
     * @return translated "{0} line"
     */
    String lineSuffix(String emptyString);

    /**
     * Translated "{0} chunk".
     * Empty String so there can be leading space
     * @return translated "{0} chunk"
     */
    String chunkSuffix(String emptyString);

    /**
     * Translated "(No commit selected)".
     *
     * @return translated "(No commit selected)"
     */
    String noCommitSelectedParentheses();

    /**
     * Translated "(all commits)".
     *
     * @return translated "(all commits)"
     */
    String allCommitsParentheses();

    /**
     * Translated "Filter by File...".
     *
     * @return translated "Filter by File..."
     */
    String filterByFileEllipses();

    /**
     * Translated "Choose File".
     *
     * @return translated "Choose File"
     */
    String chooseFileCapitalized();

    /**
     * Translated "Filter by Directory...".
     *
     * @return translated "Filter by Directory..."
     */
    String filterByDirectoryEllipses();

    /**
     * Translated "Choose Folder".
     *
     * @return translated "Choose Folder"
     */
    String chooseFolderCapitalized();

    /**
     * Translated "Filter: {0}".
     *
     * @return translated "Filter: {0}"
     */
    String filterColonPath(String path);

    /**
     * Translated "Filter: (None)".
     *
     * @return translated "Filter: (None)"
     */
    String filterColonNone();

    /**
     * Translated "View file @ {0}".
     *
     * @return translated "View file @ {0}"
     */
    String viewFileAtString(String substr);

    /**
     * Translated "Error Fetching History".
     *
     * @return translated "Error Fetching History"
     */
    String errorFetchingHistory();

    /**
     * Translated "History".
     *
     * @return translated "History"
     */
    String historyCapitalized();

    /**
     * Translated "Changes".
     *
     * @return translated "Changes"
     */
    String changesCapitalized();

    /**
     * Translated "Search version control history".
     *
     * @return translated "Search version control history"
     */
    String searchVersionControlHistory();

    /**
     * Translated "Search".
     *
     * @return translated "Search"
     */
    String searchCapitalized();

    /**
     * Translated "Reading file...".
     *
     * @return translated "Reading file..."
     */
    String readingFileEllipses();

    /**
     * Translated "Show History".
     *
     * @return translated "Show History"
     */
    String showHistoryCapitalized();

    /**
     * Translated "Show Diff".
     *
     * @return translated "Show Diff"
     */
    String showDiffCapitalized();

    /**
     * Translated "commit depth {0}".
     *
     * @return translated "commit depth {0}"
     */
    String commitDepthAltText(int nexusColumn);

    /**
     * Translated "Your branch is ahead of ''{0}'' by {1} commit.".
     *
     * @return translated "Your branch is ahead of ''{0}'' by {1} commit."
     */
    String branchAheadOfRemoteSingular(String remoteName, int commitBehind);

    /**
     * Translated "Your branch is ahead of ''{0}'' by {1} commits.".
     *
     * @return translated "Your branch is ahead of ''{0}'' by {1} commits."
     */
    String branchAheadOfRemotePlural(String remoteName, int commitBehind);

    /**
     * Translated "Status".
     *
     * @return translated "Status"
     */
    String statusCapitalized();

    /**
     * Translated "Path".
     *
     * @return translated "Path"
     */
    String pathCapitalized();

    /**
     * Translated "Subject".
     *
     * @return translated "Subject"
     */
    String subjectCapitalized();

    /**
     * Translated "Author".
     *
     * @return translated "Author"
     */
    String authorCapitalized();

    /**
     * Translated "Date".
     *
     * @return translated "Date"
     */
    String dateCapitalized();

    /**
     * Translated "Staged".
     *
     * @return translated "Staged"
     */
    String stagedCapitalized();

    /**
     * Translated "Git Tab".
     *
     * @return translated "Git Tab"
     */
    String gitTabCapitalized();

    /**
     * Translated "Pull options".
     *
     * @return translated "Pull options"
     */
    String pullOptions();

    /**
     * Translated "More".
     *
     * @return translated "More"
     */
    String moreCapitalized();

    /**
     * Translated "Refresh Now".
     *
     * @return translated "Refresh Now"
     */
    String refreshNowCapitalized();

    /**
     * Translated "Refresh options".
     *
     * @return translated "Refresh options"
     */
    String refreshOptions();

    /**
     * Translated "Pull".
     *
     * @return translated "Pull"
     */
    String pullCapitalized();

    /**
     * Translated "Push".
     *
     * @return translated "Push"
     */
    String pushCapitalized();

    /**
     * Translated "No Changes to File".
     *
     * @return translated "No Changes to File"
     */
    String noChangesToFile();

    /**
     * Translated "There are no changes to the file \"{0}\" to diff.".
     *
     * @return translated "There are no changes to the file \"{0}\" to diff."
     */
    String noChangesToFileToDiff(String filename);

    /**
     * Translated "No Changes to Revert".
     *
     * @return translated "No Changes to Revert"
     */
    String noChangesToRevert();

    /**
     * Translated "There are no changes to the file \"{0}\" to revert.".
     *
     * @return translated "There are no changes to the file \"{0}\" to revert."
     */
    String noChangesToFileToRevert(String filename);

    /**
     * Translated "Revert Changes".
     *
     * @return translated "Revert Changes"
     */
    String revertChangesCapitalized();

    /**
     * Translated "Changes to the selected file will be lost, including staged changes.\n\nAre you sure you want to continue?".
     *
     * @return translated "Changes to the selected file will be lost, including staged changes.\n\nAre you sure you want to continue?"
     */
    String changesToFileWillBeLost();

    /**
     * Translated "Changes to the selected files will be lost, including staged changes.\n\nAre you sure you want to continue?".
     *
     * @return translated "Changes to the selected files will be lost, including staged changes.\n\nAre you sure you want to continue?"
     */
    String changesToFileWillBeLostPlural();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    String errorCapitalized();

    /**
     * Translated "Unable to view {0} on GitHub.\n\nAre you sure that this file is on GitHub and is contained in the currently active project?".
     *
     * @return translated "Unable to view {0} on GitHub.\n\nAre you sure that this file is on GitHub and is contained in the currently active project?"
     */
    String unableToViewPathOnGithub(String path);

    /**
     * Translated "Git Ignore".
     *
     * @return translated "Git Ignore"
     */
    String gitIgnoreCapitalized();

    /**
     * Translated "Diff".
     *
     * @return translated "Diff"
     */
    String diffCapitalized();

    /**
     * Translated "Commit".
     *
     * @return translated "Commit"
     */
    String commitCapitalized();

    /**
     * Translated "Git Review".
     *
     * @return translated "Git Review"
     */
    String gitReviewCapitalized();

    /**
     * Translated "Git Diff".
     *
     * @return translated "Git Diff"
     */
    String gitDiffCapitalized();

    /**
     * Translated "Stage".
     *
     * @return translated "Stage"
     */
    String stageCapitalized();

    /**
     * Translated "Revert".
     *
     * @return translated "Revert"
     */
    String revertCapitalized();

    /**
     * Translated "Ignore".
     *
     * @return translated "Ignore"
     */
    String ignoreCapitalized();

    /**
     * Translated "Stage All".
     *
     * @return translated "Stage All"
     */
    String stageAllCapitalized();

    /**
     * Translated "Discard All".
     *
     * @return translated "Discard All"
     */
    String discardAllCapitalized();

    /**
     * Translated "Unstage All".
     *
     * @return translated "Unstage All"
     */
    String unstageAllCapitalized();

    /**
     * Translated "Revert...".
     *
     * @return translated "Revert..."
     */
    String revertEllipses();

    /**
     * Translated "Ignore...".
     *
     * @return translated "Ignore..."
     */
    String ignoreEllipses();

    /**
     * Translated "Open File".
     *
     * @return translated "Open File"
     */
    String openFileCapitalized();

    /**
     * Translated "{0} characters".
     * Character as in a letter in a string.
     * @return translated "{0} characters"
     */
    String lengthCharacters(int length);

    /**
     * Translated "{0} characters in message".
     *
     * @return translated "{0} characters in message"
     */
    String lengthCharactersInMessage(int length);

    /**
     * Translated "Unstage".
     *
     * @return translated "Unstage"
     */
    String unstageCapitalized();

    /**
     * Translated "Discard".
     *
     * @return translated "Discard"
     */
    String discardCapitalized();

    /**
     * Translated "All unstaged changes in this file will be lost.\n\nAre you sure you want to continue?".
     *
     * @return translated "All unstaged changes in this file will be lost.\n\nAre you sure you want to continue?"
     */
    String allUnstagedChangesWillBeLost();

    /**
     * Translated "The selected changes in this file will be lost.\n\nAre you sure you want to continue?".
     *
     * @return translated "The selected changes in this file will be lost.\n\nAre you sure you want to continue?"
     */
    String theSelectedChangesWillBeLost();

    /**
     * Translated "Some of the files to be committed are quite large (>{0} in size). Are you sure you want to commit these files?".
     *
     * @return translated "Some of the files to be committed are quite large (>{0} in size). Are you sure you want to commit these files?"
     */
    String someFilesAreQuiteLarge(String prettySize);

    /**
     * Translated "Committing Large Files".
     *
     * @return translated "Committing Large Files"
     */
    String committingLargeFiles();

    /**
     * Translated "Diff Error".
     *
     * @return translated "Diff Error"
     */
    String diffError();

    /**
     * Translated "Changelist".
     *
     * @return translated "Changelist"
     */
    String changeList();

    /**
     * Translated "SVN Ignore".
     *
     * @return translated "SVN Ignore"
     */
    String svnIgnore();

    /**
     * Translated "svn:ignore".
     *
     * @return translated "svn:ignore"
     */
    String svnColonIgnore();

    /**
     * Translated "SVN Add".
     *
     * @return translated "SVN Add"
     */
    String svnAdd();

    /**
     * Translated "SVN Delete".
     *
     * @return translated "SVN Delete"
     */
    String svnDelete();

    /**
     * Translated "Resolve".
     *
     * @return translated "Resolve"
     */
    String resolveCapitalized();

    /**
     * Translated "SVN Resolve".
     *
     * @return translated "SVN Resolve"
     */
    String svnResolve();

    /**
     * Translated "None of the selected paths appear to have conflicts.\n\nDo you want to resolve anyway?".
     *
     * @return translated "None of the selected paths appear to have conflicts.\n\nDo you want to resolve anyway?"
     */
    String noneOfSelectedPathsHaveConflicts();

    /**
     * Translated "The selected path does not appear to have conflicts.\n\nDo you want to resolve anyway?".
     *
     * @return translated "The selected path does not appear to have conflicts.\n\nDo you want to resolve anyway?"
     */
    String selectedPathDoesNotAppearToHaveConflicts();

    /**
     * Translated "No Conflicts Detected".
     *
     * @return translated "No Conflicts Detected"
     */
    String noConflictsDetected();

    /**
     * Translated "Changes to the selected file will be reverted.\n\nAre you sure you want to continue?".
     *
     * @return translated "Changes to the selected file will be reverted.\n\nAre you sure you want to continue?"
     */
    String changesToSelectedFileWillBeReverted();

    /**
     * Translated "Changes to the selected files will be reverted.\n\nAre you sure you want to continue?".
     *
     * @return translated "Changes to the selected files will be reverted.\n\nAre you sure you want to continue?"
     */
    String changesToSelectedFileWillBeRevertedPlural();

    /**
     * Translated "SVN Revert".
     *
     * @return translated "SVN Revert"
     */
    String svnRevert();

    /**
     * Translated "SVN Tab".
     *
     * @return translated "SVN Tab"
     */
    String svnTab();

    /**
     * Translated "SVN Cleanup".
     *
     * @return translated "SVN Cleanup"
     */
    String svnCleanup();

    /**
     * Translated "Cleaning up working directory...".
     *
     * @return translated "Cleaning up working directory..."
     */
    String cleaningUpWorkingDirectoryEllipses();

    /**
     * Translated "There are no changes to the file \"{0}\" to diff.".
     *
     * @return translated "There are no changes to the file \"{0}\" to diff."
     */
    String noChangesToFileTODiff(String fileName);

    /**
     * Translated "path".
     *
     * @return translated "path"
     */
    String path();

    /**
     * Translated "paths".
     *
     * @return translated "paths"
     */
    String paths();

    /**
     * Translated "File Conflict".
     *
     * @return translated "File Conflict"
     */
    String fileConflictCapitalized();

    /**
     * Translated "This file has a conflict. Would you like to mark it as resolved now?".
     *
     * @return translated "This file has a conflict. Would you like to mark it as resolved now?"
     */
    String fileConflictMarkAsResolved();

    /**
     * Translated "SVN Commit".
     *
     * @return translated "SVN Commit"
     */
    String svnCommit();

    /**
     * Translated "No Items Selected".
     *
     * @return translated "No Items Selected"
     */
    String noItemsSelectedCapitalized();

    /**
     * Translated "Please select one or more items to commit.".
     *
     * @return translated "Please select one or more items to commit."
     */
    String selectOneOrMoreItemsToCommit();

    /**
     * Translated "Message Required".
     *
     * @return translated "Message Required"
     */
    String messageRequiredCapitalized();

    /**
     * Translated "Please provide a commit message.".
     *
     * @return translated "Please provide a commit message."
     */
    String provideACommitMessage();

    /**
     * Translated "Revision".
     *
     * @return translated "Revision"
     */
    String revisionCapitalized();

    /**
     * Translated "SVN Review".
     *
     * @return translated "SVN Review"
     */
    String svnReview();

    /**
     * Translated "SVN Diff".
     *
     * @return translated "SVN Diff"
     */
    String svnDiff();

    /**
     * Translated "Refresh".
     *
     * @return translated "Refresh"
     */
    String refreshCapitalized();

    /**
     * Translated "Update".
     *
     * @return translated "Update"
     */
    String updateCapitalized();

    /**
     * Translated "All changes in this file will be lost.\n\nAre you sure you want to continue?".
     *
     * @return translated "All changes in this file will be lost.\n\nAre you sure you want to continue?"
     */
    String allChangesInFileWillBeLost();

    /**
     * Translated "The selected changes in this file will be lost.\n\nAre you sure you want to continue?".
     *
     * @return translated "The selected changes in this file will be lost.\n\nAre you sure you want to continue?"
     */
    String selectedChangesInFileWillBeLost();

    /**
     * Translated "commit".
     *
     * @return translated "commit"
     */
    String commit();

    /**
     * Translated "diff".
     *
     * @return translated "diff"
     */
    String diff();

    /**
     * Translated "Added".
     *
     * @return translated "Added"
     */
    String addedCapitalized();

    /**
     * Translated "Modified".
     *
     * @return translated "Modified"
     */
    String modifiedCapitalized();

    /**
     * Translated "Deleted".
     *
     * @return translated "Deleted"
     */
    String deletedCapitalized();

    /**
     * Translated "Renamed".
     *
     * @return translated "Renamed"
     */
    String renamedCapitalized();

    /**
     * Translated "Copied".
     *
     * @return translated "Copied"
     */
    String copiedCapitalized();

    /**
     * Translated "Untracked".
     *
     * @return translated "Untracked"
     */
    String untrackedCapitalized();

    /**
     * Translated "Unmerged".
     *
     * @return translated "Unmerged"
     */
    String unmergedCapitalized();

    /**
     * Translated "Conflicted".
     *
     * @return translated "Conflicted"
     */
    String conflictedCapitalized();

    /**
     * Translated "External".
     *
     * @return translated "External"
     */
    String externalCapitalized();

    /**
     * Translated "Ignored".
     *
     * @return translated "Ignored"
     */
    String ignoredCapitalized();

    /**
     * Translated "Missing".
     *
     * @return translated "Missing"
     */
    String missingCapitalized();

    /**
     * Translated "Obstructed".
     *
     * @return translated "Obstructed"
     */
    String obstructedCapitalized();

    /**
     * Translated "Unversioned".
     *
     * @return translated "Unversioned"
     */
    String unversionedCapitalized();
}
