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
    String cancelLabel();
    
    /**
     * Translated "Reset".
     *
     * @return translated "Reset"
     */
    @DefaultMessage("Reset")
    String resetLabel();

    /**
     * Translated "No".
     *
     * @return translated "No"
     */
    @DefaultMessage("No")
    String noLabel();

    /**
     * Translated "Yes".
     *
     * @return translated "Yes"
     */
    @DefaultMessage("Yes")
    String yesLabel();

    /**
     * Translated "OK".
     *
     * @return translated "OK"
     */
    @DefaultMessage("OK")
    String okayLabel();
    
    /**
     * Translated "Copy to Clipboard".
     *
     * @return translated "Copy to Clipboard"
     */
    @DefaultMessage("Copy to Clipboard")
    String copyToClipboardLabel();

    /**
     * Translated "Not Yet Implemented".
     *
     * @return translated "Not Yet Implemented"
     */
    @DefaultMessage("Not Yet Implemented")
    String notYetImplementedCaption();

    /**
     * Translated "This feature has not yet been implemented.".
     *
     * @return translated "This feature has not yet been implemented."
     */
    @DefaultMessage("This feature has not yet been implemented.")
    String notYetImplementedMessage();

    /**
     * Translated "Popup Blocked".
     *
     * @return translated "Popup Blocked"
     */
    @DefaultMessage("Popup Blocked")
    String popupBlockCaption();

    /**
     * Translated "Try Again".
     *
     * @return translated "Try Again"
     */
    @DefaultMessage("Try Again")
    String popupBlockTryAgainLabel();

    /**
     * Translated "{0} second".
     *
     * @return translated "{0} second"
     */
    @DefaultMessage("{0} second")
    String secondLabel(int second);

    /**
     * Translated "{0} seconds".
     *
     * @return translated "{0} seconds"
     */
    @DefaultMessage("{0} seconds")
    String secondPluralLabel(int seconds);

    /**
     * Translated "{0} minute".
     *
     * @return translated "{0} minute"
     */
    @DefaultMessage("{0} minute")
    String minuteLabel(int minute);

    /**
     * Translated "{0} minutes".
     *
     * @return translated "{0} minutes"
     */
    @DefaultMessage("{0} minutes")
    String minutePluralLabel(int minutes);

    /**
     * Translated "{0} hour".
     *
     * @return translated "{0} hour"
     */
    @DefaultMessage("{0} hour")
    String hourLabel(int hour);

    /**
     * Translated "{0} hours".
     *
     * @return translated "{0} hours"
     */
    @DefaultMessage("{0} hours")
    String hourPluralLabel(int hours);

    /**
     * Translated "Type shortcuts to see if they are bound to a command. Close this message bar when done.".
     *
     * @return translated "Type shortcuts to see if they are bound to a command. Close this message bar when done."
     */
    @DefaultMessage("Type shortcuts to see if they are bound to a command. Close this message bar when done.")
    String reportShortCutMessage();

    /**
     * Translated "Multi-gesture shortcut pending".
     *
     * @return translated "Multi-gesture shortcut pending"
     */
    @DefaultMessage("Multi-gesture shortcut pending")
    String multiGestureMessage();

    /**
     * Translated "Shortcut not bound".
     *
     * @return translated "Shortcut not bound"
     */
    @DefaultMessage("Shortcut not bound")
    String shortcutUnBoundMessage();

    /**
     * Translated "Name is empty".
     *
     * @return translated "Name is empty"
     */
    @DefaultMessage("Name is empty")
    String nameEmptyMessage();

    /**
     * Translated "Names should not start or end with spaces".
     *
     * @return translated "Names should not start or end with spaces"
     */
    @DefaultMessage("Names should not start or end with spaces")
    String nameStartWithMessage();

    /**
     * Translated "Illegal character: /".
     *
     * @return translated "Illegal character: /"
     */
    @DefaultMessage("Illegal character: /")
    String nameIllegalCharacterMessage();

    /**
     * Translated "Illegal name".
     *
     * @return translated "Illegal name"
     */
    @DefaultMessage("Illegal name")
    String illegalNameMessage();

    /**
     * Translated "Folder".
     *
     * @return translated "Folder"
     */
    @DefaultMessage("Folder")
    String fileNameLabel();

    /**
     * Translated "File name".
     *
     * @return translated "File name"
     */
    @DefaultMessage("File name")
    String getFilenameLabel();

    /**
     * Translated "File does not exist".
     *
     * @return translated "File does not exist"
     */
    @DefaultMessage("File does not exist")
    String nonexistentFileMessage();

    /**
     * Translated "Open Project".
     *
     * @return translated "Open Project"
     */
    @DefaultMessage("Open Project")
    String openProjectTitle();

    /**
     * Translated "Open".
     *
     * @return translated "Open"
     */
    @DefaultMessage("Open")
    String openButtonTitle();

    /**
     * Translated "R Projects (*.RProj)".
     *
     * @return translated "R Projects (*.RProj)"
     */
    @DefaultMessage("R Projects (*.RProj)")
    String rProjectsFilter();

    /**
     * Translated "Open in new session".
     *
     * @return translated "Open in new session"
     */
    @DefaultMessage("Open in new session")
    String newSessionCheckLabel();

    /**
     * Translated "Create".
     *
     * @return translated "Create"
     */
    @DefaultMessage("Create")
    String createButtonTitle();

    /**
     * Translated "Selected path breadcrumb".
     *
     * @return translated "Selected path breadcrumb"
     */
    @DefaultMessage("Selected path breadcrumb")
    String pathBreadCrumbSelectPath();

    /**
     * Translated "Selected path breadcrumb".
     *
     * @return translated "Go to directory"
     */
    @DefaultMessage("Go to directory")
    String pathBreadCrumbButtonTitle();

    /**
     * Translated "Go to project directory".
     *
     * @return translated "Go to project directory"
     */
    @DefaultMessage("Go to project directory")
    String projectIconDesc();

    /**
     * Translated "Projects".
     *
     * @return translated "Projects"
     */
    @DefaultMessage("Projects")
    String projectsLabel();
    
    /**
     * Translated "Home".
     *
     * @return translated "Home"
     */
    @DefaultMessage("Home")
    String anchorHomeText();

    /**
     * Translated "Cloud".
     *
     * @return translated "Cloud"
     */
    @DefaultMessage("Cloud")
    String cloudHomeText();

    /**
     * Translated "Go To Folder".
     *
     * @return translated "Go To Folder"
     */
    @DefaultMessage("Go To Folder")
    String browseFolderCaption();

    /**
     * Translated "Path to folder (use ~ for home directory):".
     *
     * @return translated "Path to folder (use ~ for home directory):"
     */
    @DefaultMessage("Path to folder (use ~ for home directory):")
    String browseFolderLabel();

    /**
     * Translated "Confirm Overwrite".
     *
     * @return translated "Confirm Overwrite"
     */
    @DefaultMessage("Confirm Overwrite")
    String showOverwriteCaption();

    /**
     * Translated "This file already exists. Do you want to replace it?".
     *
     * @return translated "This file already exists. Do you want to replace it?"
     */
    @DefaultMessage("This file already exists. Do you want to replace it?")
    String showOverwriteMessage();


    /**
     * Translated "R session".
     *
     * @return translated "R session"
     */
    @DefaultMessage("R session")
    String rSessionMessage();

    /**
     * Translated "RStudio Server".
     *
     * @return translated "RStudio Server"
     */
    @DefaultMessage("RStudio Server")
    String rStudioServerMessage();

    /**
     * Translated "OK".
     *
     * @return translated "OK"
     */
    @DefaultMessage("OK")
    String okButtonTitle();

    /**
     * Translated "Apply".
     *
     * @return translated "Apply"
     */
    @DefaultMessage("Apply")
    String addButtonTitle();

    /**
     * Translated "Saving...".
     *
     * @return translated "Saving..."
     */
    @DefaultMessage("Saving...")
    String progressIndicatorTitle();

    /**
     * Translated "Restart Required".
     *
     * @return translated "Restart Required"
     */
    @DefaultMessage("Restart Required")
    String restartRequiredCaption();

    /**
     * Translated "Working...".
     *
     * @return translated "Working..."
     */
    @DefaultMessage("Working...")
    String promiseWithProgress();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    String promiseWithProgressError();

    /**
     * Translated "Documents".
     *
     * @return translated "Documents"
     */
    @DefaultMessage("Documents")
    String documentsTabList();

    /**
     * Translated "Rename".
     *
     * @return translated "Rename"
     */
    @DefaultMessage("Rename")
    String renameMenuItem();

    /**
     * Translated "Copy Path".
     *
     * @return translated "Copy Path"
     */
    @DefaultMessage("Copy Path")
    String copyPathMenuItem();

    /**
     * Translated "Set Working Directory".
     *
     * @return translated "Set Working Directory"
     */
    @DefaultMessage("Set Working Directory")
    String setWorkingDirMenuItem();

    /**
     * Translated "Close".
     *
     * @return translated "Close"
     */
    @DefaultMessage("Close")
    String closeMenuItem();

    /**
     * Translated "Close All".
     *
     * @return translated "Close All"
     */
    @DefaultMessage("Close All")
    String closeAllMenuItem();

    /**
     * Translated "Close All Others".
     *
     * @return translated "Close All Others"
     */
    @DefaultMessage("Close All Others")
    String closeOthersMenuItem();

    /**
     * Translated "Close document tab".
     *
     * @return translated "Close document tab"
     */
    @DefaultMessage("Close document tab")
    String closeTabText();

    /**
     * Translated "Could Not Change Setting"
     *
     * @return translated "Could Not Change Setting"
     */
    @DefaultMessage("Could Not Change Setting")
    String docPropErrorMessage();

    /**
     * Translated "Close popup"
     *
     * @return translated "Close popup"
     */
    @DefaultMessage("Close popup")
    String closePopupText();

    /**
     * Translated "Please use a complete file path."
     *
     * @return translated "Please use a complete file path."
     */
    @DefaultMessage("Please use a complete file path.")
    String themeButtonOnErrorMessage();

    /**
     * Translated "Please use a complete file path."
     *
     * @return translated "Unexpected empty response from server"
     */
    @DefaultMessage("Unexpected empty response from server")
    String onSubmitErrorMessage();

    /**
     * Translated "Install"
     *
     * @return translated "Install"
     */
    @DefaultMessage("Install")
    String installText();

    /**
     * Translated "Don''t Show Again"
     *
     * @return translated "Don''t Show Again"
     */
    @DefaultMessage("Don''t Show Again")
    String donnotShowAgain();

    /**
     * Translated "Markdown format changes require a reload of the visual editor."
     *
     * @return translated "Markdown format changes require a reload of the visual editor."
     */
    @DefaultMessage("Markdown format changes require a reload of the visual editor.")
    String showPanmirrorText();

    /**
     * Translated "Reload Now"
     *
     * @return translated "Reload Now"
     */
    @DefaultMessage("Reload Now")
    String reloadNowText();

    /**
     * Translated "Install TinyTeX"
     *
     * @return translated "Install TinyTeX"
     */
    @DefaultMessage("Install TinyTeX")
    String installTinyTexText();

    /**
     * Translated "This document is read only."
     *
     * @return translated "This document is read only."
     */
    @DefaultMessage("This document is read only.")
    String showReadOnlyWarningText();

    /**
     * Translated "This document is read only. Generated from:"
     *
     * @return translated "This document is read only. Generated from:"
     */
    @DefaultMessage("This document is read only. Generated from:")
    String showReadOnlyWarningGeneratedText();


    /**
     * Translated "Add"
     *
     * @return translated "Add"
     */
    @DefaultMessage("Add")
    String buttonAddCaption();

    /**
     * Translated "Remove"
     *
     * @return translated "Remove"
     */
    @DefaultMessage("Remove")
    String buttonRemoveCaption();

    /**
     * Translated "Local repositories:"
     *
     * @return translated "Local repositories:"
     */
    @DefaultMessage("Local repositories:")
    String localReposText();

    /**
     * Translated "Help on local Packrat repositories"
     *
     * @return translated "Help on local Packrat repositories"
     */
    @DefaultMessage("Help on local Packrat repositories")
    String localReposTitle();

    /**
     * Translated "Add Local Repository"
     *
     * @return translated "Add Local Repository"
     */
    @DefaultMessage("Add Local Repository")
    String addLocalRepoText();

    /**
     * Translated "Error"
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    String errorCaption();


    /**
     * Translated "No bindings available"
     *
     * @return translated "No bindings available"
     */
    @DefaultMessage("No bindings available")
    String emptyLabel();

    /**
     * Translated "Keyboard Shortcuts"
     *
     * @return translated "Keyboard Shortcuts"
     */
    @DefaultMessage("Keyboard Shortcuts")
    String keyboardShortcutsText();

    /**
     * Translated "Apply"
     *
     * @return translated "Apply"
     */
    @DefaultMessage("Apply")
    String applyThemeButtonText();

    /**
     * Translated "All"
     *
     * @return translated "All"
     */
    @DefaultMessage("All")
    String radioButtonLabel();

    /**
     * Translated "Customized"
     *
     * @return translated "Customized"
     */
    @DefaultMessage("Customized")
    String radioCustomizedLabel();

    /**
     * Translated "Filter keyboard shortcuts"
     *
     * @return translated "Filter keyboard shortcuts"
     */
    @DefaultMessage("Filter keyboard shortcuts")
    String filterWidgetLabel();

    /**
     * Translated "Filter..."
     *
     * @return translated "Filter..."
     */
    @DefaultMessage("Filter...")
    String filterWidgetPlaceholderText();

    /**
     * Translated "Reset..."
     *
     * @return translated "Reset..."
     */
    @DefaultMessage("Reset...")
    String resetButtonText();

    /**
     * Translated "Reset Keyboard Shortcuts"
     *
     * @return translated "Reset Keyboard Shortcuts"
     */
    @DefaultMessage("Reset Keyboard Shortcuts")
    String resetKeyboardShortcutsCaption();

    /**
     * Translated "Resetting Keyboard Shortcuts..."
     *
     * @return translated "Resetting Keyboard Shortcuts..."
     */
    @DefaultMessage("Resetting Keyboard Shortcuts...")
    String resetKeyboardShortcutsProgress();

    /**
     * Translated "Name"
     *
     * @return translated "Name"
     */
    @DefaultMessage("Name")
    String nameColumnText();

    /**
     * Translated "Shortcut"
     *
     * @return translated "Shortcut"
     */
    @DefaultMessage("Shortcut")
    String editableTextColumn();

    /**
     * Translated "Scope"
     *
     * @return translated "Scope"
     */
    @DefaultMessage("Scope")
    String scopeTextColumn();

    /**
     * Translated "Failed to find <input> element in table"
     *
     * @return translated "Failed to find <input> element in table"
     */
    @DefaultMessage("Failed to find <input> element in table")
    String tagNameErrorMessage();

    /**
     * Translated "Show:"
     *
     * @return translated "Show:"
     */
    @DefaultMessage("Show:")
    String radioShowLabel();

    /**
     * Translated "Customizing Keyboard Shortcuts"
     *
     * @return translated "Customizing Keyboard Shortcuts"
     */
    @DefaultMessage("Customizing Keyboard Shortcuts")
    String customizeKeyboardHelpLink();

    /**
     * Translated "Masked by RStudio command: "
     *
     * @return translated "Masked by RStudio command: "
     */
    @DefaultMessage("Masked by RStudio command: ")
    String addMaskedCommandStylesText();

    /**
     * Translated "Conflicts with command: "
     *
     * @return translated "Conflicts with command: "
     */
    @DefaultMessage("Conflicts with command: ")
    String addConflictCommandStylesText();

    /**
     * Translated "Refresh Automatically"
     *
     * @return translated "Refresh Automatically"
     */
    @DefaultMessage("Refresh Automatically")
    String refreshAutomaticallyLabel();

    /**
     * Translated "Stop"
     *
     * @return translated "Stop"
     */
    @DefaultMessage("Stop")
    String stopButtonText();

    /**
     * Translated "Secondary Window"
     *
     * @return translated "Secondary Window"
     */
    @DefaultMessage("Secondary Window")
    String satelliteToolBarText();

    /**
     * Translated "Clear text"
     *
     * @return translated "Clear text"
     */
    @DefaultMessage("Clear text")
    String searchWidgetClearText();

    /**
     * Translated "(None)"
     *
     * @return translated "(None)"
     */
    @DefaultMessage("(None)")
    String selectWidgetListBoxNone();

    /**
     * Translated "Keyboard Shortcut Quick Reference"
     *
     * @return translated "Keyboard Shortcut Quick Reference"
     */
    @DefaultMessage("Keyboard Shortcut Quick Reference")
    String shortcutHeaderText();

    /**
     * Translated "Tabs"
     *
     * @return translated "Tabs"
     */
    @DefaultMessage("Tabs")
    String tabsGroupName();

    /**
     * Translated "Panes"
     *
     * @return translated "Panes"
     */
    @DefaultMessage("Panes")
    String panesGroupName();

    /**
     * Translated "Files"
     *
     * @return translated "Files"
     */
    @DefaultMessage("Files")
    String filesGroupName();

    /**
     * Translated "Main Menu (Server)"
     *
     * @return translated "Main Menu (Server)"
     */
    @DefaultMessage("Main Menu (Server)")
    String mainMenuGroupName();

    /**
     * Translated "Source Navigation"
     *
     * @return translated "Source Navigation"
     */
    @DefaultMessage("Source Navigation")
    String sourceNavigationGroupName();

    /**
     * Translated "Execute"
     *
     * @return translated "Execute"
     */
    @DefaultMessage("Execute")
    String executeGroupName();

    /**
     * Translated "Source Editor"
     *
     * @return translated "Source Editor"
     */
    @DefaultMessage("Source Editor")
    String sourceEditorGroupName();

    /**
     * Translated "Debug"
     *
     * @return translated "Debug"
     */
    @DefaultMessage("Debug")
    String debugGroupName();

    /**
     * Translated "Accessibility"
     *
     * @return translated "Accessibility"
     */
    @DefaultMessage("Accessibility")
    String accessibilityGroupName();

    /**
     * Translated "Source Control"
     *
     * @return translated "Source Control"
     */
    @DefaultMessage("Source Control")
    String sourceControlGroupName();

    /**
     * Translated "Build"
     *
     * @return translated "Build"
     */
    @DefaultMessage("Build")
    String buildGroupName();

    /**
     * Translated "Console"
     *
     * @return translated "Console"
     */
    @DefaultMessage("Console")
    String consoleGroupName();

    /**
     * Translated "Terminal"
     *
     * @return translated "Terminal"
     */
    @DefaultMessage("Terminal")
    String terminalGroupName();

    /**
     * Translated "Other"
     *
     * @return translated "Other"
     */
    @DefaultMessage("Other")
    String otherGroupName();

    /**
     * Translated "Add Shift to zoom (maximize) pane."
     *
     * @return translated "Add Shift to zoom (maximize) pane."
     */
    @DefaultMessage("Add Shift to zoom (maximize) pane.")
    String addShiftPTag();

    /**
     * Translated "[Use Default]"
     *
     * @return translated "[Use Default]"
     */
    @DefaultMessage("[Use Default]")
    String useDefaultPrefix();

    /**
     * Translated "You must enter a value."
     *
     * @return translated "You must enter a value."
     */
    @DefaultMessage("You must enter a value.")
    String validateMessage();

    /**
     * Translated "Not a valid number."
     *
     * @return translated "Not a valid number."
     */
    @DefaultMessage("Not a valid number.")
    String notValidNumberMessage();

    /**
     * Translated "Vim Keyboard Shortcuts"
     *
     * @return translated "Vim Keyboard Shortcuts"
     */
    @DefaultMessage("Vim Keyboard Shortcuts")
    String vimKeyboardShortcutsText();

    /**
     * Translated "Next"
     *
     * @return translated "Next"
     */
    @DefaultMessage("Next")
    String nextButtonText();

    /**
     * Translated "Back"
     *
     * @return translated "Back"
     */
    @DefaultMessage("Back")
    String backButtonText();

    /**
     * Translated "Info"
     *
     * @return translated "Info"
     */
    @DefaultMessage("Info")
    String dialogInfoText();

    /**
     * Translated "Directory Contents"
     *
     * @return translated "Directory Contents"
     */
    @DefaultMessage("Directory Contents")
    String directoryContentsLabel();

    /**
     * Translated "New Folder"
     *
     * @return translated "New Folder"
     */
    @DefaultMessage("New Folder")
    String newFolderTitle();

    /**
     * Translated "Folder name"
     *
     * @return translated "Folder name"
     */
    @DefaultMessage("Folder name")
    String folderNameLabel();

    /**
     * Translated "Warning"
     *
     * @return translated "Warning"
     */
    @DefaultMessage("Warning")
    String dialogWarningText();

    /**
     * Translated "Question"
     *
     * @return translated "Question"
     */
    @DefaultMessage("Question")
    String dialogQuestionText();

    /**
     * Translated "Popup Blocked"
     *
     * @return translated "Popup Blocked"
     */
    @DefaultMessage("Popup Blocked")
    String dialogPopupBlockedText();

    /**
     * Translated "Error"
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    String dialogErrorText();

    /**
     * Translated "Manual Refresh Only"
     *
     * @return translated "Manual Refresh Only"
     */
    @DefaultMessage("Manual Refresh Only")
    String manualRefreshLabel();

    /**
     * Translated "Busy"
     *
     * @return translated "Busy"
     */
    @DefaultMessage("Busy")
    String busyLabel();

    /**
     * Translated "[REDACTED]"
     *
     * @return translated "[REDACTED]"
     */
    @DefaultMessage("[REDACTED]")
    String redactedText();


    /**
     * Translated "Vim keyboard shortcut help not screen reader accessible. Press any key to close."
     *
     * @return translated "Vim keyboard shortcut help not screen reader accessible. Press any key to close."
     */
    @DefaultMessage("Vim keyboard shortcut help not screen reader accessible. Press any key to close.")
    String vimKeyboardShortcutHelpMessage();

    /**
     * Translated "We attempted to open an external browser window, but the action was prevented by your popup blocker. You can attempt to open the window again by pressing the "Try Again" button below. NOTE: To prevent seeing this message in the future, you should configure your browser to allow popup windows for {0}."
     *
     * @return translated "We attempted to open an external browser window, but the action was prevented by your popup blocker. You can attempt to open the window again by pressing the "Try Again" button below. NOTE: To prevent seeing this message in the future, you should configure your browser to allow popup windows for {0}."
     */
    @DefaultMessage("We attempted to open an external browser window, but the action was prevented by your popup blocker. You can attempt to open the window again by pressing the \"Try Again\" button below. NOTE: To prevent seeing this message in the future, you should configure your browser to allow popup windows for {0}.")
    String showPopupBlockMessage(String hostName);

    /**
     * Translated "Status code {0} returned by {1} when executing ''{2}''"
     *
     * @return translated "Status code {0} returned by {1} when executing ''{2}''"
     */
    @DefaultMessage("Status code {0} returned by {1} when executing ''{2}''")
    String rpcErrorMessage(String statusCode, String desktop, String method);

    /**
     * Translated "RPC Error"
     *
     * @return translated "RPC Error"
     */
    @DefaultMessage("RPC Error")
    String rpcErrorMessageCaption();

    /**
     * Translated "Unable to establish connection with {0} when executing ''{1}''"
     *
     * @return translated "Unable to establish connection with {0} when executing ''{1}''"
     */
    @DefaultMessage("Unable to establish connection with {0} when executing ''{1}''")
    String rpcOverrideErrorMessage(String desktop, String method);

    /**
     * Translated "Unable to establish connection with {0} when executing ''{1}''"
     *
     * @return translated "Unable to establish connection with {0} when executing ''{1}''"
     */
    @DefaultMessage("Unable to establish connection with the session on {0}. Please try logging in again in a new tab, then return to resume your session.")
    String rpcOverrideErrorMessageServer(String platform);

    /**
     * Translated "Log in"
     *
     * @return translated "Log in"
     */
    @DefaultMessage("Log in")
    String rpcOverrideErrorMessageLink();

    /**
     * Translated "You need to restart RStudio in order for these changes to take effect. Do you want to do this now?"
     *
     * @return translated "You need to restart RStudio in order for these changes to take effect. Do you want to do this now?"
     */
    @DefaultMessage("You need to restart RStudio in order for these changes to take effect. Do you want to do this now?")
    String restartRequiredMessage();

    /**
     * Translated "{0} minimized"
     *
     * @return translated "{0} minimized"
     */
    @DefaultMessage("{0} minimized")
    String minimizedTabListRole(String accessibleName);

    /**
     * Translated "Close"
     *
     * @return translated "Close"
     */
    @DefaultMessage("Close")
    String closeText();

    /**
     * Translated "Close {0} tab"
     *
     * @return translated "Close {0} tab"
     */
    @DefaultMessage("Close {0} tab")
    String closeButtonText(String title);

    /**
     * Translated "Minimize {0}"
     *
     * @return translated "Minimize {0}"
     */
    @DefaultMessage("Minimize {0}")
    String minimizeState(String name);

    /**
     * Translated "Maximize {0}"
     *
     * @return translated "Maximize {0}"
     */
    @DefaultMessage("Maximize {0}")
    String maximizeState(String name);

    /**
     * Translated "Restore {0}"
     *
     * @return translated "Restore {0}"
     */
    @DefaultMessage("Restore {0}")
    String normalState(String name);

    /**
     * Translated "Hide {0}"
     *
     * @return translated "Hide {0}"
     */
    @DefaultMessage("Hide {0}")
    String hideState(String name);

    /**
     * Translated "Exclusive {0}"
     *
     * @return translated "Exclusive {0}"
     */
    @DefaultMessage("Exclusive {0}")
    String exclusiveState(String name);

    /**
     * Translated "Package {0} required but is not installed."
     *
     * @return translated "Package {0} required but is not installed."
     */
    @DefaultMessage("Package {0} required but is not installed.")
    String package1Message(String packages);

    /**
     * Translated "Package {0} and {1} required but are not installed."
     *
     * @return translated "Package {0} and {1} required but are not installed."
     */
    @DefaultMessage("Package {0} and {1} required but are not installed.")
    String packages2Message(String package0, String package1);

    /**
     * Translated "Packages {0}, {1}, and {2} required but are not installed."
     *
     * @return translated "Packages {0}, {1}, and {2} required but are not installed."
     */
    @DefaultMessage("Packages {0}, {1}, and {2} required but are not installed.")
    String packages3Message(String package0, String package1, String package2);

    /**
     * Translated "Packages {0}, {1}, and {2} others required but are not installed."
     *
     * @return translated "Packages {0}, {1}, and {2} others required but are not installed."
     */
    @DefaultMessage("Packages {0}, {1}, and {2} others required but are not installed.")
    String otherPackagesMessage(String package0, String package1, String package2);

    /**
     * Translated "Are you sure you want to reset keyboard shortcuts to their default values? This action cannot be undone."
     *
     * @return translated "Are you sure you want to reset keyboard shortcuts to their default values? This action cannot be undone."
     */
    @DefaultMessage("Are you sure you want to reset keyboard shortcuts to their default values? This action cannot be undone.")
    String resetKeyboardShortcutsMessage();

    /**
     * Translated "{0} must be a valid number."
     *
     * @return translated "{0} must be a valid number."
     */
    @DefaultMessage("{0} must be a valid number.")
    String rStudioGinjectorErrorMessage(String label);

    /**
     * Translated "{0} must be greater than or equal to {1}."
     *
     * @return translated "{0} must be greater than or equal to {1}."
     */
    @DefaultMessage("{0} must be greater than or equal to {1}.")
    String rStudioGinjectorGreaterThanError(String label, int minValue);

    /**
     * Translated "{0} must be less than or equal to {1}."
     *
     * @return translated "{0} must be less than or equal to {1}."
     */
    @DefaultMessage("{0} must be less than or equal to {1}.")
    String rStudioGinjectorLessThanError(String label, int maxValue);

    /**
     * Translated "Operation completed "
     *
     * @return translated "Operation completed "
     */
    @DefaultMessage("Operation completed ")
    String operationCompletedText();

    /**
     * Translated "{0} completed"
     *
     * @return translated "{0} completed"
     */
    @DefaultMessage("{0} completed")
    String completedText(String labelText);

    /**
     * Translated "Clear".
     *
     * @return translated "Clear"
     */
    @DefaultMessage("Clear")
    String clearLabel();

    /**
     * Translated "Browse...".
     *
     * @return translated "Browse..."
     */
    @DefaultMessage("Browse...")
    String fileChooserTextBoxBrowseLabel();

    /**
     * Translated "Choose File".
     *
     * @return translated "Choose File"
     */
    @DefaultMessage("Choose File")
    String chooseFileCaption();

    /**
     * Translated "Ctrl+".
     *
     * @return translated "Ctrl+"
     */
    @DefaultMessage("Ctrl+")
    String keyComboCtrl();

    /**
     * Translated "Alt+".
     *
     * @return translated "Alt+"
     */
    @DefaultMessage("Alt+")
    String keyComboAlt();

    /**
     * Translated "Shift+".
     *
     * @return translated "Shift+"
     */
    @DefaultMessage("Shift+")
    String keyComboShift();

    /**
     * Translated "Cmd+".
     *
     * @return translated "Cmd+"
     */
    @DefaultMessage("Cmd+")
    String keyComboCmd();

    /**
     * Translated "Enter".
     *
     * @return translated "Enter"
     */
    @DefaultMessage("Enter")
    String keyNameEnter();

    /**
     * Translated "Left".
     *
     * @return translated "Left"
     */
    @DefaultMessage("Left")
    String keyNameLeft();

    /**
     * Translated "Right".
     *
     * @return translated "Right"
     */
    @DefaultMessage("Right")
    String keyNameRight();

    /**
     * Translated "Up".
     *
     * @return translated "Up"
     */
    @DefaultMessage("Up")
    String keyNameUp();

    /**
     * Translated "Down".
     *
     * @return translated "Down"
     */
    @DefaultMessage("Down")
    String keyNameDown();

    /**
     * Translated "Tab".
     *
     * @return translated "Tab"
     */
    @DefaultMessage("Tab")
    String keyNameTab();

    /**
     * Translated "PageUp".
     *
     * @return translated "PageUp"
     */
    @DefaultMessage("PageUp")
    String keyNamePageUp();

    /**
     * Translated "PageDown".
     *
     * @return translated "PageDown"
     */
    @DefaultMessage("PageDown")
    String keyNamePageDown();

    /**
     * Translated "Backspace".
     *
     * @return translated "Backspace"
     */
    @DefaultMessage("Backspace")
    String keyNameBackspace();

    /**
     * Translated "Space".
     *
     * @return translated "Space"
     */
    @DefaultMessage("Space")
    String keyNameSpace();
    

}
