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

    @DefaultMessage("New Folder")
    @Key("newFolderTitle")
    String newFolderTitle();

    @DefaultMessage("Please enter the new folder name")
    @Key("newFolderNameLabel")
    String newFolderNameLabel();

    @DefaultMessage("Creating folder...")
    @Key("creatingFolderProgressLabel")
    String creatingFolderProgressLabel();

    @DefaultMessage("Multiple Items Selected")
    @Key("multipleItemsSelectedCaption")
    String multipleItemsSelectedCaption();

    @DefaultMessage("Please select a single file or folder to copy")
    @Key("multipleItemsSelectedMessage")
    String multipleItemsSelectedMessage();

    @DefaultMessage("Choose Destination")
    @Key("chooseDestinationTitle")
    String chooseDestinationTitle();

    @DefaultMessage("Choose Folder")
    @Key("chooseFolderTitle")
    String chooseFolderTitle();

    @DefaultMessage("Invalid target folder")
    @Key("invalidTargetFolderErrorMessage")
    String invalidTargetFolderErrorMessage();

    @DefaultMessage("Moving files...")
    @Key("movingFilesLabel")
    String movingFilesLabel();

    @DefaultMessage("Export Files")
    @Key("exportFilesCaption")
    String exportFilesCaption();

    @DefaultMessage("selected file(s)")
    @Key("selectedFilesCaption")
    String selectedFilesCaption();

    @DefaultMessage("Invalid Selection")
    @Key("invalidSelectionCaption")
    String invalidSelectionCaption();

    @DefaultMessage("Please select a single file to rename.")
    @Key("invalidSelectionMessage")
    String invalidSelectionMessage();

    @DefaultMessage("Are you sure you want to permanently delete ")
    @Key("permanentDeleteMessage")
    String permanentDeleteMessage();

    @DefaultMessage("the {0} selected files")
    @Key("selectedFilesMessage")
    String selectedFilesMessage(int size);

    @DefaultMessage("?\\n\\nThis cannot be undone.")
    @Key("cannotBeUndoneMessage")
    String cannotBeUndoneMessage();

    @DefaultMessage("Confirm Delete")
    @Key("confirmDeleteCaption")
    String confirmDeleteCaption();

    @DefaultMessage("Deleting files...")
    @Key("deletingFilesLabel")
    String deletingFilesLabel();

    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    @DefaultMessage("The Public folder cannot be {0}.")
    @Key("publicFolderMessage")
    String publicFolderMessage(String verb);

    @DefaultMessage("Error Opening Files")
    @Key("errorOpeningFilesCaption")
    String errorOpeningFilesCaption();

    @DefaultMessage("{0} RNotebook files were unable to be processed and opened.")
    @Key("fileErrorMessage")
    String fileErrorMessage(int size);

    @DefaultMessage("\\n\\nErrors:")
    @Key("errorMessage")
    String errorMessage();

    @DefaultMessage("\"{0}\" failed to open\\n{1}\\n")
    @Key("failedToOpenMessage")
    String failedToOpenMessage(String notebookName, String userMessage);

    @DefaultMessage("Create a New {0} File in Current Directory")
    @Key("createNewFileTitle")
    String createNewFileTitle(String formmattedExt);

    @DefaultMessage("Create a New File in Current Directory")
    @Key("createNewFileInCurrentDirectoryTitle")
    String createNewFileInCurrentDirectoryTitle();
    

    @DefaultMessage("Please enter the new file name:")
    @Key("enterFileNameLabel")
    String enterFileNameLabel();

    @DefaultMessage("Creating file...")
    @Key("creatingFileLabel")
    String creatingFileLabel();

    @DefaultMessage("Blank File Creation Failed")
    @Key("blankFileFailedCaption")
    String blankFileFailedCaption();

    @DefaultMessage("A blank {0} file named \"{1}\" was unable to be created.\\n\\nThe server failed with the following error: \\n{2}")
    @Key("blankFileFailedMessage")
    String blankFileFailedMessage(String deafultExtension, String input, String userMessage);

    @DefaultMessage("Rename File")
    @Key("renameFileTitle")
    String renameFileTitle();

    @DefaultMessage("Please enter the new file name:")
    @Key("renameFileCaption")
    String renameFileCaption();

    @DefaultMessage("Renaming file...")
    @Key("renamingFileProgressMessage")
    String renamingFileProgressMessage();

    @DefaultMessage("Folder")
    @Key("folderLabel")
    String folderLabel();

    @DefaultMessage("File")
    @Key("fileLabel")
    String fileLabel();

    @DefaultMessage("Copy {0}")
    @Key("copyTitle")
    String copyTitle(String objectName);

    @DefaultMessage("Enter a name for the copy of ''{0}'':")
    @Key("enterNameLabel")
    String enterNameLabel(String name);

    @DefaultMessage("Copying {0}...")
    @Key("copyingLabel")
    String copyingLabel(String objectName);

    @DefaultMessage("Files")
    @Key("filesTitle")
    String filesTitle();

    @DefaultMessage("File Listing Error")
    @Key("fileListingErrorCaption")
    String fileListingErrorCaption();

    @DefaultMessage("Error navigating to {0}:\\n\\n{1}")
    @Key("fileListingErrorMessage")
    String fileListingErrorMessage(String path, String userMessage);

    @DefaultMessage("Open in Editor")
    @Key("openInEditorLabel")
    String openInEditorLabel();

    @DefaultMessage("View in Web Browser")
    @Key("viewInWebBrowserLabel")
    String viewInWebBrowserLabel();

    @DefaultMessage("View File")
    @Key("viewFileLabel")
    String viewFileLabel();

    @DefaultMessage("Import Dataset...")
    @Key("importDatasetLabel")
    String importDatasetLabel();

    @DefaultMessage("unzip not found")
    @Key("unzipNotFoundCaption")
    String unzipNotFoundCaption();

    @DefaultMessage("The unzip system utility could not be found. unzip is required for decompressing .zip archives after upload.\\n\\nWould you like to upload the zip archive without unzipping?")
    @Key("unzipNotFoundMessage")
    String unzipNotFoundMessage();

    @DefaultMessage("The upload will overwrite ")
    @Key("uploadOverwriteMessage")
    String uploadOverwriteMessage();

    @DefaultMessage("multiple files including ")
    @Key("multipleFilesMessage")
    String multipleFilesMessage();

    @DefaultMessage("the file ")
    @Key("theFileMessage")
    String theFileMessage();

    @DefaultMessage("Are you sure you want to overwrite ")
    @Key("overwriteQuestion")
    String overwriteQuestion();

    @DefaultMessage("these files?")
    @Key("filesLabel")
    String filesLabel();

    @DefaultMessage("this file?")
    @Key("thisFileLabel")
    String thisFileLabel();

    @DefaultMessage("{0} file upload...")
    @Key("fileUploadMessage")
    String fileUploadMessage(String commit);

    @DefaultMessage("Completing")
    @Key("completingLabel")
    String completingLabel();

    @DefaultMessage("Cancelling")
    @Key("cancellingLabel")
    String cancellingLabel();

    @DefaultMessage("File Upload Error")
    @Key("fileUploadErrorMessage")
    String fileUploadErrorMessage();

    @DefaultMessage("Confirm Overwrite")
    @Key("confirmOverwriteCaption")
    String confirmOverwriteCaption();

    @DefaultMessage("File Commands")
    @Key("fileCommandsLabel")
    String fileCommandsLabel();

    @DefaultMessage("New File")
    @Key("newFileText")
    String newFileText();
    

    @DefaultMessage("New Blank File")
    @Key("newBlankFileText")
    String newBlankFileText();

    @DefaultMessage("Create a new file in the current directory")
    @Key("createNewFileText")
    String createNewFileText();
    

    @DefaultMessage("Create a new blank file in current directory")
    @Key("createNewBlankFileText")
    String createNewBlankFileText();

    @DefaultMessage("Synchronize Working Directory")
    @Key("synchronizeWorkingDirectoryLabel")
    String synchronizeWorkingDirectoryLabel();

    @DefaultMessage("Show Hidden Files")
    @Key("showHiddenFilesLabel")
    String showHiddenFilesLabel();

    @DefaultMessage("More")
    @Key("moreText")
    String moreText();

    @DefaultMessage("More file commands")
    @Key("moreFileCommandsLabel")
    String moreFileCommandsLabel();

    @DefaultMessage("Folder")
    @Key("folderText")
    String folderText();

    @DefaultMessage("Blank File")
    @Key("blankFileText")
    String blankFileText();

    @DefaultMessage("Upload")
    @Key("uploadText")
    String uploadText();

    @DefaultMessage("Delete")
    @Key("deleteText")
    String deleteText();

    @DefaultMessage("Rename")
    @Key("renameText")
    String renameText();

    @DefaultMessage("New Folder")
    @Key("newFolderText")
    String newFolderText();

    @DefaultMessage("Select all files")
    @Key("selectAllFilesLabel")
    String selectAllFilesLabel();

    @DefaultMessage("Size")
    @Key("sizeText")
    String sizeText();

    @DefaultMessage("Modified")
    @Key("modifiedText")
    String modifiedText();

    @DefaultMessage("refresh not supported")
    @Key("refreshNotSupportedException")
    String refreshNotSupportedException();

    @DefaultMessage("mkdir not supported")
    @Key("mkdirNotSupportedException")
    String mkdirNotSupportedException();

    @DefaultMessage("getIcon not supported")
    @Key("getIconNotSupportedException")
    String getIconNotSupportedException();

    @DefaultMessage("Unexpected response from server")
    @Key("unexpectedResponseException")
    String unexpectedResponseException();

    @DefaultMessage("You must specify a file to upload.")
    @Key("specifyFileToUploadException")
    String specifyFileToUploadException();

    @DefaultMessage("Upload Files")
    @Key("uploadFilesTitle")
    String uploadFilesTitle();

    @DefaultMessage("Uploading file...")
    @Key("uploadingFileProgressMessage")
    String uploadingFileProgressMessage();

    @DefaultMessage("Target directory:")
    @Key("targetDirectoryLabel")
    String targetDirectoryLabel();

    @DefaultMessage("File to upload:")
    @Key("fileToUploadLabel")
    String fileToUploadLabel();

    @DefaultMessage("<b>TIP</b>: To upload multiple files or a directory, create a zip file. The zip file will be automatically expanded after upload.")
    @Key("tipHTML")
    String tipHTML();

    @DefaultMessage("File")
    @Key("fileText")
    String fileText();

    @DefaultMessage("Name")
    @Key("nameHeaderText")
    String nameHeaderText();

}
