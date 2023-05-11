/*
 * CoreClientConstants.java
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
package org.rstudio.core.client;

public interface CoreClientConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    @DefaultMessage("Cancel")
    @Key("cancelLabel")
    String cancelLabel();

    /**
     * Translated "No".
     *
     * @return translated "No"
     */
    @DefaultMessage("No")
    @Key("noLabel")
    String noLabel();

    /**
     * Translated "Yes".
     *
     * @return translated "Yes"
     */
    @DefaultMessage("Yes")
    @Key("yesLabel")
    String yesLabel();

    /**
     * Translated "OK".
     *
     * @return translated "OK"
     */
    @DefaultMessage("OK")
    @Key("okayLabel")
    String okayLabel();

    /**
     * Translated "Not Yet Implemented".
     *
     * @return translated "Not Yet Implemented"
     */
    @DefaultMessage("Not Yet Implemented")
    @Key("notYetImplementedCaption")
    String notYetImplementedCaption();

    /**
     * Translated "This feature has not yet been implemented.".
     *
     * @return translated "This feature has not yet been implemented."
     */
    @DefaultMessage("This feature has not yet been implemented.")
    @Key("notYetImplementedMessage")
    String notYetImplementedMessage();

    /**
     * Translated "Popup Blocked".
     *
     * @return translated "Popup Blocked"
     */
    @DefaultMessage("Popup Blocked")
    @Key("popupBlockCaption")
    String popupBlockCaption();

    /**
     * Translated "Try Again".
     *
     * @return translated "Try Again"
     */
    @DefaultMessage("Try Again")
    @Key("popupBlockTryAgainLabel")
    String popupBlockTryAgainLabel();

    /**
     * Translated "{0} second".
     *
     * @return translated "{0} second"
     */
    @DefaultMessage("{0} second")
    @Key("secondLabel")
    String secondLabel(int second);

    /**
     * Translated "{0} seconds".
     *
     * @return translated "{0} seconds"
     */
    @DefaultMessage("{0} seconds")
    @Key("secondPluralLabel")
    String secondPluralLabel(int seconds);

    /**
     * Translated "{0} minute".
     *
     * @return translated "{0} minute"
     */
    @DefaultMessage("{0} minute")
    @Key("minuteLabel")
    String minuteLabel(int minute);

    /**
     * Translated "{0} minutes".
     *
     * @return translated "{0} minutes"
     */
    @DefaultMessage("{0} minutes")
    @Key("minutePluralLabel")
    String minutePluralLabel(int minutes);

    /**
     * Translated "{0} hour".
     *
     * @return translated "{0} hour"
     */
    @DefaultMessage("{0} hour")
    @Key("hourLabel")
    String hourLabel(int hour);

    /**
     * Translated "{0} hours".
     *
     * @return translated "{0} hours"
     */
    @DefaultMessage("{0} hours")
    @Key("hourPluralLabel")
    String hourPluralLabel(int hours);

    /**
     * Translated "Type shortcuts to see if they are bound to a command. Close this message bar when done.".
     *
     * @return translated "Type shortcuts to see if they are bound to a command. Close this message bar when done."
     */
    @DefaultMessage("Type shortcuts to see if they are bound to a command. Close this message bar when done.")
    @Key("reportShortCutMessage")
    String reportShortCutMessage();

    /**
     * Translated "Multi-gesture shortcut pending".
     *
     * @return translated "Multi-gesture shortcut pending"
     */
    @DefaultMessage("Multi-gesture shortcut pending")
    @Key("multiGestureMessage")
    String multiGestureMessage();

    /**
     * Translated "Shortcut not bound".
     *
     * @return translated "Shortcut not bound"
     */
    @DefaultMessage("Shortcut not bound")
    @Key("shortcutUnBoundMessage")
    String shortcutUnBoundMessage();

    /**
     * Translated "Name is empty".
     *
     * @return translated "Name is empty"
     */
    @DefaultMessage("Name is empty")
    @Key("nameEmptyMessage")
    String nameEmptyMessage();

    /**
     * Translated "Names should not start or end with spaces".
     *
     * @return translated "Names should not start or end with spaces"
     */
    @DefaultMessage("Names should not start or end with spaces")
    @Key("nameStartWithMessage")
    String nameStartWithMessage();

    /**
     * Translated "Illegal character: /".
     *
     * @return translated "Illegal character: /"
     */
    @DefaultMessage("Illegal character: /")
    @Key("nameIllegalCharacterMessage")
    String nameIllegalCharacterMessage();

    /**
     * Translated "Illegal name".
     *
     * @return translated "Illegal name"
     */
    @DefaultMessage("Illegal name")
    @Key("illegalNameMessage")
    String illegalNameMessage();

    /**
     * Translated "Folder".
     *
     * @return translated "Folder"
     */
    @DefaultMessage("Folder")
    @Key("fileNameLabel")
    String fileNameLabel();

    /**
     * Translated "File name".
     *
     * @return translated "File name"
     */
    @DefaultMessage("File name")
    @Key("getFilenameLabel")
    String getFilenameLabel();

    /**
     * Translated "File does not exist".
     *
     * @return translated "File does not exist"
     */
    @DefaultMessage("File does not exist")
    @Key("nonexistentFileMessage")
    String nonexistentFileMessage();

    /**
     * Translated "Open Project".
     *
     * @return translated "Open Project"
     */
    @DefaultMessage("Open Project")
    @Key("openProjectTitle")
    String openProjectTitle();

    /**
     * Translated "Open".
     *
     * @return translated "Open"
     */
    @DefaultMessage("Open")
    @Key("openButtonTitle")
    String openButtonTitle();

    /**
     * Translated "R Projects (*.RProj)".
     *
     * @return translated "R Projects (*.RProj)"
     */
    @DefaultMessage("R Projects (*.RProj)")
    @Key("rProjectsFilter")
    String rProjectsFilter();

    /**
     * Translated "Open in new session".
     *
     * @return translated "Open in new session"
     */
    @DefaultMessage("Open in new session")
    @Key("newSessionCheckLabel")
    String newSessionCheckLabel();

    /**
     * Translated "Create".
     *
     * @return translated "Create"
     */
    @DefaultMessage("Create")
    @Key("createButtonTitle")
    String createButtonTitle();

    /**
     * Translated "Selected path breadcrumb".
     *
     * @return translated "Selected path breadcrumb"
     */
    @DefaultMessage("Selected path breadcrumb")
    @Key("pathBreadCrumbSelectPath")
    String pathBreadCrumbSelectPath();

    /**
     * Translated "Selected path breadcrumb".
     *
     * @return translated "Go to directory"
     */
    @DefaultMessage("Go to directory")
    @Key("pathBreadCrumbButtonTitle")
    String pathBreadCrumbButtonTitle();

    /**
     * Translated "Go to project directory".
     *
     * @return translated "Go to project directory"
     */
    @DefaultMessage("Go to project directory")
    @Key("projectIconDesc")
    String projectIconDesc();

    /**
     * Translated "Home".
     *
     * @return translated "Home"
     */
    @DefaultMessage("Home")
    @Key("anchorHomeText")
    String anchorHomeText();

    /**
     * Translated "Cloud".
     *
     * @return translated "Cloud"
     */
    @DefaultMessage("Cloud")
    @Key("cloudHomeText")
    String cloudHomeText();

    /**
     * Translated "Go To Folder".
     *
     * @return translated "Go To Folder"
     */
    @DefaultMessage("Go To Folder")
    @Key("browseFolderCaption")
    String browseFolderCaption();

    /**
     * Translated "Path to folder (use ~ for home directory):".
     *
     * @return translated "Path to folder (use ~ for home directory):"
     */
    @DefaultMessage("Path to folder (use ~ for home directory):")
    @Key("browseFolderLabel")
    String browseFolderLabel();

    /**
     * Translated "Confirm Overwrite".
     *
     * @return translated "Confirm Overwrite"
     */
    @DefaultMessage("Confirm Overwrite")
    @Key("showOverwriteCaption")
    String showOverwriteCaption();

    /**
     * Translated "This file already exists. Do you want to replace it?".
     *
     * @return translated "This file already exists. Do you want to replace it?"
     */
    @DefaultMessage("This file already exists. Do you want to replace it?")
    @Key("showOverwriteMessage")
    String showOverwriteMessage();


    /**
     * Translated "R session".
     *
     * @return translated "R session"
     */
    @DefaultMessage("R session")
    @Key("rSessionMessage")
    String rSessionMessage();

    /**
     * Translated "RStudio Server".
     *
     * @return translated "RStudio Server"
     */
    @DefaultMessage("RStudio Server")
    @Key("rStudioServerMessage")
    String rStudioServerMessage();

    /**
     * Translated "OK".
     *
     * @return translated "OK"
     */
    @DefaultMessage("OK")
    @Key("okButtonTitle")
    String okButtonTitle();

    /**
     * Translated "Apply".
     *
     * @return translated "Apply"
     */
    @DefaultMessage("Apply")
    @Key("addButtonTitle")
    String addButtonTitle();

    /**
     * Translated "Saving...".
     *
     * @return translated "Saving..."
     */
    @DefaultMessage("Saving...")
    @Key("progressIndicatorTitle")
    String progressIndicatorTitle();

    /**
     * Translated "Restart Required".
     *
     * @return translated "Restart Required"
     */
    @DefaultMessage("Restart Required")
    @Key("restartRequiredCaption")
    String restartRequiredCaption();

    /**
     * Translated "Working...".
     *
     * @return translated "Working..."
     */
    @DefaultMessage("Working...")
    @Key("promiseWithProgress")
    String promiseWithProgress();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    @Key("promiseWithProgressError")
    String promiseWithProgressError();

    /**
     * Translated "Documents".
     *
     * @return translated "Documents"
     */
    @DefaultMessage("Documents")
    @Key("documentsTabList")
    String documentsTabList();

    /**
     * Translated "Rename".
     *
     * @return translated "Rename"
     */
    @DefaultMessage("Rename")
    @Key("renameMenuItem")
    String renameMenuItem();

    /**
     * Translated "Copy Path".
     *
     * @return translated "Copy Path"
     */
    @DefaultMessage("Copy Path")
    @Key("copyPathMenuItem")
    String copyPathMenuItem();

    /**
     * Translated "Set Working Directory".
     *
     * @return translated "Set Working Directory"
     */
    @DefaultMessage("Set Working Directory")
    @Key("setWorkingDirMenuItem")
    String setWorkingDirMenuItem();

    /**
     * Translated "Close".
     *
     * @return translated "Close"
     */
    @DefaultMessage("Close")
    @Key("closeMenuItem")
    String closeMenuItem();

    /**
     * Translated "Close All".
     *
     * @return translated "Close All"
     */
    @DefaultMessage("Close All")
    @Key("closeAllMenuItem")
    String closeAllMenuItem();

    /**
     * Translated "Close All Others".
     *
     * @return translated "Close All Others"
     */
    @DefaultMessage("Close All Others")
    @Key("closeOthersMenuItem")
    String closeOthersMenuItem();

    /**
     * Translated "Close document tab".
     *
     * @return translated "Close document tab"
     */
    @DefaultMessage("Close document tab")
    @Key("closeTabText")
    String closeTabText();

    /**
     * Translated "Could Not Change Setting"
     *
     * @return translated "Could Not Change Setting"
     */
    @DefaultMessage("Could Not Change Setting")
    @Key("docPropErrorMessage")
    String docPropErrorMessage();

    /**
     * Translated "Close popup"
     *
     * @return translated "Close popup"
     */
    @DefaultMessage("Close popup")
    @Key("closePopupText")
    String closePopupText();

    /**
     * Translated "Please use a complete file path."
     *
     * @return translated "Please use a complete file path."
     */
    @DefaultMessage("Please use a complete file path.")
    @Key("themeButtonOnErrorMessage")
    String themeButtonOnErrorMessage();

    /**
     * Translated "Please use a complete file path."
     *
     * @return translated "Unexpected empty response from server"
     */
    @DefaultMessage("Unexpected empty response from server")
    @Key("onSubmitErrorMessage")
    String onSubmitErrorMessage();

    /**
     * Translated "Install"
     *
     * @return translated "Install"
     */
    @DefaultMessage("Install")
    @Key("installText")
    String installText();

    /**
     * Translated "Don''t Show Again"
     *
     * @return translated "Don''t Show Again"
     */
    @DefaultMessage("Don''t Show Again")
    @Key("donnotShowAgain")
    String donnotShowAgain();

    /**
     * Translated "Markdown format changes require a reload of the visual editor."
     *
     * @return translated "Markdown format changes require a reload of the visual editor."
     */
    @DefaultMessage("Markdown format changes require a reload of the visual editor.")
    @Key("showPanmirrorText")
    String showPanmirrorText();

    /**
     * Translated "Reload Now"
     *
     * @return translated "Reload Now"
     */
    @DefaultMessage("Reload Now")
    @Key("reloadNowText")
    String reloadNowText();

    /**
     * Translated "Install TinyTeX"
     *
     * @return translated "Install TinyTeX"
     */
    @DefaultMessage("Install TinyTeX")
    @Key("installTinyTexText")
    String installTinyTexText();

    /**
     * Translated "This document is read only."
     *
     * @return translated "This document is read only."
     */
    @DefaultMessage("This document is read only.")
    @Key("showReadOnlyWarningText")
    String showReadOnlyWarningText();

    /**
     * Translated "This document is read only. Generated from:"
     *
     * @return translated "This document is read only. Generated from:"
     */
    @DefaultMessage("This document is read only. Generated from:")
    @Key("showReadOnlyWarningGeneratedText")
    String showReadOnlyWarningGeneratedText();


    /**
     * Translated "Add"
     *
     * @return translated "Add"
     */
    @DefaultMessage("Add")
    @Key("buttonAddCaption")
    String buttonAddCaption();

    /**
     * Translated "Remove"
     *
     * @return translated "Remove"
     */
    @DefaultMessage("Remove")
    @Key("buttonRemoveCaption")
    String buttonRemoveCaption();

    /**
     * Translated "Local repositories:"
     *
     * @return translated "Local repositories:"
     */
    @DefaultMessage("Local repositories:")
    @Key("localReposText")
    String localReposText();

    /**
     * Translated "Help on local Packrat repositories"
     *
     * @return translated "Help on local Packrat repositories"
     */
    @DefaultMessage("Help on local Packrat repositories")
    @Key("localReposTitle")
    String localReposTitle();

    /**
     * Translated "Add Local Repository"
     *
     * @return translated "Add Local Repository"
     */
    @DefaultMessage("Add Local Repository")
    @Key("addLocalRepoText")
    String addLocalRepoText();

    /**
     * Translated "Error"
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();


    /**
     * Translated "No bindings available"
     *
     * @return translated "No bindings available"
     */
    @DefaultMessage("No bindings available")
    @Key("emptyLabel")
    String emptyLabel();

    /**
     * Translated "Keyboard Shortcuts"
     *
     * @return translated "Keyboard Shortcuts"
     */
    @DefaultMessage("Keyboard Shortcuts")
    @Key("keyboardShortcutsText")
    String keyboardShortcutsText();

    /**
     * Translated "Apply"
     *
     * @return translated "Apply"
     */
    @DefaultMessage("Apply")
    @Key("applyThemeButtonText")
    String applyThemeButtonText();

    /**
     * Translated "All"
     *
     * @return translated "All"
     */
    @DefaultMessage("All")
    @Key("radioButtonLabel")
    String radioButtonLabel();

    /**
     * Translated "Customized"
     *
     * @return translated "Customized"
     */
    @DefaultMessage("Customized")
    @Key("radioCustomizedLabel")
    String radioCustomizedLabel();

    /**
     * Translated "Filter keyboard shortcuts"
     *
     * @return translated "Filter keyboard shortcuts"
     */
    @DefaultMessage("Filter keyboard shortcuts")
    @Key("filterWidgetLabel")
    String filterWidgetLabel();

    /**
     * Translated "Filter..."
     *
     * @return translated "Filter..."
     */
    @DefaultMessage("Filter...")
    @Key("filterWidgetPlaceholderText")
    String filterWidgetPlaceholderText();

    /**
     * Translated "Reset..."
     *
     * @return translated "Reset..."
     */
    @DefaultMessage("Reset...")
    @Key("resetButtonText")
    String resetButtonText();

    /**
     * Translated "Reset Keyboard Shortcuts"
     *
     * @return translated "Reset Keyboard Shortcuts"
     */
    @DefaultMessage("Reset Keyboard Shortcuts")
    @Key("resetKeyboardShortcutsCaption")
    String resetKeyboardShortcutsCaption();

    /**
     * Translated "Resetting Keyboard Shortcuts..."
     *
     * @return translated "Resetting Keyboard Shortcuts..."
     */
    @DefaultMessage("Resetting Keyboard Shortcuts...")
    @Key("resetKeyboardShortcutsProgress")
    String resetKeyboardShortcutsProgress();

    /**
     * Translated "Name"
     *
     * @return translated "Name"
     */
    @DefaultMessage("Name")
    @Key("nameColumnText")
    String nameColumnText();

    /**
     * Translated "Shortcut"
     *
     * @return translated "Shortcut"
     */
    @DefaultMessage("Shortcut")
    @Key("editableTextColumn")
    String editableTextColumn();

    /**
     * Translated "Scope"
     *
     * @return translated "Scope"
     */
    @DefaultMessage("Scope")
    @Key("scopeTextColumn")
    String scopeTextColumn();

    /**
     * Translated "Failed to find <input> element in table"
     *
     * @return translated "Failed to find <input> element in table"
     */
    @DefaultMessage("Failed to find <input> element in table")
    @Key("tagNameErrorMessage")
    String tagNameErrorMessage();

    /**
     * Translated "Show:"
     *
     * @return translated "Show:"
     */
    @DefaultMessage("Show:")
    @Key("radioShowLabel")
    String radioShowLabel();

    /**
     * Translated "Customizing Keyboard Shortcuts"
     *
     * @return translated "Customizing Keyboard Shortcuts"
     */
    @DefaultMessage("Customizing Keyboard Shortcuts")
    @Key("customizeKeyboardHelpLink")
    String customizeKeyboardHelpLink();

    /**
     * Translated "Masked by RStudio command: "
     *
     * @return translated "Masked by RStudio command: "
     */
    @DefaultMessage("Masked by RStudio command: ")
    @Key("addMaskedCommandStylesText")
    String addMaskedCommandStylesText();

    /**
     * Translated "Conflicts with command: "
     *
     * @return translated "Conflicts with command: "
     */
    @DefaultMessage("Conflicts with command: ")
    @Key("addConflictCommandStylesText")
    String addConflictCommandStylesText();

    /**
     * Translated "Refresh Automatically"
     *
     * @return translated "Refresh Automatically"
     */
    @DefaultMessage("Refresh Automatically")
    @Key("refreshAutomaticallyLabel")
    String refreshAutomaticallyLabel();

    /**
     * Translated "Stop"
     *
     * @return translated "Stop"
     */
    @DefaultMessage("Stop")
    @Key("stopButtonText")
    String stopButtonText();

    /**
     * Translated "Secondary Window"
     *
     * @return translated "Secondary Window"
     */
    @DefaultMessage("Secondary Window")
    @Key("satelliteToolBarText")
    String satelliteToolBarText();

    /**
     * Translated "Clear text"
     *
     * @return translated "Clear text"
     */
    @DefaultMessage("Clear text")
    @Key("searchWidgetClearText")
    String searchWidgetClearText();

    /**
     * Translated "(None)"
     *
     * @return translated "(None)"
     */
    @DefaultMessage("(None)")
    @Key("selectWidgetListBoxNone")
    String selectWidgetListBoxNone();

    /**
     * Translated "Keyboard Shortcut Quick Reference"
     *
     * @return translated "Keyboard Shortcut Quick Reference"
     */
    @DefaultMessage("Keyboard Shortcut Quick Reference")
    @Key("shortcutHeaderText")
    String shortcutHeaderText();

    /**
     * Translated "Tabs"
     *
     * @return translated "Tabs"
     */
    @DefaultMessage("Tabs")
    @Key("tabsGroupName")
    String tabsGroupName();

    /**
     * Translated "Panes"
     *
     * @return translated "Panes"
     */
    @DefaultMessage("Panes")
    @Key("panesGroupName")
    String panesGroupName();

    /**
     * Translated "Files"
     *
     * @return translated "Files"
     */
    @DefaultMessage("Files")
    @Key("filesGroupName")
    String filesGroupName();

    /**
     * Translated "Main Menu (Server)"
     *
     * @return translated "Main Menu (Server)"
     */
    @DefaultMessage("Main Menu (Server)")
    @Key("mainMenuGroupName")
    String mainMenuGroupName();

    /**
     * Translated "Source Navigation"
     *
     * @return translated "Source Navigation"
     */
    @DefaultMessage("Source Navigation")
    @Key("sourceNavigationGroupName")
    String sourceNavigationGroupName();

    /**
     * Translated "Execute"
     *
     * @return translated "Execute"
     */
    @DefaultMessage("Execute")
    @Key("executeGroupName")
    String executeGroupName();

    /**
     * Translated "Source Editor"
     *
     * @return translated "Source Editor"
     */
    @DefaultMessage("Source Editor")
    @Key("sourceEditorGroupName")
    String sourceEditorGroupName();

    /**
     * Translated "Debug"
     *
     * @return translated "Debug"
     */
    @DefaultMessage("Debug")
    @Key("debugGroupName")
    String debugGroupName();

    /**
     * Translated "Accessibility"
     *
     * @return translated "Accessibility"
     */
    @DefaultMessage("Accessibility")
    @Key("accessibilityGroupName")
    String accessibilityGroupName();

    /**
     * Translated "Source Control"
     *
     * @return translated "Source Control"
     */
    @DefaultMessage("Source Control")
    @Key("sourceControlGroupName")
    String sourceControlGroupName();

    /**
     * Translated "Build"
     *
     * @return translated "Build"
     */
    @DefaultMessage("Build")
    @Key("buildGroupName")
    String buildGroupName();

    /**
     * Translated "Console"
     *
     * @return translated "Console"
     */
    @DefaultMessage("Console")
    @Key("consoleGroupName")
    String consoleGroupName();

    /**
     * Translated "Terminal"
     *
     * @return translated "Terminal"
     */
    @DefaultMessage("Terminal")
    @Key("terminalGroupName")
    String terminalGroupName();

    /**
     * Translated "Other"
     *
     * @return translated "Other"
     */
    @DefaultMessage("Other")
    @Key("otherGroupName")
    String otherGroupName();

    /**
     * Translated "Add Shift to zoom (maximize) pane."
     *
     * @return translated "Add Shift to zoom (maximize) pane."
     */
    @DefaultMessage("Add Shift to zoom (maximize) pane.")
    @Key("addShiftPTag")
    String addShiftPTag();

    /**
     * Translated "[Use Default]"
     *
     * @return translated "[Use Default]"
     */
    @DefaultMessage("[Use Default]")
    @Key("useDefaultPrefix")
    String useDefaultPrefix();

    /**
     * Translated "You must enter a value."
     *
     * @return translated "You must enter a value."
     */
    @DefaultMessage("You must enter a value.")
    @Key("validateMessage")
    String validateMessage();

    /**
     * Translated "Not a valid number."
     *
     * @return translated "Not a valid number."
     */
    @DefaultMessage("Not a valid number.")
    @Key("notValidNumberMessage")
    String notValidNumberMessage();

    /**
     * Translated "Vim Keyboard Shortcuts"
     *
     * @return translated "Vim Keyboard Shortcuts"
     */
    @DefaultMessage("Vim Keyboard Shortcuts")
    @Key("vimKeyboardShortcutsText")
    String vimKeyboardShortcutsText();

    /**
     * Translated "Next"
     *
     * @return translated "Next"
     */
    @DefaultMessage("Next")
    @Key("nextButtonText")
    String nextButtonText();

    /**
     * Translated "Back"
     *
     * @return translated "Back"
     */
    @DefaultMessage("Back")
    @Key("backButtonText")
    String backButtonText();

    /**
     * Translated "Info"
     *
     * @return translated "Info"
     */
    @DefaultMessage("Info")
    @Key("dialogInfoText")
    String dialogInfoText();

    /**
     * Translated "Directory Contents"
     *
     * @return translated "Directory Contents"
     */
    @DefaultMessage("Directory Contents")
    @Key("directoryContentsLabel")
    String directoryContentsLabel();

    /**
     * Translated "New Folder"
     *
     * @return translated "New Folder"
     */
    @DefaultMessage("New Folder")
    @Key("newFolderTitle")
    String newFolderTitle();

    /**
     * Translated "Folder name"
     *
     * @return translated "Folder name"
     */
    @DefaultMessage("Folder name")
    @Key("folderNameLabel")
    String folderNameLabel();

    /**
     * Translated "Warning"
     *
     * @return translated "Warning"
     */
    @DefaultMessage("Warning")
    @Key("dialogWarningText")
    String dialogWarningText();

    /**
     * Translated "Question"
     *
     * @return translated "Question"
     */
    @DefaultMessage("Question")
    @Key("dialogQuestionText")
    String dialogQuestionText();

    /**
     * Translated "Popup Blocked"
     *
     * @return translated "Popup Blocked"
     */
    @DefaultMessage("Popup Blocked")
    @Key("dialogPopupBlockedText")
    String dialogPopupBlockedText();

    /**
     * Translated "Error"
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    @Key("dialogErrorText")
    String dialogErrorText();

    /**
     * Translated "Manual Refresh Only"
     *
     * @return translated "Manual Refresh Only"
     */
    @DefaultMessage("Manual Refresh Only")
    @Key("manualRefreshLabel")
    String manualRefreshLabel();

    /**
     * Translated "Busy"
     *
     * @return translated "Busy"
     */
    @DefaultMessage("Busy")
    @Key("busyLabel")
    String busyLabel();

    /**
     * Translated "[REDACTED]"
     *
     * @return translated "[REDACTED]"
     */
    @DefaultMessage("[REDACTED]")
    @Key("redactedText")
    String redactedText();


    /**
     * Translated "Vim keyboard shortcut help not screen reader accessible. Press any key to close."
     *
     * @return translated "Vim keyboard shortcut help not screen reader accessible. Press any key to close."
     */
    @DefaultMessage("Vim keyboard shortcut help not screen reader accessible. Press any key to close.")
    @Key("vimKeyboardShortcutHelpMessage")
    String vimKeyboardShortcutHelpMessage();

    /**
     * Translated "We attempted to open an external browser window, but the action was prevented by your popup blocker. You can attempt to open the window again by pressing the "Try Again" button below. NOTE: To prevent seeing this message in the future, you should configure your browser to allow popup windows for {0}."
     *
     * @return translated "We attempted to open an external browser window, but the action was prevented by your popup blocker. You can attempt to open the window again by pressing the "Try Again" button below. NOTE: To prevent seeing this message in the future, you should configure your browser to allow popup windows for {0}."
     */
    @DefaultMessage("We attempted to open an external browser window, but the action was prevented by your popup blocker. You can attempt to open the window again by pressing the \"Try Again\" button below. NOTE: To prevent seeing this message in the future, you should configure your browser to allow popup windows for {0}.")
    @Key("showPopupBlockMessage")
    String showPopupBlockMessage(String hostName);

    /**
     * Translated "Status code {0} returned by {1} when executing ''{2}''"
     *
     * @return translated "Status code {0} returned by {1} when executing ''{2}''"
     */
    @DefaultMessage("Status code {0} returned by {1} when executing ''{2}''")
    @Key("rpcErrorMessage")
    String rpcErrorMessage(String statusCode, String desktop, String method);

    /**
     * Translated "Unable to establish connection with {0} when executing ''{1}''"
     *
     * @return translated "Unable to establish connection with {0} when executing ''{1}''"
     */
    @DefaultMessage("Unable to establish connection with {0} when executing ''{1}''")
    @Key("rpcOverrideErrorMessage")
    String rpcOverrideErrorMessage(String desktop, String method);

    /**
     * Translated "You need to restart RStudio in order for these changes to take effect. Do you want to do this now?"
     *
     * @return translated "You need to restart RStudio in order for these changes to take effect. Do you want to do this now?"
     */
    @DefaultMessage("You need to restart RStudio in order for these changes to take effect. Do you want to do this now?")
    @Key("restartRequiredMessage")
    String restartRequiredMessage();

    /**
     * Translated "{0} minimized"
     *
     * @return translated "{0} minimized"
     */
    @DefaultMessage("{0} minimized")
    @Key("minimizedTabListRole")
    String minimizedTabListRole(String accessibleName);

    /**
     * Translated "Close"
     *
     * @return translated "Close"
     */
    @DefaultMessage("Close")
    @Key("closeText")
    String closeText();

    /**
     * Translated "Close {0} tab"
     *
     * @return translated "Close {0} tab"
     */
    @DefaultMessage("Close {0} tab")
    @Key("closeButtonText")
    String closeButtonText(String title);

    /**
     * Translated "Minimize {0}"
     *
     * @return translated "Minimize {0}"
     */
    @DefaultMessage("Minimize {0}")
    @Key("minimizeState")
    String minimizeState(String name);

    /**
     * Translated "Maximize {0}"
     *
     * @return translated "Maximize {0}"
     */
    @DefaultMessage("Maximize {0}")
    @Key("maximizeState")
    String maximizeState(String name);

    /**
     * Translated "Restore {0}"
     *
     * @return translated "Restore {0}"
     */
    @DefaultMessage("Restore {0}")
    @Key("normalState")
    String normalState(String name);

    /**
     * Translated "Hide {0}"
     *
     * @return translated "Hide {0}"
     */
    @DefaultMessage("Hide {0}")
    @Key("hideState")
    String hideState(String name);

    /**
     * Translated "Exclusive {0}"
     *
     * @return translated "Exclusive {0}"
     */
    @DefaultMessage("Exclusive {0}")
    @Key("exclusiveState")
    String exclusiveState(String name);

    /**
     * Translated "Package {0} required but is not installed."
     *
     * @return translated "Package {0} required but is not installed."
     */
    @DefaultMessage("Package {0} required but is not installed.")
    @Key("package1Message")
    String package1Message(String packages);

    /**
     * Translated "Package {0} and {1} required but are not installed."
     *
     * @return translated "Package {0} and {1} required but are not installed."
     */
    @DefaultMessage("Package {0} and {1} required but are not installed.")
    @Key("packages2Message")
    String packages2Message(String package0, String package1);

    /**
     * Translated "Packages {0} , {1} , and {2} required but are not installed."
     *
     * @return translated "Packages {0} , {1} , and {2} required but are not installed."
     */
    @DefaultMessage("Packages {0} , {1} , and {2} required but are not installed.")
    @Key("packages3Message")
    String packages3Message(String package0, String package1, String package2);

    /**
     * Translated "Packages {0} , {1} , and {2} others required but are not installed."
     *
     * @return translated "Packages {0} , {1} , and {2} others required but are not installed."
     */
    @DefaultMessage("Packages {0} , {1} , and {2} others required but are not installed.")
    @Key("otherPackagesMessage")
    String otherPackagesMessage(String package0, String package1, String package2);

    /**
     * Translated "Are you sure you want to reset keyboard shortcuts to their default values? This action cannot be undone."
     *
     * @return translated "Are you sure you want to reset keyboard shortcuts to their default values? This action cannot be undone."
     */
    @DefaultMessage("Are you sure you want to reset keyboard shortcuts to their default values? This action cannot be undone.")
    @Key("resetKeyboardShortcutsMessage")
    String resetKeyboardShortcutsMessage();

    /**
     * Translated "{0} must be a valid number."
     *
     * @return translated "{0} must be a valid number."
     */
    @DefaultMessage("{0} must be a valid number.")
    @Key("rStudioGinjectorErrorMessage")
    String rStudioGinjectorErrorMessage(String label);

    /**
     * Translated "{0} must be greater than or equal to {1}."
     *
     * @return translated "{0} must be greater than or equal to {1}."
     */
    @DefaultMessage("{0} must be greater than or equal to {1}.")
    @Key("rStudioGinjectorGreaterThanError")
    String rStudioGinjectorGreaterThanError(String label, int minValue);

    /**
     * Translated "{0} must be less than or equal to {1}."
     *
     * @return translated "{0} must be less than or equal to {1}."
     */
    @DefaultMessage("{0} must be less than or equal to {1}.")
    @Key("rStudioGinjectorLessThanError")
    String rStudioGinjectorLessThanError(String label, int maxValue);

    /**
     * Translated "Operation completed "
     *
     * @return translated "Operation completed "
     */
    @DefaultMessage("Operation completed ")
    @Key("operationCompletedText")
    String operationCompletedText();

    /**
     * Translated "{0} completed"
     *
     * @return translated "{0} completed"
     */
    @DefaultMessage("{0} completed")
    @Key("completedText")
    String completedText(String labelText);

    /**
     * Translated "Clear".
     *
     * @return translated "Clear"
     */
    @DefaultMessage("Clear")
    @Key("clearLabel")
    String clearLabel();

    /**
     * Translated "Browse...".
     *
     * @return translated "Browse..."
     */
    @DefaultMessage("Browse...")
    @Key("fileChooserTextBoxBrowseLabel")
    String fileChooserTextBoxBrowseLabel();

    /**
     * Translated "Choose File".
     *
     * @return translated "Choose File"
     */
    @DefaultMessage("Choose File")
    @Key("chooseFileCaption")
    String chooseFileCaption();

    /**
     * Translated "Ctrl+".
     *
     * @return translated "Ctrl+"
     */
    @DefaultMessage("Ctrl+")
    @Key("keyComboCtrl")
    String keyComboCtrl();

    /**
     * Translated "Alt+".
     *
     * @return translated "Alt+"
     */
    @DefaultMessage("Alt+")
    @Key("keyComboAlt")
    String keyComboAlt();

    /**
     * Translated "Shift+".
     *
     * @return translated "Shift+"
     */
    @DefaultMessage("Shift+")
    @Key("keyComboShift")
    String keyComboShift();

    /**
     * Translated "Cmd+".
     *
     * @return translated "Cmd+"
     */
    @DefaultMessage("Cmd+")
    @Key("keyComboCmd")
    String keyComboCmd();

    /**
     * Translated "Enter".
     *
     * @return translated "Enter"
     */
    @DefaultMessage("Enter")
    @Key("keyNameEnter")
    String keyNameEnter();

    /**
     * Translated "Left".
     *
     * @return translated "Left"
     */
    @DefaultMessage("Left")
    @Key("keyNameLeft")
    String keyNameLeft();

    /**
     * Translated "Right".
     *
     * @return translated "Right"
     */
    @DefaultMessage("Right")
    @Key("keyNameRight")
    String keyNameRight();

    /**
     * Translated "Up".
     *
     * @return translated "Up"
     */
    @DefaultMessage("Up")
    @Key("keyNameUp")
    String keyNameUp();

    /**
     * Translated "Down".
     *
     * @return translated "Down"
     */
    @DefaultMessage("Down")
    @Key("keyNameDown")
    String keyNameDown();

    /**
     * Translated "Tab".
     *
     * @return translated "Tab"
     */
    @DefaultMessage("Tab")
    @Key("keyNameTab")
    String keyNameTab();

    /**
     * Translated "PageUp".
     *
     * @return translated "PageUp"
     */
    @DefaultMessage("PageUp")
    @Key("keyNamePageUp")
    String keyNamePageUp();

    /**
     * Translated "PageDown".
     *
     * @return translated "PageDown"
     */
    @DefaultMessage("PageDown")
    @Key("keyNamePageDown")
    String keyNamePageDown();

    /**
     * Translated "Backspace".
     *
     * @return translated "Backspace"
     */
    @DefaultMessage("Backspace")
    @Key("keyNameBackspace")
    String keyNameBackspace();

    /**
     * Translated "Space".
     *
     * @return translated "Space"
     */
    @DefaultMessage("Space")
    @Key("keyNameSpace")
    String keyNameSpace();

}
