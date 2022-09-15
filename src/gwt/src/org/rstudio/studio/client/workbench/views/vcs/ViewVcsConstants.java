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
    @Key("addCapitalized")
    String addCapitalized();

    /**
     * Translated "Remote Name:".
     *
     * @return translated "Remote Name:"
     */
    @DefaultMessage("Remote Name:")
    @Key("remoteNameColon")
    String remoteNameColon();

    /**
     * Translated "Remote URL:".
     *
     * @return translated "Remote URL:"
     */
    @DefaultMessage("Remote URL:")
    @Key("remoteUrlColon")
    String remoteUrlColon();

    /**
     * Translated "Switch branch".
     *
     * @return translated "Switch branch"
     */
    @DefaultMessage("Switch branch")
    @Key("switchBranch")
    String switchBranch();

    /**
     * Translated "Search by branch name".
     *
     * @return translated "Search by branch name"
     */
    @DefaultMessage("Search by branch name")
    @Key("searchByBranchName")
    String searchByBranchName();

    /**
     * Translated "(Remote: {0})".
     *
     * @return translated "(Remote: {0})"
     */
    @DefaultMessage("(Remote: {0})")
    @Key("remoteBranchCaption")
    String remoteBranchCaption(String caption);

    /**
     * Translated "(no branch)".
     *
     * @return translated "(no branch)"
     */
    @DefaultMessage("(no branch)")
    @Key("noBranchParentheses")
    String noBranchParentheses();

    /**
     * Translated "(no branches available)".
     *
     * @return translated "(no branches available)"
     */
    @DefaultMessage("(no branches available)")
    @Key("noBranchesAvailableParentheses")
    String noBranchesAvailableParentheses();

    /**
     * Translated "(local branches)".
     *
     * @return translated "(local branches)"
     */
    @DefaultMessage("(local branches)")
    @Key("localBranchesParentheses")
    String localBranchesParentheses();

    /**
     * Translated "Create".
     *
     * @return translated "Create"
     */
    @DefaultMessage("Create")
    @Key("createCapitalized")
    String createCapitalized();

    /**
     * Translated "Remote:".
     *
     * @return translated "Remote:"
     */
    @DefaultMessage("Remote:")
    @Key("remoteColon")
    String remoteColon();

    /**
     * Translated "Add Remote...".
     *
     * @return translated "Add Remote..."
     */
    @DefaultMessage("Add Remote...")
    @Key("addRemoteEllipses")
    String addRemoteEllipses();

    /**
     * Translated "Add Remote".
     *
     * @return translated "Add Remote"
     */
    @DefaultMessage("Add Remote")
    @Key("addRemote")
    String addRemote();

    /**
     * Translated "Sync branch with remote".
     *
     * @return translated "Sync branch with remote"
     */
    @DefaultMessage("Sync branch with remote")
    @Key("syncBranchWithRemote")
    String syncBranchWithRemote();

    /**
     * Translated "Branch Name:".
     *
     * @return translated "Branch Name:"
     */
    @DefaultMessage("Branch Name:")
    @Key("branchNameColon")
    String branchNameColon();

    /**
     * Translated "New Branch".
     *
     * @return translated "New Branch"
     */
    @DefaultMessage("New Branch")
    @Key("newBranchCapitalized")
    String newBranchCapitalized();

    /**
     * Translated "A local branch named ''{0}'' already exists. Would you like to check out that branch, or overwrite it?".
     *
     * @return translated "A local branch named ''{0}'' already exists. Would you like to check out that branch, or overwrite it?"
     */
    @DefaultMessage("A local branch named ''{0}'' already exists. Would you like to check out that branch, or overwrite it?")
    @Key("localBranchAlreadyExists")
    String localBranchAlreadyExists(String branchName);

    /**
     * Translated "Checkout".
     *
     * @return translated "Checkout"
     */
    @DefaultMessage("Checkout")
    @Key("checkoutCapitalized")
    String checkoutCapitalized();

    /**
     * Translated "Overwrite".
     *
     * @return translated "Overwrite"
     */
    @DefaultMessage("Overwrite")
    @Key("overwriteCapitalized")
    String overwriteCapitalized();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    @DefaultMessage("Cancel")
    @Key("cancelCapitalized")
    String cancelCapitalized();

    /**
     * Translated "Local Branch Already Exists".
     *
     * @return translated "Local Branch Already Exists"
     */
    @DefaultMessage("Local Branch Already Exists")
    @Key("localBranchAlreadyExistsCaption")
    String localBranchAlreadyExistsCaption();

    /**
     * Translated "A remote branch named ''{0}'' already exists on the remote repository ''{1}''. Would you like to check out that branch?".
     *
     * @return translated "A remote branch named ''{0}'' already exists on the remote repository ''{1}''. Would you like to check out that branch?"
     */
    @DefaultMessage("A remote branch named ''{0}'' already exists on the remote repository ''{1}''. Would you like to check out that branch?")
    @Key("remoteBranchNameAlreadyExists")
    String remoteBranchNameAlreadyExists(String remoteBranch, String remote);

    /**
     * Translated "Remote Branch Already Exists".
     *
     * @return translated "Remote Branch Already Exists"
     */
    @DefaultMessage("Remote Branch Already Exists")
    @Key("remoteBranchAlreadyExistsCaption")
    String remoteBranchAlreadyExistsCaption();

    /**
     * Translated "(all branches)".
     *
     * @return translated "(all branches)"
     */
    @DefaultMessage("(all branches)")
    @Key("allBranchesParentheses")
    String allBranchesParentheses();

    /**
     * Translated "Close".
     *
     * @return translated "Close"
     */
    @DefaultMessage("Close")
    @Key("closeCapitalized")
    String closeCapitalized();

    /**
     * Translated "Stop".
     *
     * @return translated "Stop"
     */
    @DefaultMessage("Stop")
    @Key("stopCapitalized")
    String stopCapitalized();

    /**
     * Translated "Progress details".
     *
     * @return translated "Progress details"
     */
    @DefaultMessage("Progress details")
    @Key("progressDetails")
    String progressDetails();

    /**
     * Translated "Commits {0}".
     *
     * @return translated "Commits {0}"
     */
    @DefaultMessage("Commits {0}")
    @Key("commitsPager")
    String commitsPager(String wordText);

    /**
     * Translated "{0} selection".
     * Empty String so there can be leading space
     * @return translated "{0} selection"
     */
    @DefaultMessage("{0} selection")
    @Key("selectionSuffix")
    String selectionSuffix(String emptyString);

    /**
     * Translated "{0} line".
     * Empty String so there can be leading space
     * @return translated "{0} line"
     */
    @DefaultMessage("{0} line")
    @Key("lineSuffix")
    String lineSuffix(String emptyString);

    /**
     * Translated "{0} chunk".
     * Empty String so there can be leading space
     * @return translated "{0} chunk"
     */
    @DefaultMessage("{0} chunk")
    @Key("chunkSuffix")
    String chunkSuffix(String emptyString);

    /**
     * Translated "(No commit selected)".
     *
     * @return translated "(No commit selected)"
     */
    @DefaultMessage("(No commit selected)")
    @Key("noCommitSelectedParentheses")
    String noCommitSelectedParentheses();

    /**
     * Translated "(all commits)".
     *
     * @return translated "(all commits)"
     */
    @DefaultMessage("(all commits)")
    @Key("allCommitsParentheses")
    String allCommitsParentheses();

    /**
     * Translated "Filter by File...".
     *
     * @return translated "Filter by File..."
     */
    @DefaultMessage("Filter by File...")
    @Key("filterByFileEllipses")
    String filterByFileEllipses();

    /**
     * Translated "Choose File".
     *
     * @return translated "Choose File"
     */
    @DefaultMessage("Choose File")
    @Key("chooseFileCapitalized")
    String chooseFileCapitalized();

    /**
     * Translated "Filter by Directory...".
     *
     * @return translated "Filter by Directory..."
     */
    @DefaultMessage("Filter by Directory...")
    @Key("filterByDirectoryEllipses")
    String filterByDirectoryEllipses();

    /**
     * Translated "Choose Folder".
     *
     * @return translated "Choose Folder"
     */
    @DefaultMessage("Choose Folder")
    @Key("chooseFolderCapitalized")
    String chooseFolderCapitalized();

    /**
     * Translated "Filter: {0}".
     *
     * @return translated "Filter: {0}"
     */
    @DefaultMessage("Filter: {0}")
    @Key("filterColonPath")
    String filterColonPath(String path);

    /**
     * Translated "Filter: (None)".
     *
     * @return translated "Filter: (None)"
     */
    @DefaultMessage("Filter: (None)")
    @Key("filterColonNone")
    String filterColonNone();

    /**
     * Translated "View file @ {0}".
     *
     * @return translated "View file @ {0}"
     */
    @DefaultMessage("View file @ {0}")
    @Key("viewFileAtString")
    String viewFileAtString(String substr);

    /**
     * Translated "Error Fetching History".
     *
     * @return translated "Error Fetching History"
     */
    @DefaultMessage("Error Fetching History")
    @Key("errorFetchingHistory")
    String errorFetchingHistory();

    /**
     * Translated "History".
     *
     * @return translated "History"
     */
    @DefaultMessage("History")
    @Key("historyCapitalized")
    String historyCapitalized();

    /**
     * Translated "Changes".
     *
     * @return translated "Changes"
     */
    @DefaultMessage("Changes")
    @Key("changesCapitalized")
    String changesCapitalized();

    /**
     * Translated "Search version control history".
     *
     * @return translated "Search version control history"
     */
    @DefaultMessage("Search version control history")
    @Key("searchVersionControlHistory")
    String searchVersionControlHistory();

    /**
     * Translated "Search".
     *
     * @return translated "Search"
     */
    @DefaultMessage("Search")
    @Key("searchCapitalized")
    String searchCapitalized();

    /**
     * Translated "Reading file...".
     *
     * @return translated "Reading file..."
     */
    @DefaultMessage("Reading file...")
    @Key("readingFileEllipses")
    String readingFileEllipses();

    /**
     * Translated "Show History".
     *
     * @return translated "Show History"
     */
    @DefaultMessage("Show History")
    @Key("showHistoryCapitalized")
    String showHistoryCapitalized();

    /**
     * Translated "Show Diff".
     *
     * @return translated "Show Diff"
     */
    @DefaultMessage("Show Diff")
    @Key("showDiffCapitalized")
    String showDiffCapitalized();

    /**
     * Translated "commit depth {0}".
     *
     * @return translated "commit depth {0}"
     */
    @DefaultMessage("commit depth {0}")
    @Key("commitDepthAltText")
    String commitDepthAltText(int nexusColumn);

    /**
     * Translated "Your branch is ahead of ''{0}'' by {1} commit.".
     *
     * @return translated "Your branch is ahead of ''{0}'' by {1} commit."
     */
    @DefaultMessage("Your branch is ahead of ''{0}'' by {1} commit.")
    @Key("branchAheadOfRemoteSingular")
    String branchAheadOfRemoteSingular(String remoteName, int commitBehind);

    /**
     * Translated "Your branch is ahead of ''{0}'' by {1} commits.".
     *
     * @return translated "Your branch is ahead of ''{0}'' by {1} commits."
     */
    @DefaultMessage("Your branch is ahead of ''{0}'' by {1} commits.")
    @Key("branchAheadOfRemotePlural")
    String branchAheadOfRemotePlural(String remoteName, int commitBehind);

    /**
     * Translated "Status".
     *
     * @return translated "Status"
     */
    @DefaultMessage("Status")
    @Key("statusCapitalized")
    String statusCapitalized();

    /**
     * Translated "Path".
     *
     * @return translated "Path"
     */
    @DefaultMessage("Path")
    @Key("pathCapitalized")
    String pathCapitalized();

    /**
     * Translated "Subject".
     *
     * @return translated "Subject"
     */
    @DefaultMessage("Subject")
    @Key("subjectCapitalized")
    String subjectCapitalized();

    /**
     * Translated "Author".
     *
     * @return translated "Author"
     */
    @DefaultMessage("Author")
    @Key("authorCapitalized")
    String authorCapitalized();

    /**
     * Translated "Date".
     *
     * @return translated "Date"
     */
    @DefaultMessage("Date")
    @Key("dateCapitalized")
    String dateCapitalized();

    /**
     * Translated "Staged".
     *
     * @return translated "Staged"
     */
    @DefaultMessage("Staged")
    @Key("stagedCapitalized")
    String stagedCapitalized();

    /**
     * Translated "Git Tab".
     *
     * @return translated "Git Tab"
     */
    @DefaultMessage("Git Tab")
    @Key("gitTabCapitalized")
    String gitTabCapitalized();

    /**
     * Translated "Pull options".
     *
     * @return translated "Pull options"
     */
    @DefaultMessage("Pull options")
    @Key("pullOptions")
    String pullOptions();

    /**
     * Translated "More".
     *
     * @return translated "More"
     */
    @DefaultMessage("More")
    @Key("moreCapitalized")
    String moreCapitalized();

    /**
     * Translated "Refresh Now".
     *
     * @return translated "Refresh Now"
     */
    @DefaultMessage("Refresh Now")
    @Key("refreshNowCapitalized")
    String refreshNowCapitalized();

    /**
     * Translated "Refresh options".
     *
     * @return translated "Refresh options"
     */
    @DefaultMessage("Refresh options")
    @Key("refreshOptions")
    String refreshOptions();

    /**
     * Translated "Pull".
     *
     * @return translated "Pull"
     */
    @DefaultMessage("Pull")
    @Key("pullCapitalized")
    String pullCapitalized();

    /**
     * Translated "Push".
     *
     * @return translated "Push"
     */
    @DefaultMessage("Push")
    @Key("pushCapitalized")
    String pushCapitalized();

    /**
     * Translated "No Changes to File".
     *
     * @return translated "No Changes to File"
     */
    @DefaultMessage("No Changes to File")
    @Key("noChangesToFile")
    String noChangesToFile();

    /**
     * Translated "There are no changes to the file \"{0}\" to diff.".
     *
     * @return translated "There are no changes to the file \"{0}\" to diff."
     */
    @DefaultMessage("There are no changes to the file \"{0}\" to diff.")
    @Key("noChangesToFileToDiff")
    String noChangesToFileToDiff(String filename);

    /**
     * Translated "No Changes to Revert".
     *
     * @return translated "No Changes to Revert"
     */
    @DefaultMessage("No Changes to Revert")
    @Key("noChangesToRevert")
    String noChangesToRevert();

    /**
     * Translated "There are no changes to the file \"{0}\" to revert.".
     *
     * @return translated "There are no changes to the file \"{0}\" to revert."
     */
    @DefaultMessage("There are no changes to the file \"{0}\" to revert.")
    @Key("noChangesToFileToRevert")
    String noChangesToFileToRevert(String filename);

    /**
     * Translated "Revert Changes".
     *
     * @return translated "Revert Changes"
     */
    @DefaultMessage("Revert Changes")
    @Key("revertChangesCapitalized")
    String revertChangesCapitalized();

    /**
     * Translated "Changes to the selected file will be lost, including staged changes.\n\nAre you sure you want to continue?".
     *
     * @return translated "Changes to the selected file will be lost, including staged changes.\n\nAre you sure you want to continue?"
     */
    @DefaultMessage("Changes to the selected file will be lost, including staged changes.\n\nAre you sure you want to continue?")
    @Key("changesToFileWillBeLost")
    String changesToFileWillBeLost();

    /**
     * Translated "Changes to the selected files will be lost, including staged changes.\n\nAre you sure you want to continue?".
     *
     * @return translated "Changes to the selected files will be lost, including staged changes.\n\nAre you sure you want to continue?"
     */
    @DefaultMessage("Changes to the selected files will be lost, including staged changes.\n\nAre you sure you want to continue?")
    @Key("changesToFileWillBeLostPlural")
    String changesToFileWillBeLostPlural();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    @Key("errorCapitalized")
    String errorCapitalized();

    /**
     * Translated "Unable to view {0} on GitHub.\n\nAre you sure that this file is on GitHub and is contained in the currently active project?".
     *
     * @return translated "Unable to view {0} on GitHub.\n\nAre you sure that this file is on GitHub and is contained in the currently active project?"
     */
    @DefaultMessage("Unable to view {0} on GitHub.\n\nAre you sure that this file is on GitHub and is contained in the currently active project?")
    @Key("unableToViewPathOnGithub")
    String unableToViewPathOnGithub(String path);

    /**
     * Translated "Git Ignore".
     *
     * @return translated "Git Ignore"
     */
    @DefaultMessage("Git Ignore")
    @Key("gitIgnoreCapitalized")
    String gitIgnoreCapitalized();

    /**
     * Translated "Commit".
     *
     * @return translated "Commit"
     */
    @DefaultMessage("Commit")
    @Key("commitCapitalized")
    String commitCapitalized();

    /**
     * Translated "Git Review".
     *
     * @return translated "Git Review"
     */
    @DefaultMessage("Git Review")
    @Key("gitReviewCapitalized")
    String gitReviewCapitalized();

    /**
     * Translated "Git Diff".
     *
     * @return translated "Git Diff"
     */
    @DefaultMessage("Git Diff")
    @Key("gitDiffCapitalized")
    String gitDiffCapitalized();

    /**
     * Translated "Stage".
     *
     * @return translated "Stage"
     */
    @DefaultMessage("Stage")
    @Key("stageCapitalized")
    String stageCapitalized();

    /**
     * Translated "Revert".
     *
     * @return translated "Revert"
     */
    @DefaultMessage("Revert")
    @Key("revertCapitalized")
    String revertCapitalized();

    /**
     * Translated "Ignore".
     *
     * @return translated "Ignore"
     */
    @DefaultMessage("Ignore")
    @Key("ignoreCapitalized")
    String ignoreCapitalized();

    /**
     * Translated "Stage All".
     *
     * @return translated "Stage All"
     */
    @DefaultMessage("Stage All")
    @Key("stageAllCapitalized")
    String stageAllCapitalized();

    /**
     * Translated "Discard All".
     *
     * @return translated "Discard All"
     */
    @DefaultMessage("Discard All")
    @Key("discardAllCapitalized")
    String discardAllCapitalized();

    /**
     * Translated "Unstage All".
     *
     * @return translated "Unstage All"
     */
    @DefaultMessage("Unstage All")
    @Key("unstageAllCapitalized")
    String unstageAllCapitalized();

    /**
     * Translated "Revert...".
     *
     * @return translated "Revert..."
     */
    @DefaultMessage("Revert...")
    @Key("revertEllipses")
    String revertEllipses();

    /**
     * Translated "Ignore...".
     *
     * @return translated "Ignore..."
     */
    @DefaultMessage("Ignore...")
    @Key("ignoreEllipses")
    String ignoreEllipses();

    /**
     * Translated "Open File".
     *
     * @return translated "Open File"
     */
    @DefaultMessage("Open File")
    @Key("openFileCapitalized")
    String openFileCapitalized();

    /**
     * Translated "{0} characters".
     * Character as in a letter in a string.
     * @return translated "{0} characters"
     */
    @DefaultMessage("{0} characters")
    @Key("lengthCharacters")
    String lengthCharacters(int length);

    /**
     * Translated "{0} characters in message".
     *
     * @return translated "{0} characters in message"
     */
    @DefaultMessage("{0} characters in message")
    @Key("lengthCharactersInMessage")
    String lengthCharactersInMessage(int length);

    /**
     * Translated "Unstage".
     *
     * @return translated "Unstage"
     */
    @DefaultMessage("Unstage")
    @Key("unstageCapitalized")
    String unstageCapitalized();

    /**
     * Translated "Discard".
     *
     * @return translated "Discard"
     */
    @DefaultMessage("Discard")
    @Key("discardCapitalized")
    String discardCapitalized();

    /**
     * Translated "All unstaged changes in this file will be lost.\n\nAre you sure you want to continue?".
     *
     * @return translated "All unstaged changes in this file will be lost.\n\nAre you sure you want to continue?"
     */
    @DefaultMessage("All unstaged changes in this file will be lost.\n\nAre you sure you want to continue?")
    @Key("allUnstagedChangesWillBeLost")
    String allUnstagedChangesWillBeLost();

    /**
     * Translated "The selected changes in this file will be lost.\n\nAre you sure you want to continue?".
     *
     * @return translated "The selected changes in this file will be lost.\n\nAre you sure you want to continue?"
     */
    @DefaultMessage("The selected changes in this file will be lost.\n\nAre you sure you want to continue?")
    @Key("theSelectedChangesWillBeLost")
    String theSelectedChangesWillBeLost();

    /**
     * Translated "Some of the files to be committed are quite large (>{0} in size). Are you sure you want to commit these files?".
     *
     * @return translated "Some of the files to be committed are quite large (>{0} in size). Are you sure you want to commit these files?"
     */
    @DefaultMessage("Some of the files to be committed are quite large (>{0} in size). Are you sure you want to commit these files?")
    @Key("someFilesAreQuiteLarge")
    String someFilesAreQuiteLarge(String prettySize);

    /**
     * Translated "Committing Large Files".
     *
     * @return translated "Committing Large Files"
     */
    @DefaultMessage("Committing Large Files")
    @Key("committingLargeFiles")
    String committingLargeFiles();

    /**
     * Translated "Diff Error".
     *
     * @return translated "Diff Error"
     */
    @DefaultMessage("Diff Error")
    @Key("diffError")
    String diffError();

    /**
     * Translated "Changelist".
     *
     * @return translated "Changelist"
     */
    @DefaultMessage("Changelist")
    @Key("changeList")
    String changeList();

    /**
     * Translated "SVN Ignore".
     *
     * @return translated "SVN Ignore"
     */
    @DefaultMessage("SVN Ignore")
    @Key("svnIgnore")
    String svnIgnore();

    /**
     * Translated "svn:ignore".
     *
     * @return translated "svn:ignore"
     */
    @DefaultMessage("svn:ignore")
    @Key("svnColonIgnore")
    String svnColonIgnore();

    /**
     * Translated "SVN Add".
     *
     * @return translated "SVN Add"
     */
    @DefaultMessage("SVN Add")
    @Key("svnAdd")
    String svnAdd();

    /**
     * Translated "SVN Delete".
     *
     * @return translated "SVN Delete"
     */
    @DefaultMessage("SVN Delete")
    @Key("svnDelete")
    String svnDelete();

    /**
     * Translated "Resolve".
     *
     * @return translated "Resolve"
     */
    @DefaultMessage("Resolve")
    @Key("resolveCapitalized")
    String resolveCapitalized();

    /**
     * Translated "SVN Resolve".
     *
     * @return translated "SVN Resolve"
     */
    @DefaultMessage("SVN Resolve")
    @Key("svnResolve")
    String svnResolve();

    /**
     * Translated "None of the selected paths appear to have conflicts.\n\nDo you want to resolve anyway?".
     *
     * @return translated "None of the selected paths appear to have conflicts.\n\nDo you want to resolve anyway?"
     */
    @DefaultMessage("None of the selected paths appear to have conflicts.\n\nDo you want to resolve anyway?")
    @Key("noneOfSelectedPathsHaveConflicts")
    String noneOfSelectedPathsHaveConflicts();

    /**
     * Translated "The selected path does not appear to have conflicts.\n\nDo you want to resolve anyway?".
     *
     * @return translated "The selected path does not appear to have conflicts.\n\nDo you want to resolve anyway?"
     */
    @DefaultMessage("The selected path does not appear to have conflicts.\n\nDo you want to resolve anyway?")
    @Key("selectedPathDoesNotAppearToHaveConflicts")
    String selectedPathDoesNotAppearToHaveConflicts();

    /**
     * Translated "No Conflicts Detected".
     *
     * @return translated "No Conflicts Detected"
     */
    @DefaultMessage("No Conflicts Detected")
    @Key("noConflictsDetected")
    String noConflictsDetected();

    /**
     * Translated "Changes to the selected file will be reverted.\n\nAre you sure you want to continue?".
     *
     * @return translated "Changes to the selected file will be reverted.\n\nAre you sure you want to continue?"
     */
    @DefaultMessage("Changes to the selected file will be reverted.\n\nAre you sure you want to continue?")
    @Key("changesToSelectedFileWillBeReverted")
    String changesToSelectedFileWillBeReverted();

    /**
     * Translated "Changes to the selected files will be reverted.\n\nAre you sure you want to continue?".
     *
     * @return translated "Changes to the selected files will be reverted.\n\nAre you sure you want to continue?"
     */
    @DefaultMessage("Changes to the selected files will be reverted.\n\nAre you sure you want to continue?")
    @Key("changesToSelectedFileWillBeRevertedPlural")
    String changesToSelectedFileWillBeRevertedPlural();

    /**
     * Translated "SVN Revert".
     *
     * @return translated "SVN Revert"
     */
    @DefaultMessage("SVN Revert")
    @Key("svnRevert")
    String svnRevert();

    /**
     * Translated "SVN Tab".
     *
     * @return translated "SVN Tab"
     */
    @DefaultMessage("SVN Tab")
    @Key("svnTab")
    String svnTab();

    /**
     * Translated "SVN Cleanup".
     *
     * @return translated "SVN Cleanup"
     */
    @DefaultMessage("SVN Cleanup")
    @Key("svnCleanup")
    String svnCleanup();

    /**
     * Translated "Cleaning up working directory...".
     *
     * @return translated "Cleaning up working directory..."
     */
    @DefaultMessage("Cleaning up working directory...")
    @Key("cleaningUpWorkingDirectoryEllipses")
    String cleaningUpWorkingDirectoryEllipses();

    /**
     * Translated "There are no changes to the file \"{0}\" to diff.".
     *
     * @return translated "There are no changes to the file \"{0}\" to diff."
     */
    @DefaultMessage("There are no changes to the file \"{0}\" to diff.")
    @Key("noChangesToFileTODiff")
    String noChangesToFileTODiff(String fileName);

    /**
     * Translated "path".
     *
     * @return translated "path"
     */
    @DefaultMessage("path")
    @Key("path")
    String path();

    /**
     * Translated "paths".
     *
     * @return translated "paths"
     */
    @DefaultMessage("paths")
    @Key("paths")
    String paths();

    /**
     * Translated "File Conflict".
     *
     * @return translated "File Conflict"
     */
    @DefaultMessage("File Conflict")
    @Key("fileConflictCapitalized")
    String fileConflictCapitalized();

    /**
     * Translated "This file has a conflict. Would you like to mark it as resolved now?".
     *
     * @return translated "This file has a conflict. Would you like to mark it as resolved now?"
     */
    @DefaultMessage("This file has a conflict. Would you like to mark it as resolved now?")
    @Key("fileConflictMarkAsResolved")
    String fileConflictMarkAsResolved();

    /**
     * Translated "SVN Commit".
     *
     * @return translated "SVN Commit"
     */
    @DefaultMessage("SVN Commit")
    @Key("svnCommit")
    String svnCommit();

    /**
     * Translated "No Items Selected".
     *
     * @return translated "No Items Selected"
     */
    @DefaultMessage("No Items Selected")
    @Key("noItemsSelectedCapitalized")
    String noItemsSelectedCapitalized();

    /**
     * Translated "Please select one or more items to commit.".
     *
     * @return translated "Please select one or more items to commit."
     */
    @DefaultMessage("Please select one or more items to commit.")
    @Key("selectOneOrMoreItemsToCommit")
    String selectOneOrMoreItemsToCommit();

    /**
     * Translated "Message Required".
     *
     * @return translated "Message Required"
     */
    @DefaultMessage("Message Required")
    @Key("messageRequiredCapitalized")
    String messageRequiredCapitalized();

    /**
     * Translated "Please provide a commit message.".
     *
     * @return translated "Please provide a commit message."
     */
    @DefaultMessage("Please provide a commit message.")
    @Key("provideACommitMessage")
    String provideACommitMessage();

    /**
     * Translated "Revision".
     *
     * @return translated "Revision"
     */
    @DefaultMessage("Revision")
    @Key("revisionCapitalized")
    String revisionCapitalized();

    /**
     * Translated "SVN Review".
     *
     * @return translated "SVN Review"
     */
    @DefaultMessage("SVN Review")
    @Key("svnReview")
    String svnReview();

    /**
     * Translated "SVN Diff".
     *
     * @return translated "SVN Diff"
     */
    @DefaultMessage("SVN Diff")
    @Key("svnDiff")
    String svnDiff();

    /**
     * Translated "Refresh".
     *
     * @return translated "Refresh"
     */
    @DefaultMessage("Refresh")
    @Key("refreshCapitalized")
    String refreshCapitalized();

    /**
     * Translated "Update".
     *
     * @return translated "Update"
     */
    @DefaultMessage("Update")
    @Key("updateCapitalized")
    String updateCapitalized();

    /**
     * Translated "All changes in this file will be lost.\n\nAre you sure you want to continue?".
     *
     * @return translated "All changes in this file will be lost.\n\nAre you sure you want to continue?"
     */
    @DefaultMessage("All changes in this file will be lost.\n\nAre you sure you want to continue?")
    @Key("allChangesInFileWillBeLost")
    String allChangesInFileWillBeLost();

    /**
     * Translated "The selected changes in this file will be lost.\n\nAre you sure you want to continue?".
     *
     * @return translated "The selected changes in this file will be lost.\n\nAre you sure you want to continue?"
     */
    @DefaultMessage("The selected changes in this file will be lost.\n\nAre you sure you want to continue?")
    @Key("selectedChangesInFileWillBeLost")
    String selectedChangesInFileWillBeLost();

    /**
     * Translated "commit".
     *
     * @return translated "commit"
     */
    @DefaultMessage("commit")
    @Key("commit")
    String commit();

    /**
     * Translated "diff".
     *
     * @return translated "diff"
     */
    @DefaultMessage("diff")
    @Key("diff")
    String diff();

    /**
     * Translated "Added".
     *
     * @return translated "Added"
     */
    @DefaultMessage("Added")
    @Key("addedCapitalized")
    String addedCapitalized();

    /**
     * Translated "Modified".
     *
     * @return translated "Modified"
     */
    @DefaultMessage("Modified")
    @Key("modifiedCapitalized")
    String modifiedCapitalized();

    /**
     * Translated "Deleted".
     *
     * @return translated "Deleted"
     */
    @DefaultMessage("Deleted")
    @Key("deletedCapitalized")
    String deletedCapitalized();

    /**
     * Translated "Renamed".
     *
     * @return translated "Renamed"
     */
    @DefaultMessage("Renamed")
    @Key("renamedCapitalized")
    String renamedCapitalized();

    /**
     * Translated "Copied".
     *
     * @return translated "Copied"
     */
    @DefaultMessage("Copied")
    @Key("copiedCapitalized")
    String copiedCapitalized();

    /**
     * Translated "Untracked".
     *
     * @return translated "Untracked"
     */
    @DefaultMessage("Untracked")
    @Key("untrackedCapitalized")
    String untrackedCapitalized();

    /**
     * Translated "Unmerged".
     *
     * @return translated "Unmerged"
     */
    @DefaultMessage("Unmerged")
    @Key("unmergedCapitalized")
    String unmergedCapitalized();

    /**
     * Translated "Conflicted".
     *
     * @return translated "Conflicted"
     */
    @DefaultMessage("Conflicted")
    @Key("conflictedCapitalized")
    String conflictedCapitalized();

    /**
     * Translated "External".
     *
     * @return translated "External"
     */
    @DefaultMessage("External")
    @Key("externalCapitalized")
    String externalCapitalized();

    /**
     * Translated "Ignored".
     *
     * @return translated "Ignored"
     */
    @DefaultMessage("Ignored")
    @Key("ignoredCapitalized")
    String ignoredCapitalized();

    /**
     * Translated "Missing".
     *
     * @return translated "Missing"
     */
    @DefaultMessage("Missing")
    @Key("missingCapitalized")
    String missingCapitalized();

    /**
     * Translated "Obstructed".
     *
     * @return translated "Obstructed"
     */
    @DefaultMessage("Obstructed")
    @Key("obstructedCapitalized")
    String obstructedCapitalized();

    /**
     * Translated "Unversioned".
     *
     * @return translated "Unversioned"
     */
    @DefaultMessage("Unversioned")
    @Key("unversionedCapitalized")
    String unversionedCapitalized();
}
