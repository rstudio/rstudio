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
    @DefaultMessage("Add")
    String addCapitalized();

    /**
     * Translated "Remote Name:".
     *
     * @return translated "Remote Name:"
     */
    @DefaultMessage("Remote Name:")
    String remoteNameColon();

    /**
     * Translated "Remote URL:".
     *
     * @return translated "Remote URL:"
     */
    @DefaultMessage("Remote URL:")
    String remoteUrlColon();

    /**
     * Translated "Switch branch".
     *
     * @return translated "Switch branch"
     */
    @DefaultMessage("Switch branch")
    String switchBranch();

    /**
     * Translated "Search by branch name".
     *
     * @return translated "Search by branch name"
     */
    @DefaultMessage("Search by branch name")
    String searchByBranchName();

    /**
     * Translated "(Remote: {0})".
     *
     * @return translated "(Remote: {0})"
     */
    @DefaultMessage("(Remote: {0})")
    String remoteBranchCaption(String caption);

    /**
     * Translated "(no branch)".
     *
     * @return translated "(no branch)"
     */
    @DefaultMessage("(no branch)")
    String noBranchParentheses();

    /**
     * Translated "(no branches available)".
     *
     * @return translated "(no branches available)"
     */
    @DefaultMessage("(no branches available)")
    String noBranchesAvailableParentheses();

    /**
     * Translated "(local branches)".
     *
     * @return translated "(local branches)"
     */
    @DefaultMessage("(local branches)")
    String localBranchesParentheses();

    /**
     * Translated "Create".
     *
     * @return translated "Create"
     */
    @DefaultMessage("Create")
    String createCapitalized();

    /**
     * Translated "Remote:".
     *
     * @return translated "Remote:"
     */
    @DefaultMessage("Remote:")
    String remoteColon();

    /**
     * Translated "Add Remote...".
     *
     * @return translated "Add Remote..."
     */
    @DefaultMessage("Add Remote...")
    String addRemoteEllipses();

    /**
     * Translated "Add Remote".
     *
     * @return translated "Add Remote"
     */
    @DefaultMessage("Add Remote")
    String addRemote();

    /**
     * Translated "Sync branch with remote".
     *
     * @return translated "Sync branch with remote"
     */
    @DefaultMessage("Sync branch with remote")
    String syncBranchWithRemote();

    /**
     * Translated "Branch Name:".
     *
     * @return translated "Branch Name:"
     */
    @DefaultMessage("Branch Name:")
    String branchNameColon();

    /**
     * Translated "New Branch".
     *
     * @return translated "New Branch"
     */
    @DefaultMessage("New Branch")
    String newBranchCapitalized();

    /**
     * Translated "A local branch named ''{0}'' already exists. Would you like to check out that branch, or overwrite it?".
     *
     * @return translated "A local branch named ''{0}'' already exists. Would you like to check out that branch, or overwrite it?"
     */
    @DefaultMessage("A local branch named ''{0}'' already exists. Would you like to check out that branch, or overwrite it?")
    String localBranchAlreadyExists(String branchName);

    /**
     * Translated "Checkout".
     *
     * @return translated "Checkout"
     */
    @DefaultMessage("Checkout")
    String checkoutCapitalized();

    /**
     * Translated "Overwrite".
     *
     * @return translated "Overwrite"
     */
    @DefaultMessage("Overwrite")
    String overwriteCapitalized();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    @DefaultMessage("Cancel")
    String cancelCapitalized();

    /**
     * Translated "Local Branch Already Exists".
     *
     * @return translated "Local Branch Already Exists"
     */
    @DefaultMessage("Local Branch Already Exists")
    String localBranchAlreadyExistsCaption();

    /**
     * Translated "A remote branch named ''{0}'' already exists on the remote repository ''{1}''. Would you like to check out that branch?".
     *
     * @return translated "A remote branch named ''{0}'' already exists on the remote repository ''{1}''. Would you like to check out that branch?"
     */
    @DefaultMessage("A remote branch named ''{0}'' already exists on the remote repository ''{1}''. Would you like to check out that branch?")
    String remoteBranchNameAlreadyExists(String remoteBranch, String remote);

    /**
     * Translated "Remote Branch Already Exists".
     *
     * @return translated "Remote Branch Already Exists"
     */
    @DefaultMessage("Remote Branch Already Exists")
    String remoteBranchAlreadyExistsCaption();

    /**
     * Translated "(all branches)".
     *
     * @return translated "(all branches)"
     */
    @DefaultMessage("(all branches)")
    String allBranchesParentheses();

    /**
     * Translated "Close".
     *
     * @return translated "Close"
     */
    @DefaultMessage("Close")
    String closeCapitalized();

    /**
     * Translated "Stop".
     *
     * @return translated "Stop"
     */
    @DefaultMessage("Stop")
    String stopCapitalized();

    /**
     * Translated "Progress details".
     *
     * @return translated "Progress details"
     */
    @DefaultMessage("Progress details")
    String progressDetails();

    /**
     * Translated "Commits {0}".
     *
     * @return translated "Commits {0}"
     */
    @DefaultMessage("Commits {0}")
    String commitsPager(String wordText);

    /**
     * Translated "{0} selection".
     * Empty String so there can be leading space
     * @return translated "{0} selection"
     */
    @DefaultMessage("{0} selection")
    String selectionSuffix(String emptyString);

    /**
     * Translated "{0} line".
     * Empty String so there can be leading space
     * @return translated "{0} line"
     */
    @DefaultMessage("{0} line")
    String lineSuffix(String emptyString);

    /**
     * Translated "{0} chunk".
     * Empty String so there can be leading space
     * @return translated "{0} chunk"
     */
    @DefaultMessage("{0} chunk")
    String chunkSuffix(String emptyString);

    /**
     * Translated "(No commit selected)".
     *
     * @return translated "(No commit selected)"
     */
    @DefaultMessage("(No commit selected)")
    String noCommitSelectedParentheses();

    /**
     * Translated "(all commits)".
     *
     * @return translated "(all commits)"
     */
    @DefaultMessage("(all commits)")
    String allCommitsParentheses();

    /**
     * Translated "Filter by File...".
     *
     * @return translated "Filter by File..."
     */
    @DefaultMessage("Filter by File...")
    String filterByFileEllipses();

    /**
     * Translated "Choose File".
     *
     * @return translated "Choose File"
     */
    @DefaultMessage("Choose File")
    String chooseFileCapitalized();

    /**
     * Translated "Filter by Directory...".
     *
     * @return translated "Filter by Directory..."
     */
    @DefaultMessage("Filter by Directory...")
    String filterByDirectoryEllipses();

    /**
     * Translated "Choose Folder".
     *
     * @return translated "Choose Folder"
     */
    @DefaultMessage("Choose Folder")
    String chooseFolderCapitalized();

    /**
     * Translated "Filter: {0}".
     *
     * @return translated "Filter: {0}"
     */
    @DefaultMessage("Filter: {0}")
    String filterColonPath(String path);

    /**
     * Translated "Filter: (None)".
     *
     * @return translated "Filter: (None)"
     */
    @DefaultMessage("Filter: (None)")
    String filterColonNone();

    /**
     * Translated "View file @ {0}".
     *
     * @return translated "View file @ {0}"
     */
    @DefaultMessage("View file @ {0}")
    String viewFileAtString(String substr);

    /**
     * Translated "Error Fetching History".
     *
     * @return translated "Error Fetching History"
     */
    @DefaultMessage("Error Fetching History")
    String errorFetchingHistory();

    /**
     * Translated "History".
     *
     * @return translated "History"
     */
    @DefaultMessage("History")
    String historyCapitalized();

    /**
     * Translated "Changes".
     *
     * @return translated "Changes"
     */
    @DefaultMessage("Changes")
    String changesCapitalized();

    /**
     * Translated "Search version control history".
     *
     * @return translated "Search version control history"
     */
    @DefaultMessage("Search version control history")
    String searchVersionControlHistory();

    /**
     * Translated "Search".
     *
     * @return translated "Search"
     */
    @DefaultMessage("Search")
    String searchCapitalized();

    /**
     * Translated "Reading file...".
     *
     * @return translated "Reading file..."
     */
    @DefaultMessage("Reading file...")
    String readingFileEllipses();

    /**
     * Translated "Show History".
     *
     * @return translated "Show History"
     */
    @DefaultMessage("Show History")
    String showHistoryCapitalized();

    /**
     * Translated "Show Diff".
     *
     * @return translated "Show Diff"
     */
    @DefaultMessage("Show Diff")
    String showDiffCapitalized();

    /**
     * Translated "commit depth {0}".
     *
     * @return translated "commit depth {0}"
     */
    @DefaultMessage("commit depth {0}")
    String commitDepthAltText(int nexusColumn);

    /**
     * Translated "Your branch is ahead of ''{0}'' by {1} commit.".
     *
     * @return translated "Your branch is ahead of ''{0}'' by {1} commit."
     */
    @DefaultMessage("Your branch is ahead of ''{0}'' by {1} commit.")
    String branchAheadOfRemoteSingular(String remoteName, int commitBehind);

    /**
     * Translated "Your branch is ahead of ''{0}'' by {1} commits.".
     *
     * @return translated "Your branch is ahead of ''{0}'' by {1} commits."
     */
    @DefaultMessage("Your branch is ahead of ''{0}'' by {1} commits.")
    String branchAheadOfRemotePlural(String remoteName, int commitBehind);

    /**
     * Translated "Status".
     *
     * @return translated "Status"
     */
    @DefaultMessage("Status")
    String statusCapitalized();

    /**
     * Translated "Path".
     *
     * @return translated "Path"
     */
    @DefaultMessage("Path")
    String pathCapitalized();

    /**
     * Translated "Subject".
     *
     * @return translated "Subject"
     */
    @DefaultMessage("Subject")
    String subjectCapitalized();

    /**
     * Translated "Author".
     *
     * @return translated "Author"
     */
    @DefaultMessage("Author")
    String authorCapitalized();

    /**
     * Translated "Date".
     *
     * @return translated "Date"
     */
    @DefaultMessage("Date")
    String dateCapitalized();

    /**
     * Translated "Staged".
     *
     * @return translated "Staged"
     */
    @DefaultMessage("Staged")
    String stagedCapitalized();

    /**
     * Translated "Git Tab".
     *
     * @return translated "Git Tab"
     */
    @DefaultMessage("Git Tab")
    String gitTabCapitalized();

    /**
     * Translated "Pull options".
     *
     * @return translated "Pull options"
     */
    @DefaultMessage("Pull options")
    String pullOptions();

    /**
     * Translated "More".
     *
     * @return translated "More"
     */
    @DefaultMessage("More")
    String moreCapitalized();

    /**
     * Translated "Refresh Now".
     *
     * @return translated "Refresh Now"
     */
    @DefaultMessage("Refresh Now")
    String refreshNowCapitalized();

    /**
     * Translated "Refresh options".
     *
     * @return translated "Refresh options"
     */
    @DefaultMessage("Refresh options")
    String refreshOptions();

    /**
     * Translated "Pull".
     *
     * @return translated "Pull"
     */
    @DefaultMessage("Pull")
    String pullCapitalized();

    /**
     * Translated "Push".
     *
     * @return translated "Push"
     */
    @DefaultMessage("Push")
    String pushCapitalized();

    /**
     * Translated "No Changes to File".
     *
     * @return translated "No Changes to File"
     */
    @DefaultMessage("No Changes to File")
    String noChangesToFile();

    /**
     * Translated "There are no changes to the file \"{0}\" to diff.".
     *
     * @return translated "There are no changes to the file \"{0}\" to diff."
     */
    @DefaultMessage("There are no changes to the file \"{0}\" to diff.")
    String noChangesToFileToDiff(String filename);

    /**
     * Translated "No Changes to Revert".
     *
     * @return translated "No Changes to Revert"
     */
    @DefaultMessage("No Changes to Revert")
    String noChangesToRevert();

    /**
     * Translated "There are no changes to the file \"{0}\" to revert.".
     *
     * @return translated "There are no changes to the file \"{0}\" to revert."
     */
    @DefaultMessage("There are no changes to the file \"{0}\" to revert.")
    String noChangesToFileToRevert(String filename);

    /**
     * Translated "Revert Changes".
     *
     * @return translated "Revert Changes"
     */
    @DefaultMessage("Revert Changes")
    String revertChangesCapitalized();

    /**
     * Translated "Changes to the selected file will be lost, including staged changes.\n\nAre you sure you want to continue?".
     *
     * @return translated "Changes to the selected file will be lost, including staged changes.\n\nAre you sure you want to continue?"
     */
    @DefaultMessage("Changes to the selected file will be lost, including staged changes.\n\nAre you sure you want to continue?")
    String changesToFileWillBeLost();

    /**
     * Translated "Changes to the selected files will be lost, including staged changes.\n\nAre you sure you want to continue?".
     *
     * @return translated "Changes to the selected files will be lost, including staged changes.\n\nAre you sure you want to continue?"
     */
    @DefaultMessage("Changes to the selected files will be lost, including staged changes.\n\nAre you sure you want to continue?")
    String changesToFileWillBeLostPlural();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    String errorCapitalized();

    /**
     * Translated "Unable to view {0} on GitHub.\n\nAre you sure that this file is on GitHub and is contained in the currently active project?".
     *
     * @return translated "Unable to view {0} on GitHub.\n\nAre you sure that this file is on GitHub and is contained in the currently active project?"
     */
    @DefaultMessage("Unable to view {0} on GitHub.\n\nAre you sure that this file is on GitHub and is contained in the currently active project?")
    String unableToViewPathOnGithub(String path);

    /**
     * Translated "Git Ignore".
     *
     * @return translated "Git Ignore"
     */
    @DefaultMessage("Git Ignore")
    String gitIgnoreCapitalized();

    /**
     * Translated "Diff".
     *
     * @return translated "Diff"
     */
    @DefaultMessage("Diff")
    String diffCapitalized();

    /**
     * Translated "Commit".
     *
     * @return translated "Commit"
     */
    @DefaultMessage("Commit")
    String commitCapitalized();

    /**
     * Translated "Git Review".
     *
     * @return translated "Git Review"
     */
    @DefaultMessage("Git Review")
    String gitReviewCapitalized();

    /**
     * Translated "Git Diff".
     *
     * @return translated "Git Diff"
     */
    @DefaultMessage("Git Diff")
    String gitDiffCapitalized();

    /**
     * Translated "Stage".
     *
     * @return translated "Stage"
     */
    @DefaultMessage("Stage")
    String stageCapitalized();

    /**
     * Translated "Revert".
     *
     * @return translated "Revert"
     */
    @DefaultMessage("Revert")
    String revertCapitalized();

    /**
     * Translated "Ignore".
     *
     * @return translated "Ignore"
     */
    @DefaultMessage("Ignore")
    String ignoreCapitalized();

    /**
     * Translated "Stage All".
     *
     * @return translated "Stage All"
     */
    @DefaultMessage("Stage All")
    String stageAllCapitalized();

    /**
     * Translated "Discard All".
     *
     * @return translated "Discard All"
     */
    @DefaultMessage("Discard All")
    String discardAllCapitalized();

    /**
     * Translated "Unstage All".
     *
     * @return translated "Unstage All"
     */
    @DefaultMessage("Unstage All")
    String unstageAllCapitalized();

    /**
     * Translated "Revert...".
     *
     * @return translated "Revert..."
     */
    @DefaultMessage("Revert...")
    String revertEllipses();

    /**
     * Translated "Ignore...".
     *
     * @return translated "Ignore..."
     */
    @DefaultMessage("Ignore...")
    String ignoreEllipses();

    /**
     * Translated "Open File".
     *
     * @return translated "Open File"
     */
    @DefaultMessage("Open File")
    String openFileCapitalized();

    /**
     * Translated "{0} characters".
     * Character as in a letter in a string.
     * @return translated "{0} characters"
     */
    @DefaultMessage("{0} characters")
    String lengthCharacters(int length);

    /**
     * Translated "{0} characters in message".
     *
     * @return translated "{0} characters in message"
     */
    @DefaultMessage("{0} characters in message")
    String lengthCharactersInMessage(int length);

    /**
     * Translated "Unstage".
     *
     * @return translated "Unstage"
     */
    @DefaultMessage("Unstage")
    String unstageCapitalized();

    /**
     * Translated "Discard".
     *
     * @return translated "Discard"
     */
    @DefaultMessage("Discard")
    String discardCapitalized();

    /**
     * Translated "All unstaged changes in this file will be lost.\n\nAre you sure you want to continue?".
     *
     * @return translated "All unstaged changes in this file will be lost.\n\nAre you sure you want to continue?"
     */
    @DefaultMessage("All unstaged changes in this file will be lost.\n\nAre you sure you want to continue?")
    String allUnstagedChangesWillBeLost();

    /**
     * Translated "The selected changes in this file will be lost.\n\nAre you sure you want to continue?".
     *
     * @return translated "The selected changes in this file will be lost.\n\nAre you sure you want to continue?"
     */
    @DefaultMessage("The selected changes in this file will be lost.\n\nAre you sure you want to continue?")
    String theSelectedChangesWillBeLost();

    /**
     * Translated "Some of the files to be committed are quite large (>{0} in size). Are you sure you want to commit these files?".
     *
     * @return translated "Some of the files to be committed are quite large (>{0} in size). Are you sure you want to commit these files?"
     */
    @DefaultMessage("Some of the files to be committed are quite large (>{0} in size). Are you sure you want to commit these files?")
    String someFilesAreQuiteLarge(String prettySize);

    /**
     * Translated "Committing Large Files".
     *
     * @return translated "Committing Large Files"
     */
    @DefaultMessage("Committing Large Files")
    String committingLargeFiles();

    /**
     * Translated "Diff Error".
     *
     * @return translated "Diff Error"
     */
    @DefaultMessage("Diff Error")
    String diffError();

    /**
     * Translated "Changelist".
     *
     * @return translated "Changelist"
     */
    @DefaultMessage("Changelist")
    String changeList();

    /**
     * Translated "SVN Ignore".
     *
     * @return translated "SVN Ignore"
     */
    @DefaultMessage("SVN Ignore")
    String svnIgnore();

    /**
     * Translated "svn:ignore".
     *
     * @return translated "svn:ignore"
     */
    @DefaultMessage("svn:ignore")
    String svnColonIgnore();

    /**
     * Translated "SVN Add".
     *
     * @return translated "SVN Add"
     */
    @DefaultMessage("SVN Add")
    String svnAdd();

    /**
     * Translated "SVN Delete".
     *
     * @return translated "SVN Delete"
     */
    @DefaultMessage("SVN Delete")
    String svnDelete();

    /**
     * Translated "Resolve".
     *
     * @return translated "Resolve"
     */
    @DefaultMessage("Resolve")
    String resolveCapitalized();

    /**
     * Translated "SVN Resolve".
     *
     * @return translated "SVN Resolve"
     */
    @DefaultMessage("SVN Resolve")
    String svnResolve();

    /**
     * Translated "None of the selected paths appear to have conflicts.\n\nDo you want to resolve anyway?".
     *
     * @return translated "None of the selected paths appear to have conflicts.\n\nDo you want to resolve anyway?"
     */
    @DefaultMessage("None of the selected paths appear to have conflicts.\n\nDo you want to resolve anyway?")
    String noneOfSelectedPathsHaveConflicts();

    /**
     * Translated "The selected path does not appear to have conflicts.\n\nDo you want to resolve anyway?".
     *
     * @return translated "The selected path does not appear to have conflicts.\n\nDo you want to resolve anyway?"
     */
    @DefaultMessage("The selected path does not appear to have conflicts.\n\nDo you want to resolve anyway?")
    String selectedPathDoesNotAppearToHaveConflicts();

    /**
     * Translated "No Conflicts Detected".
     *
     * @return translated "No Conflicts Detected"
     */
    @DefaultMessage("No Conflicts Detected")
    String noConflictsDetected();

    /**
     * Translated "Changes to the selected file will be reverted.\n\nAre you sure you want to continue?".
     *
     * @return translated "Changes to the selected file will be reverted.\n\nAre you sure you want to continue?"
     */
    @DefaultMessage("Changes to the selected file will be reverted.\n\nAre you sure you want to continue?")
    String changesToSelectedFileWillBeReverted();

    /**
     * Translated "Changes to the selected files will be reverted.\n\nAre you sure you want to continue?".
     *
     * @return translated "Changes to the selected files will be reverted.\n\nAre you sure you want to continue?"
     */
    @DefaultMessage("Changes to the selected files will be reverted.\n\nAre you sure you want to continue?")
    String changesToSelectedFileWillBeRevertedPlural();

    /**
     * Translated "SVN Revert".
     *
     * @return translated "SVN Revert"
     */
    @DefaultMessage("SVN Revert")
    String svnRevert();

    /**
     * Translated "SVN Tab".
     *
     * @return translated "SVN Tab"
     */
    @DefaultMessage("SVN Tab")
    String svnTab();

    /**
     * Translated "SVN Cleanup".
     *
     * @return translated "SVN Cleanup"
     */
    @DefaultMessage("SVN Cleanup")
    String svnCleanup();

    /**
     * Translated "Cleaning up working directory...".
     *
     * @return translated "Cleaning up working directory..."
     */
    @DefaultMessage("Cleaning up working directory...")
    String cleaningUpWorkingDirectoryEllipses();

    /**
     * Translated "There are no changes to the file \"{0}\" to diff.".
     *
     * @return translated "There are no changes to the file \"{0}\" to diff."
     */
    @DefaultMessage("There are no changes to the file \"{0}\" to diff.")
    String noChangesToFileTODiff(String fileName);

    /**
     * Translated "path".
     *
     * @return translated "path"
     */
    @DefaultMessage("path")
    String path();

    /**
     * Translated "paths".
     *
     * @return translated "paths"
     */
    @DefaultMessage("paths")
    String paths();

    /**
     * Translated "File Conflict".
     *
     * @return translated "File Conflict"
     */
    @DefaultMessage("File Conflict")
    String fileConflictCapitalized();

    /**
     * Translated "This file has a conflict. Would you like to mark it as resolved now?".
     *
     * @return translated "This file has a conflict. Would you like to mark it as resolved now?"
     */
    @DefaultMessage("This file has a conflict. Would you like to mark it as resolved now?")
    String fileConflictMarkAsResolved();

    /**
     * Translated "SVN Commit".
     *
     * @return translated "SVN Commit"
     */
    @DefaultMessage("SVN Commit")
    String svnCommit();

    /**
     * Translated "No Items Selected".
     *
     * @return translated "No Items Selected"
     */
    @DefaultMessage("No Items Selected")
    String noItemsSelectedCapitalized();

    /**
     * Translated "Please select one or more items to commit.".
     *
     * @return translated "Please select one or more items to commit."
     */
    @DefaultMessage("Please select one or more items to commit.")
    String selectOneOrMoreItemsToCommit();

    /**
     * Translated "Message Required".
     *
     * @return translated "Message Required"
     */
    @DefaultMessage("Message Required")
    String messageRequiredCapitalized();

    /**
     * Translated "Please provide a commit message.".
     *
     * @return translated "Please provide a commit message."
     */
    @DefaultMessage("Please provide a commit message.")
    String provideACommitMessage();

    /**
     * Translated "Revision".
     *
     * @return translated "Revision"
     */
    @DefaultMessage("Revision")
    String revisionCapitalized();

    /**
     * Translated "SVN Review".
     *
     * @return translated "SVN Review"
     */
    @DefaultMessage("SVN Review")
    String svnReview();

    /**
     * Translated "SVN Diff".
     *
     * @return translated "SVN Diff"
     */
    @DefaultMessage("SVN Diff")
    String svnDiff();

    /**
     * Translated "Refresh".
     *
     * @return translated "Refresh"
     */
    @DefaultMessage("Refresh")
    String refreshCapitalized();

    /**
     * Translated "Update".
     *
     * @return translated "Update"
     */
    @DefaultMessage("Update")
    String updateCapitalized();

    /**
     * Translated "All changes in this file will be lost.\n\nAre you sure you want to continue?".
     *
     * @return translated "All changes in this file will be lost.\n\nAre you sure you want to continue?"
     */
    @DefaultMessage("All changes in this file will be lost.\n\nAre you sure you want to continue?")
    String allChangesInFileWillBeLost();

    /**
     * Translated "The selected changes in this file will be lost.\n\nAre you sure you want to continue?".
     *
     * @return translated "The selected changes in this file will be lost.\n\nAre you sure you want to continue?"
     */
    @DefaultMessage("The selected changes in this file will be lost.\n\nAre you sure you want to continue?")
    String selectedChangesInFileWillBeLost();

    /**
     * Translated "commit".
     *
     * @return translated "commit"
     */
    @DefaultMessage("commit")
    String commit();

    /**
     * Translated "diff".
     *
     * @return translated "diff"
     */
    @DefaultMessage("diff")
    String diff();

    /**
     * Translated "Added".
     *
     * @return translated "Added"
     */
    @DefaultMessage("Added")
    String addedCapitalized();

    /**
     * Translated "Modified".
     *
     * @return translated "Modified"
     */
    @DefaultMessage("Modified")
    String modifiedCapitalized();

    /**
     * Translated "Deleted".
     *
     * @return translated "Deleted"
     */
    @DefaultMessage("Deleted")
    String deletedCapitalized();

    /**
     * Translated "Renamed".
     *
     * @return translated "Renamed"
     */
    @DefaultMessage("Renamed")
    String renamedCapitalized();

    /**
     * Translated "Copied".
     *
     * @return translated "Copied"
     */
    @DefaultMessage("Copied")
    String copiedCapitalized();

    /**
     * Translated "Untracked".
     *
     * @return translated "Untracked"
     */
    @DefaultMessage("Untracked")
    String untrackedCapitalized();

    /**
     * Translated "Unmerged".
     *
     * @return translated "Unmerged"
     */
    @DefaultMessage("Unmerged")
    String unmergedCapitalized();

    /**
     * Translated "Conflicted".
     *
     * @return translated "Conflicted"
     */
    @DefaultMessage("Conflicted")
    String conflictedCapitalized();

    /**
     * Translated "External".
     *
     * @return translated "External"
     */
    @DefaultMessage("External")
    String externalCapitalized();

    /**
     * Translated "Ignored".
     *
     * @return translated "Ignored"
     */
    @DefaultMessage("Ignored")
    String ignoredCapitalized();

    /**
     * Translated "Missing".
     *
     * @return translated "Missing"
     */
    @DefaultMessage("Missing")
    String missingCapitalized();

    /**
     * Translated "Obstructed".
     *
     * @return translated "Obstructed"
     */
    @DefaultMessage("Obstructed")
    String obstructedCapitalized();

    /**
     * Translated "Unversioned".
     *
     * @return translated "Unversioned"
     */
    @DefaultMessage("Unversioned")
    String unversionedCapitalized();
}
