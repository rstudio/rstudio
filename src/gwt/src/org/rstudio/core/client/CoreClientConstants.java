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
     */
    @DefaultMessage("Cancel")
    @Key("cancelLabel")
    String cancelLabel();
    
    /**
     * Translated "Reset".
     */
    @DefaultMessage("Reset")
    @Key("resetLabel")
    String resetLabel();

    /**
     * Translated "No".
     */
    @DefaultMessage("No")
    @Key("noLabel")
    String noLabel();

    /**
     * Translated "Yes".
     */
    @DefaultMessage("Yes")
    @Key("yesLabel")
    String yesLabel();

    /**
     * Translated "OK".
     */
    @DefaultMessage("OK")
    @Key("okayLabel")
    String okayLabel();
    
    /**
     * Translated "Copy to Clipboard".
     */
    @DefaultMessage("Copy to Clipboard")
    @Key("copyToClipboardLabel")
    String copyToClipboardLabel();

    /**
     * Translated "Not Yet Implemented".
     */
    @DefaultMessage("Not Yet Implemented")
    @Key("notYetImplementedCaption")
    String notYetImplementedCaption();

    /**
     * Translated "This feature has not yet been implemented.".
     */
    @DefaultMessage("This feature has not yet been implemented.")
    @Key("notYetImplementedMessage")
    String notYetImplementedMessage();

    /**
     * Translated "Popup Blocked".
     */
    @DefaultMessage("Popup Blocked")
    @Key("popupBlockCaption")
    String popupBlockCaption();

    /**
     * Translated "Try Again".
     */
    @DefaultMessage("Try Again")
    @Key("popupBlockTryAgainLabel")
    String popupBlockTryAgainLabel();

    /**
     * Translated "{0} second".
     */
    @DefaultMessage("{0} second")
    @Key("secondLabel")
    String secondLabel(int second);

    /**
     * Translated "{0} seconds".
     */
    @DefaultMessage("{0} seconds")
    @Key("secondPluralLabel")
    String secondPluralLabel(int seconds);

    /**
     * Translated "{0} minute".
     */
    @DefaultMessage("{0} minute")
    @Key("minuteLabel")
    String minuteLabel(int minute);

    /**
     * Translated "{0} minutes".
     */
    @DefaultMessage("{0} minutes")
    @Key("minutePluralLabel")
    String minutePluralLabel(int minutes);

    /**
     * Translated "{0} hour".
     */
    @DefaultMessage("{0} hour")
    @Key("hourLabel")
    String hourLabel(int hour);

    /**
     * Translated "{0} hours".
     */
    @DefaultMessage("{0} hours")
    @Key("hourPluralLabel")
    String hourPluralLabel(int hours);

    /**
     * Translated "Type shortcuts to see if they are bound to a command. Close this message bar when done.".
     */
    @DefaultMessage("Type shortcuts to see if they are bound to a command. Close this message bar when done.")
    @Key("reportShortCutMessage")
    String reportShortCutMessage();

    /**
     * Translated "Multi-gesture shortcut pending".
     */
    @DefaultMessage("Multi-gesture shortcut pending")
    @Key("multiGestureMessage")
    String multiGestureMessage();

    /**
     * Translated "Shortcut not bound".
     */
    @DefaultMessage("Shortcut not bound")
    @Key("shortcutUnBoundMessage")
    String shortcutUnBoundMessage();

    /**
     * Translated "Name is empty".
     */
    @DefaultMessage("Name is empty")
    @Key("nameEmptyMessage")
    String nameEmptyMessage();

    /**
     * Translated "Names should not start or end with spaces".
     */
    @DefaultMessage("Names should not start or end with spaces")
    @Key("nameStartWithMessage")
    String nameStartWithMessage();

    /**
     * Translated "Illegal character: /".
     */
    @DefaultMessage("Illegal character: /")
    @Key("nameIllegalCharacterMessage")
    String nameIllegalCharacterMessage();

    /**
     * Translated "Illegal name".
     */
    @DefaultMessage("Illegal name")
    @Key("illegalNameMessage")
    String illegalNameMessage();

    /**
     * Translated "Folder".
     */
    @DefaultMessage("Folder")
    @Key("fileNameLabel")
    String fileNameLabel();

    /**
     * Translated "File name".
     */
    @DefaultMessage("File name")
    @Key("getFilenameLabel")
    String getFilenameLabel();

    /**
     * Translated "File does not exist".
     */
    @DefaultMessage("File does not exist")
    @Key("nonexistentFileMessage")
    String nonexistentFileMessage();

    /**
     * Translated "Open Project".
     */
    @DefaultMessage("Open Project")
    @Key("openProjectTitle")
    String openProjectTitle();

    /**
     * Translated "Open".
     */
    @DefaultMessage("Open")
    @Key("openButtonTitle")
    String openButtonTitle();

    /**
     * Translated "R Projects (*.RProj)".
     */
    @DefaultMessage("R Projects (*.RProj)")
    @Key("rProjectsFilter")
    String rProjectsFilter();

    /**
     * Translated "Open in new session".
     */
    @DefaultMessage("Open in new session")
    @Key("newSessionCheckLabel")
    String newSessionCheckLabel();

    /**
     * Translated "Create".
     */
    @DefaultMessage("Create")
    @Key("createButtonTitle")
    String createButtonTitle();

    /**
     * Translated "Selected path breadcrumb".
     */
    @DefaultMessage("Selected path breadcrumb")
    @Key("pathBreadCrumbSelectPath")
    String pathBreadCrumbSelectPath();

    /**
     * Translated "Selected path breadcrumb".
     */
    @DefaultMessage("Go to directory")
    @Key("pathBreadCrumbButtonTitle")
    String pathBreadCrumbButtonTitle();

    /**
     * Translated "Go to project directory".
     */
    @DefaultMessage("Go to project directory")
    @Key("projectIconDesc")
    String projectIconDesc();

    /**
     * Translated "Projects".
     */
    @DefaultMessage("Projects")
    @Key("projectsLabel")
    String projectsLabel();
    
    /**
     * Translated "Home".
     */
    @DefaultMessage("Home")
    @Key("anchorHomeText")
    String anchorHomeText();

    /**
     * Translated "Cloud".
     */
    @DefaultMessage("Cloud")
    @Key("cloudHomeText")
    String cloudHomeText();

    /**
     * Translated "Go To Folder".
     */
    @DefaultMessage("Go To Folder")
    @Key("browseFolderCaption")
    String browseFolderCaption();

    /**
     * Translated "Path to folder (use ~ for home directory):".
     */
    @DefaultMessage("Path to folder (use ~ for home directory):")
    @Key("browseFolderLabel")
    String browseFolderLabel();

    /**
     * Translated "Confirm Overwrite".
     */
    @DefaultMessage("Confirm Overwrite")
    @Key("showOverwriteCaption")
    String showOverwriteCaption();

    /**
     * Translated "This file already exists. Do you want to replace it?".
     */
    @DefaultMessage("This file already exists. Do you want to replace it?")
    @Key("showOverwriteMessage")
    String showOverwriteMessage();

    /**
     * Translated "R session".
     */
    @DefaultMessage("R session")
    @Key("rSessionMessage")
    String rSessionMessage();

    /**
     * Translated "RStudio Server".
     */
    @DefaultMessage("RStudio Server")
    @Key("rStudioServerMessage")
    String rStudioServerMessage();

    /**
     * Translated "OK".
     */
    @DefaultMessage("OK")
    @Key("okButtonTitle")
    String okButtonTitle();

    /**
     * Translated "Apply".
     */
    @DefaultMessage("Apply")
    @Key("addButtonTitle")
    String addButtonTitle();

    /**
     * Translated "Saving...".
     */
    @DefaultMessage("Saving...")
    @Key("progressIndicatorTitle")
    String progressIndicatorTitle();

    /**
     * Translated "Restart Required".
     */
    @DefaultMessage("Restart Required")
    @Key("restartRequiredCaption")
    String restartRequiredCaption();

    /**
     * Translated "Working...".
     */
    @DefaultMessage("Working...")
    @Key("promiseWithProgress")
    String promiseWithProgress();

    /**
     * Translated "Error".
     */
    @DefaultMessage("Error")
    @Key("promiseWithProgressError")
    String promiseWithProgressError();

    /**
     * Translated "Documents".
     */
    @DefaultMessage("Documents")
    @Key("documentsTabList")
    String documentsTabList();

    /**
     * Translated "Rename".
     */
    @DefaultMessage("Rename")
    @Key("renameMenuItem")
    String renameMenuItem();

    /**
     * Translated "Copy Path".
     */
    @DefaultMessage("Copy Path")
    @Key("copyPathMenuItem")
    String copyPathMenuItem();

    /**
     * Translated "Set Working Directory".
     */
    @DefaultMessage("Set Working Directory")
    @Key("setWorkingDirMenuItem")
    String setWorkingDirMenuItem();

    /**
     * Translated "Close".
     */
    @DefaultMessage("Close")
    @Key("closeMenuItem")
    String closeMenuItem();

    /**
     * Translated "Close All".
     */
    @DefaultMessage("Close All")
    @Key("closeAllMenuItem")
    String closeAllMenuItem();

    /**
     * Translated "Close All Others".
     */
    @DefaultMessage("Close All Others")
    @Key("closeOthersMenuItem")
    String closeOthersMenuItem();

    /**
     * Translated "Close document tab".
     */
    @DefaultMessage("Close document tab")
    @Key("closeTabText")
    String closeTabText();

    /**
     * Translated "Could Not Change Setting"
     */
    @DefaultMessage("Could Not Change Setting")
    @Key("docPropErrorMessage")
    String docPropErrorMessage();

    /**
     * Translated "Close popup"
     */
    @DefaultMessage("Close popup")
    @Key("closePopupText")
    String closePopupText();

    /**
     * Translated "Please use a complete file path."
     */
    @DefaultMessage("Please use a complete file path.")
    @Key("themeButtonOnErrorMessage")
    String themeButtonOnErrorMessage();

    /**
     * Translated "Please use a complete file path."
     */
    @DefaultMessage("Unexpected empty response from server")
    @Key("onSubmitErrorMessage")
    String onSubmitErrorMessage();

    /**
     * Translated "Install"
     */
    @DefaultMessage("Install")
    @Key("installText")
    String installText();

    /**
     * Translated "Don''t Show Again"
     */
    @DefaultMessage("Don''t Show Again")
    @Key("donnotShowAgain")
    String donnotShowAgain();

    /**
     * Translated "Markdown format changes require a reload of the visual editor."
     */
    @DefaultMessage("Markdown format changes require a reload of the visual editor.")
    @Key("showPanmirrorText")
    String showPanmirrorText();

    /**
     * Translated "Reload Now"
     */
    @DefaultMessage("Reload Now")
    @Key("reloadNowText")
    String reloadNowText();

    /**
     * Translated "Install TinyTeX"
     */
    @DefaultMessage("Install TinyTeX")
    @Key("installTinyTexText")
    String installTinyTexText();

    /**
     * Translated "This document is read only."
     */
    @DefaultMessage("This document is read only.")
    @Key("showReadOnlyWarningText")
    String showReadOnlyWarningText();

    /**
     * Translated "This document is read only. Generated from:"
     */
    @DefaultMessage("This document is read only. Generated from:")
    @Key("showReadOnlyWarningGeneratedText")
    String showReadOnlyWarningGeneratedText();

    /**
     * Translated "Add..."
     */
    @DefaultMessage("Add...")
    @Key("buttonAddCaption")
    String buttonAddCaption();

    /**
     * Translated "Remove"
     */
    @DefaultMessage("Remove")
    @Key("buttonRemoveCaption")
    String buttonRemoveCaption();

    /**
     * Translated "Local repositories:"
     */
    @DefaultMessage("Local repositories:")
    @Key("localReposText")
    String localReposText();

    /**
     * Translated "Help on local Packrat repositories"
     */
    @DefaultMessage("Help on local Packrat repositories")
    @Key("localReposTitle")
    String localReposTitle();

    /**
     * Translated "Add Local Repository"
     */
    @DefaultMessage("Add Local Repository")
    @Key("addLocalRepoText")
    String addLocalRepoText();

    /**
     * Translated "Error"
     */
    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    /**
     * Translated "No bindings available"
     */
    @DefaultMessage("No bindings available")
    @Key("emptyLabel")
    String emptyLabel();

    /**
     * Translated "Keyboard Shortcuts"
     */
    @DefaultMessage("Keyboard Shortcuts")
    @Key("keyboardShortcutsText")
    String keyboardShortcutsText();

    /**
     * Translated "Apply"
     */
    @DefaultMessage("Apply")
    @Key("applyThemeButtonText")
    String applyThemeButtonText();

    /**
     * Translated "All"
     */
    @DefaultMessage("All")
    @Key("radioButtonLabel")
    String radioButtonLabel();

    /**
     * Translated "Customized"
     */
    @DefaultMessage("Customized")
    @Key("radioCustomizedLabel")
    String radioCustomizedLabel();

    /**
     * Translated "Filter keyboard shortcuts"
     */
    @DefaultMessage("Filter keyboard shortcuts")
    @Key("filterWidgetLabel")
    String filterWidgetLabel();

    /**
     * Translated "Filter..."
     */
    @DefaultMessage("Filter...")
    @Key("filterWidgetPlaceholderText")
    String filterWidgetPlaceholderText();

    /**
     * Translated "Reset..."
     */
    @DefaultMessage("Reset...")
    @Key("resetButtonText")
    String resetButtonText();

    /**
     * Translated "Reset Keyboard Shortcuts"
     */
    @DefaultMessage("Reset Keyboard Shortcuts")
    @Key("resetKeyboardShortcutsCaption")
    String resetKeyboardShortcutsCaption();

    /**
     * Translated "Resetting Keyboard Shortcuts..."
     */
    @DefaultMessage("Resetting Keyboard Shortcuts...")
    @Key("resetKeyboardShortcutsProgress")
    String resetKeyboardShortcutsProgress();

    /**
     * Translated "Name"
     */
    @DefaultMessage("Name")
    @Key("nameColumnText")
    String nameColumnText();

    /**
     * Translated "Shortcut"
     */
    @DefaultMessage("Shortcut")
    @Key("editableTextColumn")
    String editableTextColumn();

    /**
     * Translated "Scope"
     */
    @DefaultMessage("Scope")
    @Key("scopeTextColumn")
    String scopeTextColumn();

    /**
     * Translated "Failed to find <input> element in table"
     */
    @DefaultMessage("Failed to find <input> element in table")
    @Key("tagNameErrorMessage")
    String tagNameErrorMessage();

    /**
     * Translated "Show:"
     */
    @DefaultMessage("Show:")
    @Key("radioShowLabel")
    String radioShowLabel();

    /**
     * Translated "Customizing Keyboard Shortcuts"
     */
    @DefaultMessage("Customizing Keyboard Shortcuts")
    @Key("customizeKeyboardHelpLink")
    String customizeKeyboardHelpLink();

    /**
     * Translated "Masked by RStudio command: "
     */
    @DefaultMessage("Masked by RStudio command: ")
    @Key("addMaskedCommandStylesText")
    String addMaskedCommandStylesText();

    /**
     * Translated "Conflicts with command: "
     */
    @DefaultMessage("Conflicts with command: ")
    @Key("addConflictCommandStylesText")
    String addConflictCommandStylesText();

    /**
     * Translated "Refresh Automatically"
     */
    @DefaultMessage("Refresh Automatically")
    @Key("refreshAutomaticallyLabel")
    String refreshAutomaticallyLabel();

    /**
     * Translated "Stop"
     */
    @DefaultMessage("Stop")
    @Key("stopButtonText")
    String stopButtonText();

    /**
     * Translated "Secondary Window"
     */
    @DefaultMessage("Secondary Window")
    @Key("satelliteToolBarText")
    String satelliteToolBarText();

    /**
     * Translated "Clear text"
     */
    @DefaultMessage("Clear text")
    @Key("searchWidgetClearText")
    String searchWidgetClearText();

    /**
     * Translated "(None)"
     */
    @DefaultMessage("(None)")
    @Key("selectWidgetListBoxNone")
    String selectWidgetListBoxNone();

    /**
     * Translated "Keyboard Shortcut Quick Reference"
     */
    @DefaultMessage("Keyboard Shortcut Quick Reference")
    @Key("shortcutHeaderText")
    String shortcutHeaderText();

    /**
     * Translated "Tabs"
     */
    @DefaultMessage("Tabs")
    @Key("tabsGroupName")
    String tabsGroupName();

    /**
     * Translated "Panes"
     */
    @DefaultMessage("Panes")
    @Key("panesGroupName")
    String panesGroupName();

    /**
     * Translated "Files"
     */
    @DefaultMessage("Files")
    @Key("filesGroupName")
    String filesGroupName();

    /**
     * Translated "Main Menu (Server)"
     */
    @DefaultMessage("Main Menu (Server)")
    @Key("mainMenuGroupName")
    String mainMenuGroupName();

    /**
     * Translated "Source Navigation"
     */
    @DefaultMessage("Source Navigation")
    @Key("sourceNavigationGroupName")
    String sourceNavigationGroupName();

    /**
     * Translated "Execute"
     */
    @DefaultMessage("Execute")
    @Key("executeGroupName")
    String executeGroupName();

    /**
     * Translated "Source Editor"
     */
    @DefaultMessage("Source Editor")
    @Key("sourceEditorGroupName")
    String sourceEditorGroupName();

    /**
     * Translated "Debug"
     */
    @DefaultMessage("Debug")
    @Key("debugGroupName")
    String debugGroupName();

    /**
     * Translated "Accessibility"
     */
    @DefaultMessage("Accessibility")
    @Key("accessibilityGroupName")
    String accessibilityGroupName();

    /**
     * Translated "Source Control"
     */
    @DefaultMessage("Source Control")
    @Key("sourceControlGroupName")
    String sourceControlGroupName();

    /**
     * Translated "Build"
     */
    @DefaultMessage("Build")
    @Key("buildGroupName")
    String buildGroupName();

    /**
     * Translated "Console"
     */
    @DefaultMessage("Console")
    @Key("consoleGroupName")
    String consoleGroupName();

    /**
     * Translated "Terminal"
     */
    @DefaultMessage("Terminal")
    @Key("terminalGroupName")
    String terminalGroupName();

    /**
     * Translated "Other"
     */
    @DefaultMessage("Other")
    @Key("otherGroupName")
    String otherGroupName();

    /**
     * Translated "Add Shift to zoom (maximize) pane."
     */
    @DefaultMessage("Add Shift to zoom (maximize) pane.")
    @Key("addShiftPTag")
    String addShiftPTag();

    /**
     * Translated "[Use Default]"
     */
    @DefaultMessage("[Use Default]")
    @Key("useDefaultPrefix")
    String useDefaultPrefix();

    /**
     * Translated "You must enter a value."
     */
    @DefaultMessage("You must enter a value.")
    @Key("validateMessage")
    String validateMessage();

    /**
     * Translated "Not a valid number."
     */
    @DefaultMessage("Not a valid number.")
    @Key("notValidNumberMessage")
    String notValidNumberMessage();

    /**
     * Translated "Vim Keyboard Shortcuts"
     */
    @DefaultMessage("Vim Keyboard Shortcuts")
    @Key("vimKeyboardShortcutsText")
    String vimKeyboardShortcutsText();

    /**
     * Translated "Next"
     */
    @DefaultMessage("Next")
    @Key("nextButtonText")
    String nextButtonText();

    /**
     * Translated "Back"
     */
    @DefaultMessage("Back")
    @Key("backButtonText")
    String backButtonText();

    /**
     * Translated "Info"
     */
    @DefaultMessage("Info")
    @Key("dialogInfoText")
    String dialogInfoText();

    /**
     * Translated "Directory Contents"
     */
    @DefaultMessage("Directory Contents")
    @Key("directoryContentsLabel")
    String directoryContentsLabel();

    /**
     * Translated "New Folder"
     */
    @DefaultMessage("New Folder")
    @Key("newFolderTitle")
    String newFolderTitle();

    /**
     * Translated "Folder name"
     */
    @DefaultMessage("Folder name")
    @Key("folderNameLabel")
    String folderNameLabel();

    /**
     * Translated "Warning"
     */
    @DefaultMessage("Warning")
    @Key("dialogWarningText")
    String dialogWarningText();

    /**
     * Translated "Question"
     */
    @DefaultMessage("Question")
    @Key("dialogQuestionText")
    String dialogQuestionText();

    /**
     * Translated "Popup Blocked"
     */
    @DefaultMessage("Popup Blocked")
    @Key("dialogPopupBlockedText")
    String dialogPopupBlockedText();

    /**
     * Translated "Error"
     */
    @DefaultMessage("Error")
    @Key("dialogErrorText")
    String dialogErrorText();

    /**
     * Translated "Manual Refresh Only"
     */
    @DefaultMessage("Manual Refresh Only")
    @Key("manualRefreshLabel")
    String manualRefreshLabel();

    /**
     * Translated "Busy"
     */
    @DefaultMessage("Busy")
    @Key("busyLabel")
    String busyLabel();

    /**
     * Translated "[REDACTED]"
     */
    @DefaultMessage("[REDACTED]")
    @Key("redactedText")
    String redactedText();

    /**
     * Translated "Vim keyboard shortcut help not screen reader accessible. Press any key to close."
     */
    @DefaultMessage("Vim keyboard shortcut help not screen reader accessible. Press any key to close.")
    @Key("vimKeyboardShortcutHelpMessage")
    String vimKeyboardShortcutHelpMessage();

    /**
     * Translated "We attempted to open an external browser window, but the action was prevented by your popup blocker. You can attempt to open the window again by pressing the "Try Again" button below. NOTE: To prevent seeing this message in the future, you should configure your browser to allow popup windows for {0}."
     */
    @DefaultMessage("We attempted to open an external browser window, but the action was prevented by your popup blocker. You can attempt to open the window again by pressing the \"Try Again\" button below. NOTE: To prevent seeing this message in the future, you should configure your browser to allow popup windows for {0}.")
    @Key("showPopupBlockMessage")
    String showPopupBlockMessage(String hostName);

    /**
     * Translated "Status code {0} returned by {1} when executing ''{2}''"
     */
    @DefaultMessage("Status code {0} returned by {1} when executing ''{2}''")
    @Key("rpcErrorMessage")
    String rpcErrorMessage(String statusCode, String desktop, String method);

    /**
     * Translated "RPC Error"
     */
    @DefaultMessage("RPC Error")
    @Key("rpcErrorMessageCaption")
    String rpcErrorMessageCaption();

    /**
     * Translated "Unable to establish connection with {0} when executing ''{1}''"
     */
    @DefaultMessage("Unable to establish connection with {0} when executing ''{1}''")
    @Key("rpcOverrideErrorMessage")
    String rpcOverrideErrorMessage(String desktop, String method);

    /**
     * Translated "Unable to establish connection with the session on {0}. Please try logging in again in a new tab, then return to resume your session."
     */
    @DefaultMessage("Unable to establish connection with the session on {0}. Please try logging in again in a new tab, then return to resume your session.")
    @Key("rpcOverrideErrorMessageServer")
    String rpcOverrideErrorMessageServer(String platform);

    /**
     * Translated "Log in"
     */
    @DefaultMessage("Log in")
    @Key("rpcOverrideErrorMessageLink")
    String rpcOverrideErrorMessageLink();

    /**
     * Translated "You need to restart RStudio in order for these changes to take effect. Do you want to do this now?"
     */
    @DefaultMessage("You need to restart RStudio in order for these changes to take effect. Do you want to do this now?")
    @Key("restartRequiredMessage")
    String restartRequiredMessage();

    /**
     * Translated "{0} minimized"
     */
    @DefaultMessage("{0} minimized")
    @Key("minimizedTabListRole")
    String minimizedTabListRole(String accessibleName);

    /**
     * Translated "Close"
     */
    @DefaultMessage("Close")
    @Key("closeText")
    String closeText();

    /**
     * Translated "Close {0} tab"
     */
    @DefaultMessage("Close {0} tab")
    @Key("closeButtonText")
    String closeButtonText(String title);

    /**
     * Translated "Minimize {0}"
     */
    @DefaultMessage("Minimize {0}")
    @Key("minimizeState")
    String minimizeState(String name);

    /**
     * Translated "Maximize {0}"
     */
    @DefaultMessage("Maximize {0}")
    @Key("maximizeState")
    String maximizeState(String name);

    /**
     * Translated "Restore {0}"
     */
    @DefaultMessage("Restore {0}")
    @Key("normalState")
    String normalState(String name);

    /**
     * Translated "Hide {0}"
     */
    @DefaultMessage("Hide {0}")
    @Key("hideState")
    String hideState(String name);

    /**
     * Translated "Exclusive {0}"
     */
    @DefaultMessage("Exclusive {0}")
    @Key("exclusiveState")
    String exclusiveState(String name);

    /**
     * Translated "Package {0} required but is not installed."
     */
    @DefaultMessage("Package {0} required but is not installed.")
    @Key("package1Message")
    String package1Message(String packages);

    /**
     * Translated "Packages {0} and {1} required but are not installed."
     */
    @DefaultMessage("Packages {0} and {1} required but are not installed.")
    @Key("packages2Message")
    String packages2Message(String package0, String package1);

    /**
     * Translated "Packages {0}, {1}, and {2} required but are not installed."
     */
    @DefaultMessage("Packages {0}, {1}, and {2} required but are not installed.")
    @Key("packages3Message")
    String packages3Message(String package0, String package1, String package2);

    /**
     * Translated "Packages {0}, {1}, and {2} others required but are not installed."
     */
    @DefaultMessage("Packages {0}, {1}, and {2} others required but are not installed.")
    @Key("otherPackagesMessage")
    String otherPackagesMessage(String package0, String package1, String package2);

    /**
     * Translated "Are you sure you want to reset keyboard shortcuts to their default values? This action cannot be undone."
     */
    @DefaultMessage("Are you sure you want to reset keyboard shortcuts to their default values? This action cannot be undone.")
    @Key("resetKeyboardShortcutsMessage")
    String resetKeyboardShortcutsMessage();

    /**
     * Translated "{0} must be a valid number."
     */
    @DefaultMessage("{0} must be a valid number.")
    @Key("rStudioGinjectorErrorMessage")
    String rStudioGinjectorErrorMessage(String label);

    /**
     * Translated "{0} must be greater than or equal to {1}."
     */
    @DefaultMessage("{0} must be greater than or equal to {1}.")
    @Key("rStudioGinjectorGreaterThanError")
    String rStudioGinjectorGreaterThanError(String label, int minValue);

    /**
     * Translated "{0} must be less than or equal to {1}."
     */
    @DefaultMessage("{0} must be less than or equal to {1}.")
    @Key("rStudioGinjectorLessThanError")
    String rStudioGinjectorLessThanError(String label, int maxValue);

    /**
     * Translated "Operation completed "
     */
    @DefaultMessage("Operation completed ")
    @Key("operationCompletedText")
    String operationCompletedText();

    /**
     * Translated "{0} completed"
     */
    @DefaultMessage("{0} completed")
    @Key("completedText")
    String completedText(String labelText);

    /**
     * Translated "Clear".
     */
    @DefaultMessage("Clear")
    @Key("clearLabel")
    String clearLabel();

    /**
     * Translated "Browse...".
     */
    @DefaultMessage("Browse...")
    @Key("fileChooserTextBoxBrowseLabel")
    String fileChooserTextBoxBrowseLabel();

    /**
     * Translated "Choose File".
     */
    @DefaultMessage("Choose File")
    @Key("chooseFileCaption")
    String chooseFileCaption();

    /**
     * Translated "Ctrl+".
     */
    @DefaultMessage("Ctrl+")
    @Key("keyComboCtrl")
    String keyComboCtrl();

    /**
     * Translated "Alt+".
     */
    @DefaultMessage("Alt+")
    @Key("keyComboAlt")
    String keyComboAlt();

    /**
     * Translated "Shift+".
     */
    @DefaultMessage("Shift+")
    @Key("keyComboShift")
    String keyComboShift();

    /**
     * Translated "Cmd+".
     */
    @DefaultMessage("Cmd+")
    @Key("keyComboCmd")
    String keyComboCmd();

    /**
     * Translated "Enter".
     */
    @DefaultMessage("Enter")
    @Key("keyNameEnter")
    String keyNameEnter();

    /**
     * Translated "Left".
     */
    @DefaultMessage("Left")
    @Key("keyNameLeft")
    String keyNameLeft();

    /**
     * Translated "Right".
     */
    @DefaultMessage("Right")
    @Key("keyNameRight")
    String keyNameRight();

    /**
     * Translated "Up".
     */
    @DefaultMessage("Up")
    @Key("keyNameUp")
    String keyNameUp();

    /**
     * Translated "Down".
     */
    @DefaultMessage("Down")
    @Key("keyNameDown")
    String keyNameDown();

    /**
     * Translated "Tab".
     */
    @DefaultMessage("Tab")
    @Key("keyNameTab")
    String keyNameTab();

    /**
     * Translated "PageUp".
     */
    @DefaultMessage("PageUp")
    @Key("keyNamePageUp")
    String keyNamePageUp();

    /**
     * Translated "PageDown".
     */
    @DefaultMessage("PageDown")
    @Key("keyNamePageDown")
    String keyNamePageDown();

    /**
     * Translated "Backspace".
     */
    @DefaultMessage("Backspace")
    @Key("keyNameBackspace")
    String keyNameBackspace();

    /**
     * Translated "Space".
     */
    @DefaultMessage("Space")
    @Key("keyNameSpace")
    String keyNameSpace();

}
