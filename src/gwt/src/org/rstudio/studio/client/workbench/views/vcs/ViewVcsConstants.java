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
    String addCapitalized();
    String remoteNameColon();
    String remoteUrlColon();
    String switchBranch();
    String searchByBranchName();
    String remoteBranchCaption(String caption);
    String noBranchParentheses();
    String noBranchesAvailableParentheses();
    String localBranchesParentheses();
    String createCapitalized();
    String remoteColon();
    String addRemoteEllipses();
    String addRemote();
    String syncBranchWithRemote();
    String branchNameColon();
    String newBranchCapitalized();
    String localBranchAlreadyExists(String branchName);
    String checkoutCapitalized();
    String overwriteCapitalized();
    String cancelCapitalized();
    String localBranchAlreadyExistsCaption();
    String remoteBranchNameAlreadyExists(String remoteBranch, String remote);
    String remoteBranchAlreadyExistsCaption();
    String allBranchesParentheses();
    String closeCapitalized();
    String stopCapitalized();
    String progressDetails();
    String commitsPager(String wordText);
    String selectionSuffix(String emptyString);
    String lineSuffix(String emptyString);
    String chunkSuffix(String emptyString);
    String noCommitSelectedParentheses();
    String allCommitsParentheses();
    String filterByFileEllipses();
    String chooseFileCapitalized();
    String filterByDirectoryEllipses();
    String chooseFolderCapitalized();
    String filterColonPath(String path);
    String filterColonNone();
    String viewFileAtString(String substr);
    String errorFetchingHistory();
    String historyCapitalized();
    String changesCapitalized();
    String searchVersionControlHistory();
    String searchCapitalized();
    String readingFileEllipses();
    String showHistoryCapitalized();
    String showDiffCapitalized();
    String commitDepthAltText(int nexusColumn);
    String branchAheadOfRemoteSingular(String remoteName, int commitBehind);
    String branchAheadOfRemotePlural(String remoteName, int commitBehind);
    String statusCapitalized();
    String pathCapitalized();
    String subjectCapitalized();
    String authorCapitalized();
    String dateCapitalized();
    String stagedCapitalized();
    String gitTabCapitalized();
    String pullOptions();
    String moreCapitalized();
    String refreshNowCapitalized();
    String refreshOptions();
    String pullCapitalized();
    String pushCapitalized();
    String noChangesToFile();
    String noChangesToFileToDiff(String filename);
    String noChangesToRevert();
    String noChangesToFileToRevert(String filename);
    String revertChangesCapitalized();
    String changesToFileWillBeLost();
    String changesToFileWillBeLostPlural();
    String errorCapitalized();
    String unableToViewPathOnGithub(String path);
    String gitIgnoreCapitalized();
    String diffCapitalized();
    String commitCapitalized();
    String gitReviewCapitalized();
    String gitDiffCapitalized();
    String stageCapitalized();
    String revertCapitalized();
    String ignoreCapitalized();
    String stageAllCapitalized();
    String discardAllCapitalized();
    String unstageAllCapitalized();
    String revertEllipses();
    String ignoreEllipses();
    String openFileCapitalized();
    String lengthCharacters(int length);
    String lengthCharactersInMessage(int length);
    String unstageCapitalized();
    String discardCapitalized();
    String allUnstagedChangesWillBeLost();
    String theSelectedChangesWillBeLost();
    String someFilesAreQuiteLarge(String prettySize);
    String committingLargeFiles();
    String diffError();
    String changeList();
    String svnIgnore();
    String svnColonIgnore();
    String svnAdd();
    String svnDelete();
    String resolveCapitalized();
    String svnResolve();
    String noneOfSelectedPathsHaveConflicts();
    String selectedPathDoesNotAppearToHaveConflicts();
    String noConflictsDetected();
    String changesToSelectedFileWillBeReverted();
    String changesToSelectedFileWillBeRevertedPlural();
    String svnRevert();
    String svnTab();
    String svnCleanup();
    String cleaningUpWorkingDirectoryEllipses();
    String noChangesToFileTODiff(String fileName);
    String path();
    String paths();
    String fileConflictCapitalized();
    String fileConflictMarkAsResolved();
    String svnCommit();
    String noItemsSelectedCapitalized();
    String selectOneOrMoreItemsToCommit();
    String messageRequiredCapitalized();
    String provideACommitMessage();
    String revisionCapitalized();
    String svnReview();
    String svnDiff();
    String refreshCapitalized();
    String updateCapitalized();
    String allChangesInFileWillBeLost();
    String selectedChangesInFileWillBeLost();
    String commit();
    String diff();
    String addedCapitalized();
    String modifiedCapitalized();
    String deletedCapitalized();
    String renamedCapitalized();
    String copiedCapitalized();
    String untrackedCapitalized();
    String unmergedCapitalized();
    String conflictedCapitalized();
    String externalCapitalized();
    String ignoredCapitalized();
    String missingCapitalized();
    String obstructedCapitalized();
    String unversionedCapitalized();
}
