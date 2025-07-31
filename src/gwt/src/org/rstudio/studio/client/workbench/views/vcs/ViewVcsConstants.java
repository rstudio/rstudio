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

    @Key("addCapitalized")
    String addCapitalized();

    @Key("remoteNameColon")
    String remoteNameColon();

    @Key("remoteUrlColon")
    String remoteUrlColon();

    @Key("switchBranch")
    String switchBranch();

    @Key("searchByBranchName")
    String searchByBranchName();

    @Key("remoteBranchCaption")
    String remoteBranchCaption(String caption);

    @Key("noBranchParentheses")
    String noBranchParentheses();

    @Key("noBranchesAvailableParentheses")
    String noBranchesAvailableParentheses();

    @Key("localBranchesParentheses")
    String localBranchesParentheses();

    @Key("createCapitalized")
    String createCapitalized();

    @Key("remoteColon")
    String remoteColon();

    @Key("addRemoteEllipses")
    String addRemoteEllipses();

    @Key("addRemote")
    String addRemote();

    @Key("syncBranchWithRemote")
    String syncBranchWithRemote();

    @Key("branchNameColon")
    String branchNameColon();

    @Key("newBranchCapitalized")
    String newBranchCapitalized();

    @Key("localBranchAlreadyExists")
    String localBranchAlreadyExists(String branchName);

    @Key("checkoutCapitalized")
    String checkoutCapitalized();

    @Key("overwriteCapitalized")
    String overwriteCapitalized();

    @Key("cancelCapitalized")
    String cancelCapitalized();

    @Key("localBranchAlreadyExistsCaption")
    String localBranchAlreadyExistsCaption();

    @Key("remoteBranchNameAlreadyExists")
    String remoteBranchNameAlreadyExists(String remoteBranch, String remote);

    @Key("remoteBranchAlreadyExistsCaption")
    String remoteBranchAlreadyExistsCaption();

    @Key("allBranchesParentheses")
    String allBranchesParentheses();

    @Key("closeCapitalized")
    String closeCapitalized();

    @Key("stopCapitalized")
    String stopCapitalized();

    @Key("progressDetails")
    String progressDetails();

    @Key("commitsPager")
    String commitsPager(String wordText);

    @Key("selectionSuffix")
    String selectionSuffix(String emptyString);

    @Key("lineSuffix")
    String lineSuffix(String emptyString);

    @Key("chunkSuffix")
    String chunkSuffix(String emptyString);

    @Key("noCommitSelectedParentheses")
    String noCommitSelectedParentheses();

    @Key("allCommitsParentheses")
    String allCommitsParentheses();

    @Key("filterByFileEllipses")
    String filterByFileEllipses();

    @Key("chooseFileCapitalized")
    String chooseFileCapitalized();

    @Key("filterByDirectoryEllipses")
    String filterByDirectoryEllipses();

    @Key("chooseFolderCapitalized")
    String chooseFolderCapitalized();

    @Key("filterColonPath")
    String filterColonPath(String path);

    @Key("filterColonNone")
    String filterColonNone();

    @Key("viewFileAtString")
    String viewFileAtString(String substr);

    @Key("errorFetchingHistory")
    String errorFetchingHistory();

    @Key("historyCapitalized")
    String historyCapitalized();

    @Key("changesCapitalized")
    String changesCapitalized();

    @Key("searchVersionControlHistory")
    String searchVersionControlHistory();

    @Key("searchCapitalized")
    String searchCapitalized();

    @Key("readingFileEllipses")
    String readingFileEllipses();

    @Key("showHistoryCapitalized")
    String showHistoryCapitalized();

    @Key("showDiffCapitalized")
    String showDiffCapitalized();

    @Key("commitDepthAltText")
    String commitDepthAltText(int nexusColumn);

    @Key("branchAheadOfRemoteSingular")
    String branchAheadOfRemoteSingular(String remoteName, int commitBehind);

    @Key("branchAheadOfRemotePlural")
    String branchAheadOfRemotePlural(String remoteName, int commitBehind);

    @Key("statusCapitalized")
    String statusCapitalized();

    @Key("pathCapitalized")
    String pathCapitalized();

    @Key("subjectCapitalized")
    String subjectCapitalized();

    @Key("authorCapitalized")
    String authorCapitalized();

    @Key("dateCapitalized")
    String dateCapitalized();

    @Key("stagedCapitalized")
    String stagedCapitalized();

    @Key("gitTabCapitalized")
    String gitTabCapitalized();

    @Key("pullOptions")
    String pullOptions();

    @Key("moreCapitalized")
    String moreCapitalized();

    @Key("refreshNowCapitalized")
    String refreshNowCapitalized();

    @Key("refreshOptions")
    String refreshOptions();

    @Key("pullCapitalized")
    String pullCapitalized();

    @Key("pushCapitalized")
    String pushCapitalized();

    @Key("noChangesToFile")
    String noChangesToFile();

    @Key("noChangesToFileToDiff")
    String noChangesToFileToDiff(String filename);

    @Key("noChangesToRevert")
    String noChangesToRevert();

    @Key("noChangesToFileToRevert")
    String noChangesToFileToRevert(String filename);

    @Key("revertChangesCapitalized")
    String revertChangesCapitalized();

    @Key("changesToFileWillBeLost")
    String changesToFileWillBeLost();

    @Key("changesToFileWillBeLostPlural")
    String changesToFileWillBeLostPlural();

    @Key("errorCapitalized")
    String errorCapitalized();

    @Key("unableToViewPathOnGithub")
    String unableToViewPathOnGithub(String path);

    @Key("gitIgnoreCapitalized")
    String gitIgnoreCapitalized();

    @Key("diffCapitalized")
    String diffCapitalized();

    @Key("commitCapitalized")
    String commitCapitalized();

    @Key("gitReviewCapitalized")
    String gitReviewCapitalized();

    @Key("gitDiffCapitalized")
    String gitDiffCapitalized();

    @Key("stageCapitalized")
    String stageCapitalized();

    @Key("revertCapitalized")
    String revertCapitalized();

    @Key("ignoreCapitalized")
    String ignoreCapitalized();

    @Key("stageAllCapitalized")
    String stageAllCapitalized();

    @Key("discardAllCapitalized")
    String discardAllCapitalized();

    @Key("unstageAllCapitalized")
    String unstageAllCapitalized();

    @Key("revertEllipses")
    String revertEllipses();

    @Key("ignoreEllipses")
    String ignoreEllipses();

    @Key("openFileCapitalized")
    String openFileCapitalized();

    @Key("lengthCharacters")
    String lengthCharacters(int length);

    @Key("lengthCharactersInMessage")
    String lengthCharactersInMessage(int length);

    @Key("unstageCapitalized")
    String unstageCapitalized();

    @Key("discardCapitalized")
    String discardCapitalized();

    @Key("allUnstagedChangesWillBeLost")
    String allUnstagedChangesWillBeLost();

    @Key("theSelectedChangesWillBeLost")
    String theSelectedChangesWillBeLost();

    @Key("someFilesAreQuiteLarge")
    String someFilesAreQuiteLarge(String prettySize);

    @Key("committingLargeFiles")
    String committingLargeFiles();

    @Key("diffError")
    String diffError();

    @Key("changeList")
    String changeList();

    @Key("svnIgnore")
    String svnIgnore();

    @Key("svnColonIgnore")
    String svnColonIgnore();

    @Key("svnAdd")
    String svnAdd();

    @Key("svnDelete")
    String svnDelete();

    @Key("resolveCapitalized")
    String resolveCapitalized();

    @Key("svnResolve")
    String svnResolve();

    @Key("noneOfSelectedPathsHaveConflicts")
    String noneOfSelectedPathsHaveConflicts();

    @Key("selectedPathDoesNotAppearToHaveConflicts")
    String selectedPathDoesNotAppearToHaveConflicts();

    @Key("noConflictsDetected")
    String noConflictsDetected();

    @Key("changesToSelectedFileWillBeReverted")
    String changesToSelectedFileWillBeReverted();

    @Key("changesToSelectedFileWillBeRevertedPlural")
    String changesToSelectedFileWillBeRevertedPlural();

    @Key("svnRevert")
    String svnRevert();

    @Key("svnTab")
    String svnTab();

    @Key("svnCleanup")
    String svnCleanup();

    @Key("cleaningUpWorkingDirectoryEllipses")
    String cleaningUpWorkingDirectoryEllipses();

    @Key("noChangesToFileTODiff")
    String noChangesToFileTODiff(String fileName);

    @Key("path")
    String path();

    @Key("paths")
    String paths();

    @Key("fileConflictCapitalized")
    String fileConflictCapitalized();

    @Key("fileConflictMarkAsResolved")
    String fileConflictMarkAsResolved();

    @Key("svnCommit")
    String svnCommit();

    @Key("noItemsSelectedCapitalized")
    String noItemsSelectedCapitalized();

    @Key("selectOneOrMoreItemsToCommit")
    String selectOneOrMoreItemsToCommit();

    @Key("messageRequiredCapitalized")
    String messageRequiredCapitalized();

    @Key("provideACommitMessage")
    String provideACommitMessage();

    @Key("revisionCapitalized")
    String revisionCapitalized();

    @Key("svnReview")
    String svnReview();

    @Key("svnDiff")
    String svnDiff();

    @Key("refreshCapitalized")
    String refreshCapitalized();

    @Key("updateCapitalized")
    String updateCapitalized();

    @Key("allChangesInFileWillBeLost")
    String allChangesInFileWillBeLost();

    @Key("selectedChangesInFileWillBeLost")
    String selectedChangesInFileWillBeLost();

    @Key("commit")
    String commit();

    @Key("diff")
    String diff();

    @Key("addedCapitalized")
    String addedCapitalized();

    @Key("modifiedCapitalized")
    String modifiedCapitalized();

    @Key("deletedCapitalized")
    String deletedCapitalized();

    @Key("renamedCapitalized")
    String renamedCapitalized();

    @Key("copiedCapitalized")
    String copiedCapitalized();

    @Key("untrackedCapitalized")
    String untrackedCapitalized();

    @Key("unmergedCapitalized")
    String unmergedCapitalized();

    @Key("conflictedCapitalized")
    String conflictedCapitalized();

    @Key("externalCapitalized")
    String externalCapitalized();

    @Key("ignoredCapitalized")
    String ignoredCapitalized();

    @Key("missingCapitalized")
    String missingCapitalized();

    @Key("obstructedCapitalized")
    String obstructedCapitalized();

    @Key("unversionedCapitalized")
    String unversionedCapitalized();
}
