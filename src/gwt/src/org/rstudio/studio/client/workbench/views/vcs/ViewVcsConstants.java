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

    @DefaultMessage("Add")
    @Key("addCapitalized")
    String addCapitalized();

    @DefaultMessage("Remote Name:")
    @Key("remoteNameColon")
    String remoteNameColon();

    @DefaultMessage("Remote URL:")
    @Key("remoteUrlColon")
    String remoteUrlColon();

    @DefaultMessage("Switch branch")
    @Key("switchBranch")
    String switchBranch();

    @DefaultMessage("Search by branch name")
    @Key("searchByBranchName")
    String searchByBranchName();

    @DefaultMessage("(Remote: {0})")
    @Key("remoteBranchCaption")
    String remoteBranchCaption(String caption);

    @DefaultMessage("(no branch)")
    @Key("noBranchParentheses")
    String noBranchParentheses();

    @DefaultMessage("(no branches available)")
    @Key("noBranchesAvailableParentheses")
    String noBranchesAvailableParentheses();

    @DefaultMessage("(local branches)")
    @Key("localBranchesParentheses")
    String localBranchesParentheses();

    @DefaultMessage("Create")
    @Key("createCapitalized")
    String createCapitalized();

    @DefaultMessage("Remote:")
    @Key("remoteColon")
    String remoteColon();

    @DefaultMessage("Add Remote...")
    @Key("addRemoteEllipses")
    String addRemoteEllipses();

    @DefaultMessage("Add Remote")
    @Key("addRemote")
    String addRemote();

    @DefaultMessage("Sync branch with remote")
    @Key("syncBranchWithRemote")
    String syncBranchWithRemote();

    @DefaultMessage("Branch Name:")
    @Key("branchNameColon")
    String branchNameColon();

    @DefaultMessage("New Branch")
    @Key("newBranchCapitalized")
    String newBranchCapitalized();

    @DefaultMessage("A local branch named ''{0}'' already exists. Would you like to check out that branch, or overwrite it?")
    @Key("localBranchAlreadyExists")
    String localBranchAlreadyExists(String branchName);

    @DefaultMessage("Checkout")
    @Key("checkoutCapitalized")
    String checkoutCapitalized();

    @DefaultMessage("Overwrite")
    @Key("overwriteCapitalized")
    String overwriteCapitalized();

    @DefaultMessage("Cancel")
    @Key("cancelCapitalized")
    String cancelCapitalized();

    @DefaultMessage("Local Branch Already Exists")
    @Key("localBranchAlreadyExistsCaption")
    String localBranchAlreadyExistsCaption();

    @DefaultMessage("A remote branch named ''{0}'' already exists on the remote repository ''{1}''. Would you like to check out that branch?")
    @Key("remoteBranchNameAlreadyExists")
    String remoteBranchNameAlreadyExists(String remoteBranch, String remote);

    @DefaultMessage("Remote Branch Already Exists")
    @Key("remoteBranchAlreadyExistsCaption")
    String remoteBranchAlreadyExistsCaption();

    @DefaultMessage("(all branches)")
    @Key("allBranchesParentheses")
    String allBranchesParentheses();

    @DefaultMessage("Close")
    @Key("closeCapitalized")
    String closeCapitalized();

    @DefaultMessage("Stop")
    @Key("stopCapitalized")
    String stopCapitalized();

    @DefaultMessage("Progress details")
    @Key("progressDetails")
    String progressDetails();

    @DefaultMessage("Commits {0}")
    @Key("commitsPager")
    String commitsPager(String wordText);

    @DefaultMessage("{0} selection")
    @Key("selectionSuffix")
    String selectionSuffix(String emptyString);

    @DefaultMessage("{0} line")
    @Key("lineSuffix")
    String lineSuffix(String emptyString);

    @DefaultMessage("{0} chunk")
    @Key("chunkSuffix")
    String chunkSuffix(String emptyString);

    @DefaultMessage("(No commit selected)")
    @Key("noCommitSelectedParentheses")
    String noCommitSelectedParentheses();

    @DefaultMessage("(all commits)")
    @Key("allCommitsParentheses")
    String allCommitsParentheses();

    @DefaultMessage("Filter by File...")
    @Key("filterByFileEllipses")
    String filterByFileEllipses();

    @DefaultMessage("Choose File")
    @Key("chooseFileCapitalized")
    String chooseFileCapitalized();

    @DefaultMessage("Filter by Directory...")
    @Key("filterByDirectoryEllipses")
    String filterByDirectoryEllipses();

    @DefaultMessage("Choose Folder")
    @Key("chooseFolderCapitalized")
    String chooseFolderCapitalized();

    @DefaultMessage("Filter: {0}")
    @Key("filterColonPath")
    String filterColonPath(String path);

    @DefaultMessage("Filter: (None)")
    @Key("filterColonNone")
    String filterColonNone();

    @DefaultMessage("View file @ {0}")
    @Key("viewFileAtString")
    String viewFileAtString(String substr);

    @DefaultMessage("Error Fetching History")
    @Key("errorFetchingHistory")
    String errorFetchingHistory();

    @DefaultMessage("History")
    @Key("historyCapitalized")
    String historyCapitalized();

    @DefaultMessage("Changes")
    @Key("changesCapitalized")
    String changesCapitalized();

    @DefaultMessage("Search version control history")
    @Key("searchVersionControlHistory")
    String searchVersionControlHistory();

    @DefaultMessage("Search")
    @Key("searchCapitalized")
    String searchCapitalized();

    @DefaultMessage("Reading file...")
    @Key("readingFileEllipses")
    String readingFileEllipses();

    @DefaultMessage("Show History")
    @Key("showHistoryCapitalized")
    String showHistoryCapitalized();

    @DefaultMessage("Show Diff")
    @Key("showDiffCapitalized")
    String showDiffCapitalized();

    @DefaultMessage("commit depth {0}")
    @Key("commitDepthAltText")
    String commitDepthAltText(int nexusColumn);

    @DefaultMessage("Your branch is ahead of ''{0}'' by {1} commit.")
    @Key("branchAheadOfRemoteSingular")
    String branchAheadOfRemoteSingular(String remoteName, int commitBehind);

    @DefaultMessage("Your branch is ahead of ''{0}'' by {1} commits.")
    @Key("branchAheadOfRemotePlural")
    String branchAheadOfRemotePlural(String remoteName, int commitBehind);

    @DefaultMessage("Status")
    @Key("statusCapitalized")
    String statusCapitalized();

    @DefaultMessage("Path")
    @Key("pathCapitalized")
    String pathCapitalized();

    @DefaultMessage("Subject")
    @Key("subjectCapitalized")
    String subjectCapitalized();

    @DefaultMessage("Author")
    @Key("authorCapitalized")
    String authorCapitalized();

    @DefaultMessage("Date")
    @Key("dateCapitalized")
    String dateCapitalized();

    @DefaultMessage("Staged")
    @Key("stagedCapitalized")
    String stagedCapitalized();

    @DefaultMessage("Git Tab")
    @Key("gitTabCapitalized")
    String gitTabCapitalized();

    @DefaultMessage("Pull options")
    @Key("pullOptions")
    String pullOptions();

    @DefaultMessage("More")
    @Key("moreCapitalized")
    String moreCapitalized();

    @DefaultMessage("Refresh Now")
    @Key("refreshNowCapitalized")
    String refreshNowCapitalized();

    @DefaultMessage("Refresh options")
    @Key("refreshOptions")
    String refreshOptions();

    @DefaultMessage("Pull")
    @Key("pullCapitalized")
    String pullCapitalized();

    @DefaultMessage("Push")
    @Key("pushCapitalized")
    String pushCapitalized();

    @DefaultMessage("No Changes to File")
    @Key("noChangesToFile")
    String noChangesToFile();

    @DefaultMessage("There are no changes to the file \"{0}\" to diff.")
    @Key("noChangesToFileToDiff")
    String noChangesToFileToDiff(String filename);

    @DefaultMessage("No Changes to Revert")
    @Key("noChangesToRevert")
    String noChangesToRevert();

    @DefaultMessage("There are no changes to the file \"{0}\" to revert.")
    @Key("noChangesToFileToRevert")
    String noChangesToFileToRevert(String filename);

    @DefaultMessage("Revert Changes")
    @Key("revertChangesCapitalized")
    String revertChangesCapitalized();

    @DefaultMessage("Changes to the selected file will be lost, including staged changes.\n\nAre you sure you want to continue?")
    @Key("changesToFileWillBeLost")
    String changesToFileWillBeLost();

    @DefaultMessage("Changes to the selected files will be lost, including staged changes.\n\nAre you sure you want to continue?")
    @Key("changesToFileWillBeLostPlural")
    String changesToFileWillBeLostPlural();

    @DefaultMessage("Error")
    @Key("errorCapitalized")
    String errorCapitalized();

    @DefaultMessage("Unable to view {0} on GitHub.\n\nAre you sure that this file is on GitHub and is contained in the currently active project?")
    @Key("unableToViewPathOnGithub")
    String unableToViewPathOnGithub(String path);

    @DefaultMessage("Git Ignore")
    @Key("gitIgnoreCapitalized")
    String gitIgnoreCapitalized();

    @DefaultMessage("Diff")
    @Key("diffCapitalized")
    String diffCapitalized();

    @DefaultMessage("Commit")
    @Key("commitCapitalized")
    String commitCapitalized();

    @DefaultMessage("Git Review")
    @Key("gitReviewCapitalized")
    String gitReviewCapitalized();

    @DefaultMessage("Git Diff")
    @Key("gitDiffCapitalized")
    String gitDiffCapitalized();

    @DefaultMessage("Stage")
    @Key("stageCapitalized")
    String stageCapitalized();

    @DefaultMessage("Revert")
    @Key("revertCapitalized")
    String revertCapitalized();

    @DefaultMessage("Ignore")
    @Key("ignoreCapitalized")
    String ignoreCapitalized();

    @DefaultMessage("Stage All")
    @Key("stageAllCapitalized")
    String stageAllCapitalized();

    @DefaultMessage("Discard All")
    @Key("discardAllCapitalized")
    String discardAllCapitalized();

    @DefaultMessage("Unstage All")
    @Key("unstageAllCapitalized")
    String unstageAllCapitalized();

    @DefaultMessage("Revert...")
    @Key("revertEllipses")
    String revertEllipses();

    @DefaultMessage("Ignore...")
    @Key("ignoreEllipses")
    String ignoreEllipses();

    @DefaultMessage("Open File")
    @Key("openFileCapitalized")
    String openFileCapitalized();

    @DefaultMessage("{0} characters")
    @Key("lengthCharacters")
    String lengthCharacters(int length);

    @DefaultMessage("{0} characters in message")
    @Key("lengthCharactersInMessage")
    String lengthCharactersInMessage(int length);

    @DefaultMessage("Unstage")
    @Key("unstageCapitalized")
    String unstageCapitalized();

    @DefaultMessage("Discard")
    @Key("discardCapitalized")
    String discardCapitalized();

    @DefaultMessage("All unstaged changes in this file will be lost.\n\nAre you sure you want to continue?")
    @Key("allUnstagedChangesWillBeLost")
    String allUnstagedChangesWillBeLost();

    @DefaultMessage("The selected changes in this file will be lost.\n\nAre you sure you want to continue?")
    @Key("theSelectedChangesWillBeLost")
    String theSelectedChangesWillBeLost();

    @DefaultMessage("Some of the files to be committed are quite large (>{0} in size). Are you sure you want to commit these files?")
    @Key("someFilesAreQuiteLarge")
    String someFilesAreQuiteLarge(String prettySize);

    @DefaultMessage("Committing Large Files")
    @Key("committingLargeFiles")
    String committingLargeFiles();

    @DefaultMessage("Diff Error")
    @Key("diffError")
    String diffError();

    @DefaultMessage("Changelist")
    @Key("changeList")
    String changeList();

    @DefaultMessage("SVN Ignore")
    @Key("svnIgnore")
    String svnIgnore();

    @DefaultMessage("svn:ignore")
    @Key("svnColonIgnore")
    String svnColonIgnore();

    @DefaultMessage("SVN Add")
    @Key("svnAdd")
    String svnAdd();

    @DefaultMessage("SVN Delete")
    @Key("svnDelete")
    String svnDelete();

    @DefaultMessage("Resolve")
    @Key("resolveCapitalized")
    String resolveCapitalized();

    @DefaultMessage("SVN Resolve")
    @Key("svnResolve")
    String svnResolve();

    @DefaultMessage("None of the selected paths appear to have conflicts.\n\nDo you want to resolve anyway?")
    @Key("noneOfSelectedPathsHaveConflicts")
    String noneOfSelectedPathsHaveConflicts();

    @DefaultMessage("The selected path does not appear to have conflicts.\n\nDo you want to resolve anyway?")
    @Key("selectedPathDoesNotAppearToHaveConflicts")
    String selectedPathDoesNotAppearToHaveConflicts();

    @DefaultMessage("No Conflicts Detected")
    @Key("noConflictsDetected")
    String noConflictsDetected();

    @DefaultMessage("Changes to the selected file will be reverted.\n\nAre you sure you want to continue?")
    @Key("changesToSelectedFileWillBeReverted")
    String changesToSelectedFileWillBeReverted();

    @DefaultMessage("Changes to the selected files will be reverted.\n\nAre you sure you want to continue?")
    @Key("changesToSelectedFileWillBeRevertedPlural")
    String changesToSelectedFileWillBeRevertedPlural();

    @DefaultMessage("SVN Revert")
    @Key("svnRevert")
    String svnRevert();

    @DefaultMessage("SVN Tab")
    @Key("svnTab")
    String svnTab();

    @DefaultMessage("SVN Cleanup")
    @Key("svnCleanup")
    String svnCleanup();

    @DefaultMessage("Cleaning up working directory...")
    @Key("cleaningUpWorkingDirectoryEllipses")
    String cleaningUpWorkingDirectoryEllipses();

    @DefaultMessage("There are no changes to the file \"{0}\" to diff.")
    @Key("noChangesToFileTODiff")
    String noChangesToFileTODiff(String fileName);

    @DefaultMessage("path")
    @Key("path")
    String path();

    @DefaultMessage("paths")
    @Key("paths")
    String paths();

    @DefaultMessage("File Conflict")
    @Key("fileConflictCapitalized")
    String fileConflictCapitalized();

    @DefaultMessage("This file has a conflict. Would you like to mark it as resolved now?")
    @Key("fileConflictMarkAsResolved")
    String fileConflictMarkAsResolved();

    @DefaultMessage("SVN Commit")
    @Key("svnCommit")
    String svnCommit();

    @DefaultMessage("No Items Selected")
    @Key("noItemsSelectedCapitalized")
    String noItemsSelectedCapitalized();

    @DefaultMessage("Please select one or more items to commit.")
    @Key("selectOneOrMoreItemsToCommit")
    String selectOneOrMoreItemsToCommit();

    @DefaultMessage("Message Required")
    @Key("messageRequiredCapitalized")
    String messageRequiredCapitalized();

    @DefaultMessage("Please provide a commit message.")
    @Key("provideACommitMessage")
    String provideACommitMessage();

    @DefaultMessage("Revision")
    @Key("revisionCapitalized")
    String revisionCapitalized();

    @DefaultMessage("SVN Review")
    @Key("svnReview")
    String svnReview();

    @DefaultMessage("SVN Diff")
    @Key("svnDiff")
    String svnDiff();

    @DefaultMessage("Refresh")
    @Key("refreshCapitalized")
    String refreshCapitalized();

    @DefaultMessage("Update")
    @Key("updateCapitalized")
    String updateCapitalized();

    @DefaultMessage("All changes in this file will be lost.\n\nAre you sure you want to continue?")
    @Key("allChangesInFileWillBeLost")
    String allChangesInFileWillBeLost();

    @DefaultMessage("The selected changes in this file will be lost.\n\nAre you sure you want to continue?")
    @Key("selectedChangesInFileWillBeLost")
    String selectedChangesInFileWillBeLost();

    @DefaultMessage("commit")
    @Key("commit")
    String commit();

    @DefaultMessage("diff")
    @Key("diff")
    String diff();

    @DefaultMessage("Added")
    @Key("addedCapitalized")
    String addedCapitalized();

    @DefaultMessage("Modified")
    @Key("modifiedCapitalized")
    String modifiedCapitalized();

    @DefaultMessage("Deleted")
    @Key("deletedCapitalized")
    String deletedCapitalized();

    @DefaultMessage("Renamed")
    @Key("renamedCapitalized")
    String renamedCapitalized();

    @DefaultMessage("Copied")
    @Key("copiedCapitalized")
    String copiedCapitalized();

    @DefaultMessage("Untracked")
    @Key("untrackedCapitalized")
    String untrackedCapitalized();

    @DefaultMessage("Unmerged")
    @Key("unmergedCapitalized")
    String unmergedCapitalized();

    @DefaultMessage("Conflicted")
    @Key("conflictedCapitalized")
    String conflictedCapitalized();

    @DefaultMessage("External")
    @Key("externalCapitalized")
    String externalCapitalized();

    @DefaultMessage("Ignored")
    @Key("ignoredCapitalized")
    String ignoredCapitalized();

    @DefaultMessage("Missing")
    @Key("missingCapitalized")
    String missingCapitalized();

    @DefaultMessage("Obstructed")
    @Key("obstructedCapitalized")
    String obstructedCapitalized();

    @DefaultMessage("Unversioned")
    @Key("unversionedCapitalized")
    String unversionedCapitalized();
}
