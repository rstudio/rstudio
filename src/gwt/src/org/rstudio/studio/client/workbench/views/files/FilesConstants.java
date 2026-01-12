/*
 * FilesConstants.java
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
package org.rstudio.studio.client.workbench.views.files;
public interface FilesConstants extends com.google.gwt.i18n.client.Messages {
    String newFolderTitle();
    String newFolderNameLabel();
    String creatingFolderProgressLabel();
    String multipleItemsSelectedCaption();
    String multipleItemsSelectedMessage();
    String chooseDestinationTitle();
    String chooseFolderTitle();
    String invalidTargetFolderErrorMessage();
    String movingFilesLabel();
    String exportFilesCaption();
    String selectedFilesCaption();
    String invalidSelectionCaption();
    String invalidSelectionMessage();
    String permanentDeleteMessage();
    String selectedFilesMessage(int size);
    String cannotBeUndoneMessage();
    String confirmDeleteCaption();
    String deletingFilesLabel();
    String errorCaption();
    String publicFolderMessage(String verb);
    String errorOpeningFilesCaption();
    String fileErrorMessage(int size);
    String errorMessage();
    String failedToOpenMessage(String notebookName, String userMessage);
    String createNewFileTitle(String formmattedExt);
    String createNewFileInCurrentDirectoryTitle();
    String enterFileNameLabel();
    String creatingFileLabel();
    String blankFileFailedCaption();
    String blankFileFailedMessage(String deafultExtension, String input, String userMessage);
    String renameFileTitle();
    String renameFileCaption();
    String renamingFileProgressMessage();
    String folderLabel();
    String fileLabel();
    String copyTitle(String objectName);
    String enterNameLabel(String name);
    String copyingLabel(String objectName);
    String filesTitle();
    String fileListingErrorCaption();
    String fileListingErrorMessage(String path, String userMessage);
    String openInEditorLabel();
    String viewInWebBrowserLabel();
    String viewFileLabel();
    String importDatasetLabel();
    String unzipNotFoundCaption();
    String unzipNotFoundMessage();
    String uploadOverwriteMessage();
    String multipleFilesMessage();
    String theFileMessage();
    String overwriteQuestion();
    String filesLabel();
    String thisFileLabel();
    String fileUploadMessage(String commit);
    String completingLabel();
    String cancellingLabel();
    String fileUploadErrorMessage();
    String confirmOverwriteCaption();
    String fileCommandsLabel();
    String newFileText();
    String newBlankFileText();
    String createNewFileText();
    String createNewBlankFileText();
    String synchronizeWorkingDirectoryLabel();
    String showHiddenFilesLabel();
    String deleteToTrashLabel();
    String deleteToRecycleBinLabel();
    String moreText();
    String moreFileCommandsLabel();
    String folderText();
    String blankFileText();
    String uploadText();
    String deleteText();
    String renameText();
    String newFolderText();
    String selectAllFilesLabel();
    String sizeText();
    String modifiedText();
    String refreshNotSupportedException();
    String mkdirNotSupportedException();
    String getIconNotSupportedException();
    String unexpectedResponseException();
    String specifyFileToUploadException();
    String uploadFilesTitle();
    String uploadingFileProgressMessage();
    String targetDirectoryLabel();
    String fileToUploadLabel();
    String tipHTML();
    String fileText();
    String nameHeaderText();
}
