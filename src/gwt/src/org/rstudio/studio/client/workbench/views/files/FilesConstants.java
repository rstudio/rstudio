/*
 * FilesConstants.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.files;

public interface FilesConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "New Folder".
     *
     * @return translated "New Folder"
     */
    @DefaultMessage("New Folder")
    @Key("newFolderTitle")
    String newFolderTitle();

    /**
     * Translated "Please enter the new folder name".
     *
     * @return translated "Please enter the new folder name"
     */
    @DefaultMessage("Please enter the new folder name")
    @Key("newFolderNameLabel")
    String newFolderNameLabel();

    /**
     * Translated "Creating folder...".
     *
     * @return translated "Creating folder..."
     */
    @DefaultMessage("Creating folder...")
    @Key("creatingFolderProgressLabel")
    String creatingFolderProgressLabel();

    /**
     * Translated "Multiple Items Selected".
     *
     * @return translated "Multiple Items Selected"
     */
    @DefaultMessage("Multiple Items Selected")
    @Key("multipleItemsSelectedCaption")
    String multipleItemsSelectedCaption();

    /**
     * Translated "Please select a single file or folder to copy".
     *
     * @return translated "Please select a single file or folder to copy"
     */
    @DefaultMessage("Please select a single file or folder to copy")
    @Key("multipleItemsSelectedMessage")
    String multipleItemsSelectedMessage();

    /**
     * Translated "Choose Destination".
     *
     * @return translated "Choose Destination"
     */
    @DefaultMessage("Choose Destination")
    @Key("chooseDestinationTitle")
    String chooseDestinationTitle();

    /**
     * Translated "Choose Folder".
     *
     * @return translated "Choose Folder"
     */
    @DefaultMessage("Choose Folder")
    @Key("chooseFolderTitle")
    String chooseFolderTitle();

    /**
     * Translated "Invalid target folder".
     *
     * @return translated "Invalid target folder"
     */
    @DefaultMessage("Invalid target folder")
    @Key("invalidTargetFolderErrorMessage")
    String invalidTargetFolderErrorMessage();

    /**
     * Translated "Moving files...".
     *
     * @return translated "Moving files..."
     */
    @DefaultMessage("Moving files...")
    @Key("movingFilesLabel")
    String movingFilesLabel();

    /**
     * Translated "Export Files".
     *
     * @return translated "Export Files"
     */
    @DefaultMessage("Export Files")
    @Key("exportFilesCaption")
    String exportFilesCaption();

    /**
     * Translated "selected file(s)".
     *
     * @return translated "selected file(s)"
     */
    @DefaultMessage("selected file(s)")
    @Key("selectedFilesCaption")
    String selectedFilesCaption();

    /**
     * Translated "Invalid Selection".
     *
     * @return translated "Invalid Selection"
     */
    @DefaultMessage("Invalid Selection")
    @Key("invalidSelectionCaption")
    String invalidSelectionCaption();

    /**
     * Translated "Please select a single file to rename.".
     *
     * @return translated "Please select a single file to rename."
     */
    @DefaultMessage("Please select a single file to rename.")
    @Key("invalidSelectionMessage")
    String invalidSelectionMessage();

    /**
     * Translated "Are you sure you want to permanently delete ".
     *
     * @return translated "Are you sure you want to permanently delete "
     */
    @DefaultMessage("Are you sure you want to permanently delete ")
    @Key("permanentDeleteMessage")
    String permanentDeleteMessage();


    /**
     * Translated "the {0} selected files".
     *
     * @return translated "the {0} selected files"
     */
    @DefaultMessage("the {0} selected files")
    @Key("selectedFilesMessage")
    String selectedFilesMessage(int size);

    /**
     * Translated "?\n\nThis cannot be undone.".
     *
     * @return translated "?\n\nThis cannot be undone."
     */
    @DefaultMessage("?\\n\\nThis cannot be undone.")
    @Key("cannotBeUndoneMessage")
    String cannotBeUndoneMessage();

    /**
     * Translated "Confirm Delete".
     *
     * @return translated "Confirm Delete"
     */
    @DefaultMessage("Confirm Delete")
    @Key("confirmDeleteCaption")
    String confirmDeleteCaption();

    /**
     * Translated "Deleting files...".
     *
     * @return translated "Deleting files..."
     */
    @DefaultMessage("Deleting files...")
    @Key("deletingFilesLabel")
    String deletingFilesLabel();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    /**
     * Translated "The Public folder cannot be {0}.".
     *
     * @return translated "The Public folder cannot be {0}."
     */
    @DefaultMessage("The Public folder cannot be {0}.")
    @Key("publicFolderMessage")
    String publicFolderMessage(String verb);

    /**
     * Translated "Error Opening Files".
     *
     * @return translated "Error Opening Files"
     */
    @DefaultMessage("Error Opening Files")
    @Key("errorOpeningFilesCaption")
    String errorOpeningFilesCaption();

    /**
     * Translated "{0} RNotebook files were unable to be processed and opened.".
     *
     * @return translated "{0} RNotebook files were unable to be processed and opened."
     */
    @DefaultMessage("{0} RNotebook files were unable to be processed and opened.")
    @Key("fileErrorMessage")
    String fileErrorMessage(int size);

    /**
     * Translated "\n\nErrors:".
     *
     * @return translated "\n\nErrors:"
     */
    @DefaultMessage("\\n\\nErrors:")
    @Key("errorMessage")
    String errorMessage();

    /**
     * Translated ""{0}" failed to open\n{1}\n".
     *
     * @return translated ""{0}" failed to open\n{1}\n"
     */
    @DefaultMessage("\"{0}\" failed to open\\n{1}\\n")
    @Key("failedToOpenMessage")
    String failedToOpenMessage(String notebookName, String userMessage);

    /**
     * Translated "Create a New {0} File in Current Directory".
     *
     * @return translated "Create a New {0} File in Current Directory"
     */
    @DefaultMessage("Create a New {0} File in Current Directory")
    @Key("createNewFileTitle")
    String createNewFileTitle(String formmattedExt);

    /**
     * Translated "Please enter the new file name:".
     *
     * @return translated "Please enter the new file name:"
     */
    @DefaultMessage("Please enter the new file name:")
    @Key("enterFileNameLabel")
    String enterFileNameLabel();

    /**
     * Translated "Creating file...".
     *
     * @return translated "Creating file..."
     */
    @DefaultMessage("Creating file...")
    @Key("creatingFileLabel")
    String creatingFileLabel();

    /**
     * Translated "Blank File Creation Failed".
     *
     * @return translated "Blank File Creation Failed"
     */
    @DefaultMessage("Blank File Creation Failed")
    @Key("blankFileFailedCaption")
    String blankFileFailedCaption();

    /**
     * Translated "A blank {0} file named "{1}" was unable to be created.\n\nThe server failed with the following error: \n{2}".
     *
     * @return translated "A blank {0} file named "{1}" was unable to be created.\n\nThe server failed with the following error: \n{2}"
     */
    @DefaultMessage("A blank {0} file named \"{1}\" was unable to be created.\\n\\nThe server failed with the following error: \\n{2}")
    @Key("blankFileFailedMessage")
    String blankFileFailedMessage(String deafultExtension, String input, String userMessage);

    /**
     * Translated "Rename File".
     *
     * @return translated "Rename File"
     */
    @DefaultMessage("Rename File")
    @Key("renameFileTitle")
    String renameFileTitle();

    /**
     * Translated "Please enter the new file name:".
     *
     * @return translated "Please enter the new file name:"
     */
    @DefaultMessage("Please enter the new file name:")
    @Key("renameFileCaption")
    String renameFileCaption();

    /**
     * Translated "Renaming file...".
     *
     * @return translated "Renaming file..."
     */
    @DefaultMessage("Renaming file...")
    @Key("renamingFileProgressMessage")
    String renamingFileProgressMessage();

    /**
     * Translated "Folder".
     *
     * @return translated "Folder"
     */
    @DefaultMessage("Folder")
    @Key("folderLabel")
    String folderLabel();

    /**
     * Translated "File".
     *
     * @return translated "File"
     */
    @DefaultMessage("File")
    @Key("fileLabel")
    String fileLabel();

    /**
     * Translated "Copy {0}".
     *
     * @return translated "Copy {0}"
     */
    @DefaultMessage("Copy {0}")
    @Key("copyTitle")
    String copyTitle(String objectName);

    /**
     * Translated "Enter a name for the copy of ''{0}'':".
     *
     * @return translated "Enter a name for the copy of ''{0}'':"
     */
    @DefaultMessage("Enter a name for the copy of ''{0}'':")
    @Key("enterNameLabel")
    String enterNameLabel(String name);

    /**
     * Translated "Copying {0}...".
     *
     * @return translated "Copying {0}..."
     */
    @DefaultMessage("Copying {0}...")
    @Key("copyingLabel")
    String copyingLabel(String objectName);

    /**
     * Translated "Files".
     *
     * @return translated "Files"
     */
    @DefaultMessage("Files")
    @Key("filesTitle")
    String filesTitle();

    /**
     * Translated "File Listing Error".
     *
     * @return translated "File Listing Error"
     */
    @DefaultMessage("File Listing Error")
    @Key("fileListingErrorCaption")
    String fileListingErrorCaption();

    /**
     * Translated "Error navigating to {0}:\n\n{1}".
     *
     * @return translated "Error navigating to {0}:\n\n{1}"
     */
    @DefaultMessage("Error navigating to {0}:\\n\\n{1}")
    @Key("fileListingErrorMessage")
    String fileListingErrorMessage(String path, String userMessage);

    /**
     * Translated "Open in Editor".
     *
     * @return translated "Open in Editor"
     */
    @DefaultMessage("Open in Editor")
    @Key("openInEditorLabel")
    String openInEditorLabel();

    /**
     * Translated "View in Web Browser".
     *
     * @return translated "View in Web Browser"
     */
    @DefaultMessage("View in Web Browser")
    @Key("viewInWebBrowserLabel")
    String viewInWebBrowserLabel();

    /**
     * Translated "View File".
     *
     * @return translated "View File"
     */
    @DefaultMessage("View File")
    @Key("viewFileLabel")
    String viewFileLabel();

    /**
     * Translated "Import Dataset...".
     *
     * @return translated "Import Dataset..."
     */
    @DefaultMessage("Import Dataset...")
    @Key("importDatasetLabel")
    String importDatasetLabel();

    /**
     * Translated "unzip not found".
     *
     * @return translated "unzip not found"
     */
    @DefaultMessage("unzip not found")
    @Key("unzipNotFoundCaption")
    String unzipNotFoundCaption();

    /**
     * Translated "The unzip system utility could not be found. unzip is required for decompressing .zip archives after upload.\n\nWould you like to upload the zip archive without unzipping?".
     *
     * @return translated "The unzip system utility could not be found. unzip is required for decompressing .zip archives after upload.\n\nWould you like to upload the zip archive without unzipping?"
     */
    @DefaultMessage("The unzip system utility could not be found. unzip is required for decompressing .zip archives after upload.\\n\\nWould you like to upload the zip archive without unzipping?")
    @Key("unzipNotFoundMessage")
    String unzipNotFoundMessage();

    /**
     * Translated "The upload will overwrite ".
     *
     * @return translated "The upload will overwrite "
     */
    @DefaultMessage("The upload will overwrite ")
    @Key("uploadOverwriteMessage")
    String uploadOverwriteMessage();

    /**
     * Translated "multiple files including ".
     *
     * @return translated "multiple files including "
     */
    @DefaultMessage("multiple files including ")
    @Key("multipleFilesMessage")
    String multipleFilesMessage();

    /**
     * Translated "the file ".
     *
     * @return translated "the file "
     */
    @DefaultMessage("the file ")
    @Key("theFileMessage")
    String theFileMessage();

    /**
     * Translated "Are you sure you want to overwrite ".
     *
     * @return translated "Are you sure you want to overwrite "
     */
    @DefaultMessage("Are you sure you want to overwrite ")
    @Key("overwriteQuestion")
    String overwriteQuestion();

    /**
     * Translated "these files?".
     *
     * @return translated "these files?"
     */
    @DefaultMessage("these files?")
    @Key("filesLabel")
    String filesLabel();

    /**
     * Translated "this file?".
     *
     * @return translated "this file?"
     */
    @DefaultMessage("this file?")
    @Key("thisFileLabel")
    String thisFileLabel();

    /**
     * Translated "{0} file upload...".
     *
     * @return translated "{0} file upload..."
     */
    @DefaultMessage("{0} file upload...")
    @Key("fileUploadMessage")
    String fileUploadMessage(String commit);

    /**
     * Translated "Completing".
     *
     * @return translated "Completing"
     */
    @DefaultMessage("Completing")
    @Key("completingLabel")
    String completingLabel();

    /**
     * Translated "Cancelling".
     *
     * @return translated "Cancelling"
     */
    @DefaultMessage("Cancelling")
    @Key("cancellingLabel")
    String cancellingLabel();

    /**
     * Translated "File Upload Error".
     *
     * @return translated "File Upload Error"
     */
    @DefaultMessage("File Upload Error")
    @Key("fileUploadErrorMessage")
    String fileUploadErrorMessage();

    /**
     * Translated "Confirm Overwrite".
     *
     * @return translated "Confirm Overwrite"
     */
    @DefaultMessage("Confirm Overwrite")
    @Key("confirmOverwriteCaption")
    String confirmOverwriteCaption();

    /**
     * Translated "File Commands".
     *
     * @return translated "File Commands"
     */
    @DefaultMessage("File Commands")
    @Key("fileCommandsLabel")
    String fileCommandsLabel();

    /**
     * Translated "New Blank File".
     *
     * @return translated "New Blank File"
     */
    @DefaultMessage("New Blank File")
    @Key("newBlankFileText")
    String newBlankFileText();

    /**
     * Translated "Create a new blank file in current directory".
     *
     * @return translated "Create a new blank file in current directory"
     */
    @DefaultMessage("Create a new blank file in current directory")
    @Key("createNewBlankFileText")
    String createNewBlankFileText();

    /**
     * Translated "Synchronize Working Directory".
     *
     * @return translated "Synchronize Working Directory"
     */
    @DefaultMessage("Synchronize Working Directory")
    @Key("synchronizeWorkingDirectoryLabel")
    String synchronizeWorkingDirectoryLabel();

    /**
     * Translated "Show Hidden Files".
     *
     * @return translated "Show Hidden Files"
     */
    @DefaultMessage("Show Hidden Files")
    @Key("showHiddenFilesLabel")
    String showHiddenFilesLabel();

    /**
     * Translated "More".
     *
     * @return translated "More"
     */
    @DefaultMessage("More")
    @Key("moreText")
    String moreText();

    /**
     * Translated "More file commands".
     *
     * @return translated "More file commands"
     */
    @DefaultMessage("More file commands")
    @Key("moreFileCommandsLabel")
    String moreFileCommandsLabel();

    /**
     * Translated "Folder".
     *
     * @return translated "Folder"
     */
    @DefaultMessage("Folder")
    @Key("folderText")
    String folderText();

    /**
     * Translated "Blank File".
     *
     * @return translated "Blank File"
     */
    @DefaultMessage("Blank File")
    @Key("blankFileText")
    String blankFileText();

    /**
     * Translated "Upload".
     *
     * @return translated "Upload"
     */
    @DefaultMessage("Upload")
    @Key("uploadText")
    String uploadText();

    /**
     * Translated "Delete".
     *
     * @return translated "Delete"
     */
    @DefaultMessage("Delete")
    @Key("deleteText")
    String deleteText();

    /**
     * Translated "Rename".
     *
     * @return translated "Rename"
     */
    @DefaultMessage("Rename")
    @Key("renameText")
    String renameText();

    /**
     * Translated "New Folder".
     *
     * @return translated "New Folder"
     */
    @DefaultMessage("New Folder")
    @Key("newFolderText")
    String newFolderText();

    /**
     * Translated "Select all files".
     *
     * @return translated "Select all files"
     */
    @DefaultMessage("Select all files")
    @Key("selectAllFilesLabel")
    String selectAllFilesLabel();

    /**
     * Translated "Size".
     *
     * @return translated "Size"
     */
    @DefaultMessage("Size")
    @Key("sizeText")
    String sizeText();

    /**
     * Translated "Modified".
     *
     * @return translated "Modified"
     */
    @DefaultMessage("Modified")
    @Key("modifiedText")
    String modifiedText();

    /**
     * Translated "refresh not supported".
     *
     * @return translated "refresh not supported"
     */
    @DefaultMessage("refresh not supported")
    @Key("refreshNotSupportedException")
    String refreshNotSupportedException();

    /**
     * Translated "mkdir not supported".
     *
     * @return translated "mkdir not supported"
     */
    @DefaultMessage("mkdir not supported")
    @Key("mkdirNotSupportedException")
    String mkdirNotSupportedException();

    /**
     * Translated "getIcon not supported".
     *
     * @return translated "getIcon not supported"
     */
    @DefaultMessage("getIcon not supported")
    @Key("getIconNotSupportedException")
    String getIconNotSupportedException();

    /**
     * Translated "Unexpected response from server".
     *
     * @return translated "Unexpected response from server"
     */
    @DefaultMessage("Unexpected response from server")
    @Key("unexpectedResponseException")
    String unexpectedResponseException();

    /**
     * Translated "You must specify a file to upload.".
     *
     * @return translated "You must specify a file to upload."
     */
    @DefaultMessage("You must specify a file to upload.")
    @Key("specifyFileToUploadException")
    String specifyFileToUploadException();

    /**
     * Translated "Upload Files".
     *
     * @return translated "Upload Files"
     */
    @DefaultMessage("Upload Files")
    @Key("uploadFilesTitle")
    String uploadFilesTitle();

    /**
     * Translated "Uploading file...".
     *
     * @return translated "Uploading file..."
     */
    @DefaultMessage("Uploading file...")
    @Key("uploadingFileProgressMessage")
    String uploadingFileProgressMessage();

    /**
     * Translated "Target directory:".
     *
     * @return translated "Target directory:"
     */
    @DefaultMessage("Target directory:")
    @Key("targetDirectoryLabel")
    String targetDirectoryLabel();

    /**
     * Translated "File to upload:".
     *
     * @return translated "File to upload:"
     */
    @DefaultMessage("File to upload:")
    @Key("fileToUploadLabel")
    String fileToUploadLabel();

    /**
     * Translated "<b>TIP</b>: To upload multiple files or a directory, create a zip file. The zip file will be automatically expanded after upload.".
     *
     * @return translated "<b>TIP</b>: To upload multiple files or a directory, create a zip file. The zip file will be automatically expanded after upload."
     */
    @DefaultMessage("<b>TIP</b>: To upload multiple files or a directory, create a zip file. The zip file will be automatically expanded after upload.")
    @Key("tipHTML")
    String tipHTML();

    /**
     * Translated "fileText".
     *
     * @return translated "fileText"
     */
    @DefaultMessage("fileText")
    @Key("fileText")
    String fileText();

    /**
     * Translated "Name".
     *
     * @return translated "Name"
     */
    @DefaultMessage("Name")
    @Key("nameHeaderText")
    String nameHeaderText();

}
