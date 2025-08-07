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

    /**
     * Translated "New Folder".
     *
     * @return translated "New Folder"
     */
    @DefaultMessage("New Folder")
    String newFolderTitle();

    /**
     * Translated "Please enter the new folder name".
     *
     * @return translated "Please enter the new folder name"
     */
    @DefaultMessage("Please enter the new folder name")
    String newFolderNameLabel();

    /**
     * Translated "Creating folder...".
     *
     * @return translated "Creating folder..."
     */
    @DefaultMessage("Creating folder...")
    String creatingFolderProgressLabel();

    /**
     * Translated "Multiple Items Selected".
     *
     * @return translated "Multiple Items Selected"
     */
    @DefaultMessage("Multiple Items Selected")
    String multipleItemsSelectedCaption();

    /**
     * Translated "Please select a single file or folder to copy".
     *
     * @return translated "Please select a single file or folder to copy"
     */
    @DefaultMessage("Please select a single file or folder to copy")
    String multipleItemsSelectedMessage();

    /**
     * Translated "Choose Destination".
     *
     * @return translated "Choose Destination"
     */
    @DefaultMessage("Choose Destination")
    String chooseDestinationTitle();

    /**
     * Translated "Choose Folder".
     *
     * @return translated "Choose Folder"
     */
    @DefaultMessage("Choose Folder")
    String chooseFolderTitle();

    /**
     * Translated "Invalid target folder".
     *
     * @return translated "Invalid target folder"
     */
    @DefaultMessage("Invalid target folder")
    String invalidTargetFolderErrorMessage();

    /**
     * Translated "Moving files...".
     *
     * @return translated "Moving files..."
     */
    @DefaultMessage("Moving files...")
    String movingFilesLabel();

    /**
     * Translated "Export Files".
     *
     * @return translated "Export Files"
     */
    @DefaultMessage("Export Files")
    String exportFilesCaption();

    /**
     * Translated "selected file(s)".
     *
     * @return translated "selected file(s)"
     */
    @DefaultMessage("selected file(s)")
    String selectedFilesCaption();

    /**
     * Translated "Invalid Selection".
     *
     * @return translated "Invalid Selection"
     */
    @DefaultMessage("Invalid Selection")
    String invalidSelectionCaption();

    /**
     * Translated "Please select a single file to rename.".
     *
     * @return translated "Please select a single file to rename."
     */
    @DefaultMessage("Please select a single file to rename.")
    String invalidSelectionMessage();

    /**
     * Translated "Are you sure you want to permanently delete ".
     *
     * @return translated "Are you sure you want to permanently delete "
     */
    @DefaultMessage("Are you sure you want to permanently delete ")
    String permanentDeleteMessage();


    /**
     * Translated "the {0} selected files".
     *
     * @return translated "the {0} selected files"
     */
    @DefaultMessage("the {0} selected files")
    String selectedFilesMessage(int size);

    /**
     * Translated "?\n\nThis cannot be undone.".
     *
     * @return translated "?\n\nThis cannot be undone."
     */
    @DefaultMessage("?\\n\\nThis cannot be undone.")
    String cannotBeUndoneMessage();

    /**
     * Translated "Confirm Delete".
     *
     * @return translated "Confirm Delete"
     */
    @DefaultMessage("Confirm Delete")
    String confirmDeleteCaption();

    /**
     * Translated "Deleting files...".
     *
     * @return translated "Deleting files..."
     */
    @DefaultMessage("Deleting files...")
    String deletingFilesLabel();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    String errorCaption();

    /**
     * Translated "The Public folder cannot be {0}.".
     *
     * @return translated "The Public folder cannot be {0}."
     */
    @DefaultMessage("The Public folder cannot be {0}.")
    String publicFolderMessage(String verb);

    /**
     * Translated "Error Opening Files".
     *
     * @return translated "Error Opening Files"
     */
    @DefaultMessage("Error Opening Files")
    String errorOpeningFilesCaption();

    /**
     * Translated "{0} RNotebook files were unable to be processed and opened.".
     *
     * @return translated "{0} RNotebook files were unable to be processed and opened."
     */
    @DefaultMessage("{0} RNotebook files were unable to be processed and opened.")
    String fileErrorMessage(int size);

    /**
     * Translated "\n\nErrors:".
     *
     * @return translated "\n\nErrors:"
     */
    @DefaultMessage("\\n\\nErrors:")
    String errorMessage();

    /**
     * Translated ""{0}" failed to open\n{1}\n".
     *
     * @return translated ""{0}" failed to open\n{1}\n"
     */
    @DefaultMessage("\"{0}\" failed to open\\n{1}\\n")
    String failedToOpenMessage(String notebookName, String userMessage);

    /**
     * Translated "Create a New {0} File in Current Directory".
     *
     * @return translated "Create a New {0} File in Current Directory"
     */
    @DefaultMessage("Create a New {0} File in Current Directory")
    String createNewFileTitle(String formmattedExt);

    /**
     * Translated "Create a New File in Current Directory".
     *
     * @return translated "Create a New File in Current Directory"
     */
    @DefaultMessage("Create a New File in Current Directory")
    String createNewFileInCurrentDirectoryTitle();
    
    /**
     * Translated "Please enter the new file name:".
     *
     * @return translated "Please enter the new file name:"
     */
    @DefaultMessage("Please enter the new file name:")
    String enterFileNameLabel();

    /**
     * Translated "Creating file...".
     *
     * @return translated "Creating file..."
     */
    @DefaultMessage("Creating file...")
    String creatingFileLabel();

    /**
     * Translated "Blank File Creation Failed".
     *
     * @return translated "Blank File Creation Failed"
     */
    @DefaultMessage("Blank File Creation Failed")
    String blankFileFailedCaption();

    /**
     * Translated "A blank {0} file named "{1}" was unable to be created.\n\nThe server failed with the following error: \n{2}".
     *
     * @return translated "A blank {0} file named "{1}" was unable to be created.\n\nThe server failed with the following error: \n{2}"
     */
    @DefaultMessage("A blank {0} file named \"{1}\" was unable to be created.\\n\\nThe server failed with the following error: \\n{2}")
    String blankFileFailedMessage(String deafultExtension, String input, String userMessage);

    /**
     * Translated "Rename File".
     *
     * @return translated "Rename File"
     */
    @DefaultMessage("Rename File")
    String renameFileTitle();

    /**
     * Translated "Please enter the new file name:".
     *
     * @return translated "Please enter the new file name:"
     */
    @DefaultMessage("Please enter the new file name:")
    String renameFileCaption();

    /**
     * Translated "Renaming file...".
     *
     * @return translated "Renaming file..."
     */
    @DefaultMessage("Renaming file...")
    String renamingFileProgressMessage();

    /**
     * Translated "Folder".
     *
     * @return translated "Folder"
     */
    @DefaultMessage("Folder")
    String folderLabel();

    /**
     * Translated "File".
     *
     * @return translated "File"
     */
    @DefaultMessage("File")
    String fileLabel();

    /**
     * Translated "Copy {0}".
     *
     * @return translated "Copy {0}"
     */
    @DefaultMessage("Copy {0}")
    String copyTitle(String objectName);

    /**
     * Translated "Enter a name for the copy of ''{0}'':".
     *
     * @return translated "Enter a name for the copy of ''{0}'':"
     */
    @DefaultMessage("Enter a name for the copy of ''{0}'':")
    String enterNameLabel(String name);

    /**
     * Translated "Copying {0}...".
     *
     * @return translated "Copying {0}..."
     */
    @DefaultMessage("Copying {0}...")
    String copyingLabel(String objectName);

    /**
     * Translated "Files".
     *
     * @return translated "Files"
     */
    @DefaultMessage("Files")
    String filesTitle();

    /**
     * Translated "File Listing Error".
     *
     * @return translated "File Listing Error"
     */
    @DefaultMessage("File Listing Error")
    String fileListingErrorCaption();

    /**
     * Translated "Error navigating to {0}:\n\n{1}".
     *
     * @return translated "Error navigating to {0}:\n\n{1}"
     */
    @DefaultMessage("Error navigating to {0}:\\n\\n{1}")
    String fileListingErrorMessage(String path, String userMessage);

    /**
     * Translated "Open in Editor".
     *
     * @return translated "Open in Editor"
     */
    @DefaultMessage("Open in Editor")
    String openInEditorLabel();

    /**
     * Translated "View in Web Browser".
     *
     * @return translated "View in Web Browser"
     */
    @DefaultMessage("View in Web Browser")
    String viewInWebBrowserLabel();

    /**
     * Translated "View File".
     *
     * @return translated "View File"
     */
    @DefaultMessage("View File")
    String viewFileLabel();

    /**
     * Translated "Import Dataset...".
     *
     * @return translated "Import Dataset..."
     */
    @DefaultMessage("Import Dataset...")
    String importDatasetLabel();

    /**
     * Translated "unzip not found".
     *
     * @return translated "unzip not found"
     */
    @DefaultMessage("unzip not found")
    String unzipNotFoundCaption();

    /**
     * Translated "The unzip system utility could not be found. unzip is required for decompressing .zip archives after upload.\n\nWould you like to upload the zip archive without unzipping?".
     *
     * @return translated "The unzip system utility could not be found. unzip is required for decompressing .zip archives after upload.\n\nWould you like to upload the zip archive without unzipping?"
     */
    @DefaultMessage("The unzip system utility could not be found. unzip is required for decompressing .zip archives after upload.\\n\\nWould you like to upload the zip archive without unzipping?")
    String unzipNotFoundMessage();

    /**
     * Translated "The upload will overwrite ".
     *
     * @return translated "The upload will overwrite "
     */
    @DefaultMessage("The upload will overwrite ")
    String uploadOverwriteMessage();

    /**
     * Translated "multiple files including ".
     *
     * @return translated "multiple files including "
     */
    @DefaultMessage("multiple files including ")
    String multipleFilesMessage();

    /**
     * Translated "the file ".
     *
     * @return translated "the file "
     */
    @DefaultMessage("the file ")
    String theFileMessage();

    /**
     * Translated "Are you sure you want to overwrite ".
     *
     * @return translated "Are you sure you want to overwrite "
     */
    @DefaultMessage("Are you sure you want to overwrite ")
    String overwriteQuestion();

    /**
     * Translated "these files?".
     *
     * @return translated "these files?"
     */
    @DefaultMessage("these files?")
    String filesLabel();

    /**
     * Translated "this file?".
     *
     * @return translated "this file?"
     */
    @DefaultMessage("this file?")
    String thisFileLabel();

    /**
     * Translated "{0} file upload...".
     *
     * @return translated "{0} file upload..."
     */
    @DefaultMessage("{0} file upload...")
    String fileUploadMessage(String commit);

    /**
     * Translated "Completing".
     *
     * @return translated "Completing"
     */
    @DefaultMessage("Completing")
    String completingLabel();

    /**
     * Translated "Cancelling".
     *
     * @return translated "Cancelling"
     */
    @DefaultMessage("Cancelling")
    String cancellingLabel();

    /**
     * Translated "File Upload Error".
     *
     * @return translated "File Upload Error"
     */
    @DefaultMessage("File Upload Error")
    String fileUploadErrorMessage();

    /**
     * Translated "Confirm Overwrite".
     *
     * @return translated "Confirm Overwrite"
     */
    @DefaultMessage("Confirm Overwrite")
    String confirmOverwriteCaption();

    /**
     * Translated "File Commands".
     *
     * @return translated "File Commands"
     */
    @DefaultMessage("File Commands")
    String fileCommandsLabel();

    /**
     * Translated "New File".
     *
     * @return translated "New File"
     */
    @DefaultMessage("New File")
    String newFileText();
    
    /**
     * Translated "New Blank File".
     *
     * @return translated "New Blank File"
     */
    @DefaultMessage("New Blank File")
    String newBlankFileText();

    /**
     * Translated "Create a new file in the current directory".
     *
     * @return translated "Create a new file in the current directory"
     */
    @DefaultMessage("Create a new file in the current directory")
    String createNewFileText();
    
    /**
     * Translated "Create a new blank file in current directory".
     *
     * @return translated "Create a new blank file in current directory"
     */
    @DefaultMessage("Create a new blank file in current directory")
    String createNewBlankFileText();

    /**
     * Translated "Synchronize Working Directory".
     *
     * @return translated "Synchronize Working Directory"
     */
    @DefaultMessage("Synchronize Working Directory")
    String synchronizeWorkingDirectoryLabel();

    /**
     * Translated "Show Hidden Files".
     *
     * @return translated "Show Hidden Files"
     */
    @DefaultMessage("Show Hidden Files")
    String showHiddenFilesLabel();

    /**
     * Translated "More".
     *
     * @return translated "More"
     */
    @DefaultMessage("More")
    String moreText();

    /**
     * Translated "More file commands".
     *
     * @return translated "More file commands"
     */
    @DefaultMessage("More file commands")
    String moreFileCommandsLabel();

    /**
     * Translated "Folder".
     *
     * @return translated "Folder"
     */
    @DefaultMessage("Folder")
    String folderText();

    /**
     * Translated "Blank File".
     *
     * @return translated "Blank File"
     */
    @DefaultMessage("Blank File")
    String blankFileText();

    /**
     * Translated "Upload".
     *
     * @return translated "Upload"
     */
    @DefaultMessage("Upload")
    String uploadText();

    /**
     * Translated "Delete".
     *
     * @return translated "Delete"
     */
    @DefaultMessage("Delete")
    String deleteText();

    /**
     * Translated "Rename".
     *
     * @return translated "Rename"
     */
    @DefaultMessage("Rename")
    String renameText();

    /**
     * Translated "New Folder".
     *
     * @return translated "New Folder"
     */
    @DefaultMessage("New Folder")
    String newFolderText();

    /**
     * Translated "Select all files".
     *
     * @return translated "Select all files"
     */
    @DefaultMessage("Select all files")
    String selectAllFilesLabel();

    /**
     * Translated "Size".
     *
     * @return translated "Size"
     */
    @DefaultMessage("Size")
    String sizeText();

    /**
     * Translated "Modified".
     *
     * @return translated "Modified"
     */
    @DefaultMessage("Modified")
    String modifiedText();

    /**
     * Translated "refresh not supported".
     *
     * @return translated "refresh not supported"
     */
    @DefaultMessage("refresh not supported")
    String refreshNotSupportedException();

    /**
     * Translated "mkdir not supported".
     *
     * @return translated "mkdir not supported"
     */
    @DefaultMessage("mkdir not supported")
    String mkdirNotSupportedException();

    /**
     * Translated "getIcon not supported".
     *
     * @return translated "getIcon not supported"
     */
    @DefaultMessage("getIcon not supported")
    String getIconNotSupportedException();

    /**
     * Translated "Unexpected response from server".
     *
     * @return translated "Unexpected response from server"
     */
    @DefaultMessage("Unexpected response from server")
    String unexpectedResponseException();

    /**
     * Translated "You must specify a file to upload.".
     *
     * @return translated "You must specify a file to upload."
     */
    @DefaultMessage("You must specify a file to upload.")
    String specifyFileToUploadException();

    /**
     * Translated "Upload Files".
     *
     * @return translated "Upload Files"
     */
    @DefaultMessage("Upload Files")
    String uploadFilesTitle();

    /**
     * Translated "Uploading file...".
     *
     * @return translated "Uploading file..."
     */
    @DefaultMessage("Uploading file...")
    String uploadingFileProgressMessage();

    /**
     * Translated "Target directory:".
     *
     * @return translated "Target directory:"
     */
    @DefaultMessage("Target directory:")
    String targetDirectoryLabel();

    /**
     * Translated "File to upload:".
     *
     * @return translated "File to upload:"
     */
    @DefaultMessage("File to upload:")
    String fileToUploadLabel();

    /**
     * Translated "<b>TIP</b>: To upload multiple files or a directory, create a zip file. The zip file will be automatically expanded after upload.".
     *
     * @return translated "<b>TIP</b>: To upload multiple files or a directory, create a zip file. The zip file will be automatically expanded after upload."
     */
    @DefaultMessage("<b>TIP</b>: To upload multiple files or a directory, create a zip file. The zip file will be automatically expanded after upload.")
    String tipHTML();

    /**
     * Translated "File".
     *
     * @return translated "File"
     */
    @DefaultMessage("File")
    String fileText();

    /**
     * Translated "Name".
     *
     * @return translated "Name"
     */
    @DefaultMessage("Name")
    String nameHeaderText();

}
