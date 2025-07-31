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

    @DefaultMessage("Cancel")
    @Key("cancelLabel")
    String cancelLabel();
    

    @DefaultMessage("Reset")
    @Key("resetLabel")
    String resetLabel();

    @DefaultMessage("No")
    @Key("noLabel")
    String noLabel();

    @DefaultMessage("Yes")
    @Key("yesLabel")
    String yesLabel();

    @DefaultMessage("OK")
    @Key("okayLabel")
    String okayLabel();
    

    @DefaultMessage("Copy to Clipboard")
    @Key("copyToClipboardLabel")
    String copyToClipboardLabel();

    @DefaultMessage("Not Yet Implemented")
    @Key("notYetImplementedCaption")
    String notYetImplementedCaption();

    @DefaultMessage("This feature has not yet been implemented.")
    @Key("notYetImplementedMessage")
    String notYetImplementedMessage();

    @DefaultMessage("Popup Blocked")
    @Key("popupBlockCaption")
    String popupBlockCaption();

    @DefaultMessage("Try Again")
    @Key("popupBlockTryAgainLabel")
    String popupBlockTryAgainLabel();

    @DefaultMessage("{0} second")
    @Key("secondLabel")
    String secondLabel(int second);

    @DefaultMessage("{0} seconds")
    @Key("secondPluralLabel")
    String secondPluralLabel(int seconds);

    @DefaultMessage("{0} minute")
    @Key("minuteLabel")
    String minuteLabel(int minute);

    @DefaultMessage("{0} minutes")
    @Key("minutePluralLabel")
    String minutePluralLabel(int minutes);

    @DefaultMessage("{0} hour")
    @Key("hourLabel")
    String hourLabel(int hour);

    @DefaultMessage("{0} hours")
    @Key("hourPluralLabel")
    String hourPluralLabel(int hours);

    @DefaultMessage("Type shortcuts to see if they are bound to a command. Close this message bar when done.")
    @Key("reportShortCutMessage")
    String reportShortCutMessage();

    @DefaultMessage("Multi-gesture shortcut pending")
    @Key("multiGestureMessage")
    String multiGestureMessage();

    @DefaultMessage("Shortcut not bound")
    @Key("shortcutUnBoundMessage")
    String shortcutUnBoundMessage();

    @DefaultMessage("Name is empty")
    @Key("nameEmptyMessage")
    String nameEmptyMessage();

    @DefaultMessage("Names should not start or end with spaces")
    @Key("nameStartWithMessage")
    String nameStartWithMessage();

    @DefaultMessage("Illegal character: /")
    @Key("nameIllegalCharacterMessage")
    String nameIllegalCharacterMessage();

    @DefaultMessage("Illegal name")
    @Key("illegalNameMessage")
    String illegalNameMessage();

    @DefaultMessage("Folder")
    @Key("fileNameLabel")
    String fileNameLabel();

    @DefaultMessage("File name")
    @Key("getFilenameLabel")
    String getFilenameLabel();

    @DefaultMessage("File does not exist")
    @Key("nonexistentFileMessage")
    String nonexistentFileMessage();

    @DefaultMessage("Open Project")
    @Key("openProjectTitle")
    String openProjectTitle();

    @DefaultMessage("Open")
    @Key("openButtonTitle")
    String openButtonTitle();

    @DefaultMessage("R Projects (*.RProj)")
    @Key("rProjectsFilter")
    String rProjectsFilter();

    @DefaultMessage("Open in new session")
    @Key("newSessionCheckLabel")
    String newSessionCheckLabel();

    @DefaultMessage("Create")
    @Key("createButtonTitle")
    String createButtonTitle();

    @DefaultMessage("Selected path breadcrumb")
    @Key("pathBreadCrumbSelectPath")
    String pathBreadCrumbSelectPath();

    @DefaultMessage("Go to directory")
    @Key("pathBreadCrumbButtonTitle")
    String pathBreadCrumbButtonTitle();

    @DefaultMessage("Go to project directory")
    @Key("projectIconDesc")
    String projectIconDesc();

    @DefaultMessage("Projects")
    @Key("projectsLabel")
    String projectsLabel();
    

    @DefaultMessage("Home")
    @Key("anchorHomeText")
    String anchorHomeText();

    @DefaultMessage("Cloud")
    @Key("cloudHomeText")
    String cloudHomeText();

    @DefaultMessage("Go To Folder")
    @Key("browseFolderCaption")
    String browseFolderCaption();

    @DefaultMessage("Path to folder (use ~ for home directory):")
    @Key("browseFolderLabel")
    String browseFolderLabel();

    @DefaultMessage("Confirm Overwrite")
    @Key("showOverwriteCaption")
    String showOverwriteCaption();

    @DefaultMessage("This file already exists. Do you want to replace it?")
    @Key("showOverwriteMessage")
    String showOverwriteMessage();

    @DefaultMessage("R session")
    @Key("rSessionMessage")
    String rSessionMessage();

    @DefaultMessage("RStudio Server")
    @Key("rStudioServerMessage")
    String rStudioServerMessage();

    @DefaultMessage("OK")
    @Key("okButtonTitle")
    String okButtonTitle();

    @DefaultMessage("Apply")
    @Key("addButtonTitle")
    String addButtonTitle();

    @DefaultMessage("Saving...")
    @Key("progressIndicatorTitle")
    String progressIndicatorTitle();

    @DefaultMessage("Restart Required")
    @Key("restartRequiredCaption")
    String restartRequiredCaption();

    @DefaultMessage("Working...")
    @Key("promiseWithProgress")
    String promiseWithProgress();

    @DefaultMessage("Error")
    @Key("promiseWithProgressError")
    String promiseWithProgressError();

    @DefaultMessage("Documents")
    @Key("documentsTabList")
    String documentsTabList();

    @DefaultMessage("Rename")
    @Key("renameMenuItem")
    String renameMenuItem();

    @DefaultMessage("Copy Path")
    @Key("copyPathMenuItem")
    String copyPathMenuItem();

    @DefaultMessage("Set Working Directory")
    @Key("setWorkingDirMenuItem")
    String setWorkingDirMenuItem();

    @DefaultMessage("Close")
    @Key("closeMenuItem")
    String closeMenuItem();

    @DefaultMessage("Close All")
    @Key("closeAllMenuItem")
    String closeAllMenuItem();

    @DefaultMessage("Close All Others")
    @Key("closeOthersMenuItem")
    String closeOthersMenuItem();

    @DefaultMessage("Close document tab")
    @Key("closeTabText")
    String closeTabText();

    @DefaultMessage("Could Not Change Setting")
    @Key("docPropErrorMessage")
    String docPropErrorMessage();

    @DefaultMessage("Close popup")
    @Key("closePopupText")
    String closePopupText();

    @DefaultMessage("Please use a complete file path.")
    @Key("themeButtonOnErrorMessage")
    String themeButtonOnErrorMessage();

    @DefaultMessage("Unexpected empty response from server")
    @Key("onSubmitErrorMessage")
    String onSubmitErrorMessage();

    @DefaultMessage("Install")
    @Key("installText")
    String installText();

    @DefaultMessage("Don''t Show Again")
    @Key("donnotShowAgain")
    String donnotShowAgain();

    @DefaultMessage("Markdown format changes require a reload of the visual editor.")
    @Key("showPanmirrorText")
    String showPanmirrorText();

    @DefaultMessage("Reload Now")
    @Key("reloadNowText")
    String reloadNowText();

    @DefaultMessage("Install TinyTeX")
    @Key("installTinyTexText")
    String installTinyTexText();

    @DefaultMessage("This document is read only.")
    @Key("showReadOnlyWarningText")
    String showReadOnlyWarningText();

    @DefaultMessage("This document is read only. Generated from:")
    @Key("showReadOnlyWarningGeneratedText")
    String showReadOnlyWarningGeneratedText();

    @DefaultMessage("Add")
    @Key("buttonAddCaption")
    String buttonAddCaption();

    @DefaultMessage("Remove")
    @Key("buttonRemoveCaption")
    String buttonRemoveCaption();

    @DefaultMessage("Local repositories:")
    @Key("localReposText")
    String localReposText();

    @DefaultMessage("Help on local Packrat repositories")
    @Key("localReposTitle")
    String localReposTitle();

    @DefaultMessage("Add Local Repository")
    @Key("addLocalRepoText")
    String addLocalRepoText();

    @DefaultMessage("Error")
    @Key("errorCaption")
    String errorCaption();

    @DefaultMessage("No bindings available")
    @Key("emptyLabel")
    String emptyLabel();

    @DefaultMessage("Keyboard Shortcuts")
    @Key("keyboardShortcutsText")
    String keyboardShortcutsText();

    @DefaultMessage("Apply")
    @Key("applyThemeButtonText")
    String applyThemeButtonText();

    @DefaultMessage("All")
    @Key("radioButtonLabel")
    String radioButtonLabel();

    @DefaultMessage("Customized")
    @Key("radioCustomizedLabel")
    String radioCustomizedLabel();

    @DefaultMessage("Filter keyboard shortcuts")
    @Key("filterWidgetLabel")
    String filterWidgetLabel();

    @DefaultMessage("Filter...")
    @Key("filterWidgetPlaceholderText")
    String filterWidgetPlaceholderText();

    @DefaultMessage("Reset...")
    @Key("resetButtonText")
    String resetButtonText();

    @DefaultMessage("Reset Keyboard Shortcuts")
    @Key("resetKeyboardShortcutsCaption")
    String resetKeyboardShortcutsCaption();

    @DefaultMessage("Resetting Keyboard Shortcuts...")
    @Key("resetKeyboardShortcutsProgress")
    String resetKeyboardShortcutsProgress();

    @DefaultMessage("Name")
    @Key("nameColumnText")
    String nameColumnText();

    @DefaultMessage("Shortcut")
    @Key("editableTextColumn")
    String editableTextColumn();

    @DefaultMessage("Scope")
    @Key("scopeTextColumn")
    String scopeTextColumn();

    @DefaultMessage("Failed to find <input> element in table")
    @Key("tagNameErrorMessage")
    String tagNameErrorMessage();

    @DefaultMessage("Show:")
    @Key("radioShowLabel")
    String radioShowLabel();

    @DefaultMessage("Customizing Keyboard Shortcuts")
    @Key("customizeKeyboardHelpLink")
    String customizeKeyboardHelpLink();

    @DefaultMessage("Masked by RStudio command: ")
    @Key("addMaskedCommandStylesText")
    String addMaskedCommandStylesText();

    @DefaultMessage("Conflicts with command: ")
    @Key("addConflictCommandStylesText")
    String addConflictCommandStylesText();

    @DefaultMessage("Refresh Automatically")
    @Key("refreshAutomaticallyLabel")
    String refreshAutomaticallyLabel();

    @DefaultMessage("Stop")
    @Key("stopButtonText")
    String stopButtonText();

    @DefaultMessage("Secondary Window")
    @Key("satelliteToolBarText")
    String satelliteToolBarText();

    @DefaultMessage("Clear text")
    @Key("searchWidgetClearText")
    String searchWidgetClearText();

    @DefaultMessage("(None)")
    @Key("selectWidgetListBoxNone")
    String selectWidgetListBoxNone();

    @DefaultMessage("Keyboard Shortcut Quick Reference")
    @Key("shortcutHeaderText")
    String shortcutHeaderText();

    @DefaultMessage("Tabs")
    @Key("tabsGroupName")
    String tabsGroupName();

    @DefaultMessage("Panes")
    @Key("panesGroupName")
    String panesGroupName();

    @DefaultMessage("Files")
    @Key("filesGroupName")
    String filesGroupName();

    @DefaultMessage("Main Menu (Server)")
    @Key("mainMenuGroupName")
    String mainMenuGroupName();

    @DefaultMessage("Source Navigation")
    @Key("sourceNavigationGroupName")
    String sourceNavigationGroupName();

    @DefaultMessage("Execute")
    @Key("executeGroupName")
    String executeGroupName();

    @DefaultMessage("Source Editor")
    @Key("sourceEditorGroupName")
    String sourceEditorGroupName();

    @DefaultMessage("Debug")
    @Key("debugGroupName")
    String debugGroupName();

    @DefaultMessage("Accessibility")
    @Key("accessibilityGroupName")
    String accessibilityGroupName();

    @DefaultMessage("Source Control")
    @Key("sourceControlGroupName")
    String sourceControlGroupName();

    @DefaultMessage("Build")
    @Key("buildGroupName")
    String buildGroupName();

    @DefaultMessage("Console")
    @Key("consoleGroupName")
    String consoleGroupName();

    @DefaultMessage("Terminal")
    @Key("terminalGroupName")
    String terminalGroupName();

    @DefaultMessage("Other")
    @Key("otherGroupName")
    String otherGroupName();

    @DefaultMessage("Add Shift to zoom (maximize) pane.")
    @Key("addShiftPTag")
    String addShiftPTag();

    @DefaultMessage("[Use Default]")
    @Key("useDefaultPrefix")
    String useDefaultPrefix();

    @DefaultMessage("You must enter a value.")
    @Key("validateMessage")
    String validateMessage();

    @DefaultMessage("Not a valid number.")
    @Key("notValidNumberMessage")
    String notValidNumberMessage();

    @DefaultMessage("Vim Keyboard Shortcuts")
    @Key("vimKeyboardShortcutsText")
    String vimKeyboardShortcutsText();

    @DefaultMessage("Next")
    @Key("nextButtonText")
    String nextButtonText();

    @DefaultMessage("Back")
    @Key("backButtonText")
    String backButtonText();

    @DefaultMessage("Info")
    @Key("dialogInfoText")
    String dialogInfoText();

    @DefaultMessage("Directory Contents")
    @Key("directoryContentsLabel")
    String directoryContentsLabel();

    @DefaultMessage("New Folder")
    @Key("newFolderTitle")
    String newFolderTitle();

    @DefaultMessage("Folder name")
    @Key("folderNameLabel")
    String folderNameLabel();

    @DefaultMessage("Warning")
    @Key("dialogWarningText")
    String dialogWarningText();

    @DefaultMessage("Question")
    @Key("dialogQuestionText")
    String dialogQuestionText();

    @DefaultMessage("Popup Blocked")
    @Key("dialogPopupBlockedText")
    String dialogPopupBlockedText();

    @DefaultMessage("Error")
    @Key("dialogErrorText")
    String dialogErrorText();

    @DefaultMessage("Manual Refresh Only")
    @Key("manualRefreshLabel")
    String manualRefreshLabel();

    @DefaultMessage("Busy")
    @Key("busyLabel")
    String busyLabel();

    @DefaultMessage("[REDACTED]")
    @Key("redactedText")
    String redactedText();

    @DefaultMessage("Vim keyboard shortcut help not screen reader accessible. Press any key to close.")
    @Key("vimKeyboardShortcutHelpMessage")
    String vimKeyboardShortcutHelpMessage();

    @DefaultMessage("We attempted to open an external browser window, but the action was prevented by your popup blocker. You can attempt to open the window again by pressing the \"Try Again\" button below. NOTE: To prevent seeing this message in the future, you should configure your browser to allow popup windows for {0}.")
    @Key("showPopupBlockMessage")
    String showPopupBlockMessage(String hostName);

    @DefaultMessage("Status code {0} returned by {1} when executing ''{2}''")
    @Key("rpcErrorMessage")
    String rpcErrorMessage(String statusCode, String desktop, String method);

    @DefaultMessage("RPC Error")
    @Key("rpcErrorMessageCaption")
    String rpcErrorMessageCaption();

    @DefaultMessage("Unable to establish connection with {0} when executing ''{1}''")
    @Key("rpcOverrideErrorMessage")
    String rpcOverrideErrorMessage(String desktop, String method);

    @DefaultMessage("Unable to establish connection with the session on {0}. Please try logging in again in a new tab, then return to resume your session.")
    @Key("rpcOverrideErrorMessageServer")
    String rpcOverrideErrorMessageServer(String platform);

    @DefaultMessage("Log in")
    @Key("rpcOverrideErrorMessageLink")
    String rpcOverrideErrorMessageLink();

    @DefaultMessage("You need to restart RStudio in order for these changes to take effect. Do you want to do this now?")
    @Key("restartRequiredMessage")
    String restartRequiredMessage();

    @DefaultMessage("{0} minimized")
    @Key("minimizedTabListRole")
    String minimizedTabListRole(String accessibleName);

    @DefaultMessage("Close")
    @Key("closeText")
    String closeText();

    @DefaultMessage("Close {0} tab")
    @Key("closeButtonText")
    String closeButtonText(String title);

    @DefaultMessage("Minimize {0}")
    @Key("minimizeState")
    String minimizeState(String name);

    @DefaultMessage("Maximize {0}")
    @Key("maximizeState")
    String maximizeState(String name);

    @DefaultMessage("Restore {0}")
    @Key("normalState")
    String normalState(String name);

    @DefaultMessage("Hide {0}")
    @Key("hideState")
    String hideState(String name);

    @DefaultMessage("Exclusive {0}")
    @Key("exclusiveState")
    String exclusiveState(String name);

    @DefaultMessage("Package {0} required but is not installed.")
    @Key("package1Message")
    String package1Message(String packages);

    @DefaultMessage("Package {0} and {1} required but are not installed.")
    @Key("packages2Message")
    String packages2Message(String package0, String package1);

    @DefaultMessage("Packages {0}, {1}, and {2} required but are not installed.")
    @Key("packages3Message")
    String packages3Message(String package0, String package1, String package2);

    @DefaultMessage("Packages {0}, {1}, and {2} others required but are not installed.")
    @Key("otherPackagesMessage")
    String otherPackagesMessage(String package0, String package1, String package2);

    @DefaultMessage("Are you sure you want to reset keyboard shortcuts to their default values? This action cannot be undone.")
    @Key("resetKeyboardShortcutsMessage")
    String resetKeyboardShortcutsMessage();

    @DefaultMessage("{0} must be a valid number.")
    @Key("rStudioGinjectorErrorMessage")
    String rStudioGinjectorErrorMessage(String label);

    @DefaultMessage("{0} must be greater than or equal to {1}.")
    @Key("rStudioGinjectorGreaterThanError")
    String rStudioGinjectorGreaterThanError(String label, int minValue);

    @DefaultMessage("{0} must be less than or equal to {1}.")
    @Key("rStudioGinjectorLessThanError")
    String rStudioGinjectorLessThanError(String label, int maxValue);

    @DefaultMessage("Operation completed ")
    @Key("operationCompletedText")
    String operationCompletedText();

    @DefaultMessage("{0} completed")
    @Key("completedText")
    String completedText(String labelText);

    @DefaultMessage("Clear")
    @Key("clearLabel")
    String clearLabel();

    @DefaultMessage("Browse...")
    @Key("fileChooserTextBoxBrowseLabel")
    String fileChooserTextBoxBrowseLabel();

    @DefaultMessage("Choose File")
    @Key("chooseFileCaption")
    String chooseFileCaption();

    @DefaultMessage("Ctrl+")
    @Key("keyComboCtrl")
    String keyComboCtrl();

    @DefaultMessage("Alt+")
    @Key("keyComboAlt")
    String keyComboAlt();

    @DefaultMessage("Shift+")
    @Key("keyComboShift")
    String keyComboShift();

    @DefaultMessage("Cmd+")
    @Key("keyComboCmd")
    String keyComboCmd();

    @DefaultMessage("Enter")
    @Key("keyNameEnter")
    String keyNameEnter();

    @DefaultMessage("Left")
    @Key("keyNameLeft")
    String keyNameLeft();

    @DefaultMessage("Right")
    @Key("keyNameRight")
    String keyNameRight();

    @DefaultMessage("Up")
    @Key("keyNameUp")
    String keyNameUp();

    @DefaultMessage("Down")
    @Key("keyNameDown")
    String keyNameDown();

    @DefaultMessage("Tab")
    @Key("keyNameTab")
    String keyNameTab();

    @DefaultMessage("PageUp")
    @Key("keyNamePageUp")
    String keyNamePageUp();

    @DefaultMessage("PageDown")
    @Key("keyNamePageDown")
    String keyNamePageDown();

    @DefaultMessage("Backspace")
    @Key("keyNameBackspace")
    String keyNameBackspace();

    @DefaultMessage("Space")
    @Key("keyNameSpace")
    String keyNameSpace();
    

}
