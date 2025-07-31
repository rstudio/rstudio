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

    @Key("newFolderTitle")
    String newFolderTitle();

    @Key("newFolderNameLabel")
    String newFolderNameLabel();

    @Key("creatingFolderProgressLabel")
    String creatingFolderProgressLabel();

    @Key("multipleItemsSelectedCaption")
    String multipleItemsSelectedCaption();

    @Key("multipleItemsSelectedMessage")
    String multipleItemsSelectedMessage();

    @Key("chooseDestinationTitle")
    String chooseDestinationTitle();

    @Key("chooseFolderTitle")
    String chooseFolderTitle();

    @Key("invalidTargetFolderErrorMessage")
    String invalidTargetFolderErrorMessage();

    @Key("movingFilesLabel")
    String movingFilesLabel();

    @Key("exportFilesCaption")
    String exportFilesCaption();

    @Key("selectedFilesCaption")
    String selectedFilesCaption();

    @Key("invalidSelectionCaption")
    String invalidSelectionCaption();

    @Key("invalidSelectionMessage")
    String invalidSelectionMessage();

    @Key("permanentDeleteMessage")
    String permanentDeleteMessage();

    @Key("selectedFilesMessage")
    String selectedFilesMessage(int size);

    @Key("cannotBeUndoneMessage")
    String cannotBeUndoneMessage();

    @Key("confirmDeleteCaption")
    String confirmDeleteCaption();

    @Key("deletingFilesLabel")
    String deletingFilesLabel();

    @Key("errorCaption")
    String errorCaption();

    @Key("publicFolderMessage")
    String publicFolderMessage(String verb);

    @Key("errorOpeningFilesCaption")
    String errorOpeningFilesCaption();

    @Key("fileErrorMessage")
    String fileErrorMessage(int size);

    @Key("errorMessage")
    String errorMessage();

    @Key("failedToOpenMessage")
    String failedToOpenMessage(String notebookName, String userMessage);

    @Key("createNewFileTitle")
    String createNewFileTitle(String formmattedExt);

    @Key("createNewFileInCurrentDirectoryTitle")
    String createNewFileInCurrentDirectoryTitle();
    

    @Key("enterFileNameLabel")
    String enterFileNameLabel();

    @Key("creatingFileLabel")
    String creatingFileLabel();

    @Key("blankFileFailedCaption")
    String blankFileFailedCaption();

    @Key("blankFileFailedMessage")
    String blankFileFailedMessage(String deafultExtension, String input, String userMessage);

    @Key("renameFileTitle")
    String renameFileTitle();

    @Key("renameFileCaption")
    String renameFileCaption();

    @Key("renamingFileProgressMessage")
    String renamingFileProgressMessage();

    @Key("folderLabel")
    String folderLabel();

    @Key("fileLabel")
    String fileLabel();

    @Key("copyTitle")
    String copyTitle(String objectName);

    @Key("enterNameLabel")
    String enterNameLabel(String name);

    @Key("copyingLabel")
    String copyingLabel(String objectName);

    @Key("filesTitle")
    String filesTitle();

    @Key("fileListingErrorCaption")
    String fileListingErrorCaption();

    @Key("fileListingErrorMessage")
    String fileListingErrorMessage(String path, String userMessage);

    @Key("openInEditorLabel")
    String openInEditorLabel();

    @Key("viewInWebBrowserLabel")
    String viewInWebBrowserLabel();

    @Key("viewFileLabel")
    String viewFileLabel();

    @Key("importDatasetLabel")
    String importDatasetLabel();

    @Key("unzipNotFoundCaption")
    String unzipNotFoundCaption();

    @Key("unzipNotFoundMessage")
    String unzipNotFoundMessage();

    @Key("uploadOverwriteMessage")
    String uploadOverwriteMessage();

    @Key("multipleFilesMessage")
    String multipleFilesMessage();

    @Key("theFileMessage")
    String theFileMessage();

    @Key("overwriteQuestion")
    String overwriteQuestion();

    @Key("filesLabel")
    String filesLabel();

    @Key("thisFileLabel")
    String thisFileLabel();

    @Key("fileUploadMessage")
    String fileUploadMessage(String commit);

    @Key("completingLabel")
    String completingLabel();

    @Key("cancellingLabel")
    String cancellingLabel();

    @Key("fileUploadErrorMessage")
    String fileUploadErrorMessage();

    @Key("confirmOverwriteCaption")
    String confirmOverwriteCaption();

    @Key("fileCommandsLabel")
    String fileCommandsLabel();

    @Key("newFileText")
    String newFileText();
    

    @Key("newBlankFileText")
    String newBlankFileText();

    @Key("createNewFileText")
    String createNewFileText();
    

    @Key("createNewBlankFileText")
    String createNewBlankFileText();

    @Key("synchronizeWorkingDirectoryLabel")
    String synchronizeWorkingDirectoryLabel();

    @Key("showHiddenFilesLabel")
    String showHiddenFilesLabel();

    @Key("moreText")
    String moreText();

    @Key("moreFileCommandsLabel")
    String moreFileCommandsLabel();

    @Key("folderText")
    String folderText();

    @Key("blankFileText")
    String blankFileText();

    @Key("uploadText")
    String uploadText();

    @Key("deleteText")
    String deleteText();

    @Key("renameText")
    String renameText();

    @Key("newFolderText")
    String newFolderText();

    @Key("selectAllFilesLabel")
    String selectAllFilesLabel();

    @Key("sizeText")
    String sizeText();

    @Key("modifiedText")
    String modifiedText();

    @Key("refreshNotSupportedException")
    String refreshNotSupportedException();

    @Key("mkdirNotSupportedException")
    String mkdirNotSupportedException();

    @Key("getIconNotSupportedException")
    String getIconNotSupportedException();

    @Key("unexpectedResponseException")
    String unexpectedResponseException();

    @Key("specifyFileToUploadException")
    String specifyFileToUploadException();

    @Key("uploadFilesTitle")
    String uploadFilesTitle();

    @Key("uploadingFileProgressMessage")
    String uploadingFileProgressMessage();

    @Key("targetDirectoryLabel")
    String targetDirectoryLabel();

    @Key("fileToUploadLabel")
    String fileToUploadLabel();

    @Key("tipHTML")
    String tipHTML();

    @Key("fileText")
    String fileText();

    @Key("nameHeaderText")
    String nameHeaderText();

}
