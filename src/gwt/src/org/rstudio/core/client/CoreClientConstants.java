/*
 * CoreClientConstants.java
 *
 * Copyright (C) 2021 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client;

public interface CoreClientConstants extends com.google.gwt.i18n.client.Constants {

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    @DefaultStringValue("Cancel")
    @Key("cancelLabel")
    String cancelLabel();

    /**
     * Translated "No".
     *
     * @return translated "No"
     */
    @DefaultStringValue("No")
    @Key("noLabel")
    String noLabel();

    /**
     * Translated "Yes".
     *
     * @return translated "Yes"
     */
    @DefaultStringValue("Yes")
    @Key("yesLabel")
    String yesLabel();

    /**
     * Translated "OK".
     *
     * @return translated "OK"
     */
    @DefaultStringValue("OK")
    @Key("okayLabel")
    String okayLabel();

    /**
     * Translated "Not Yet Implemented".
     *
     * @return translated "Not Yet Implemented"
     */
    @DefaultStringValue("Not Yet Implemented")
    @Key("notYetImplementedCaption")
    String notYetImplementedCaption();

    /**
     * Translated "This feature has not yet been implemented.".
     *
     * @return translated "This feature has not yet been implemented."
     */
    @DefaultStringValue("This feature has not yet been implemented.")
    @Key("notYetImplementedMessage")
    String notYetImplementedMessage();

    /**
     * Translated "Popup Blocked".
     *
     * @return translated "Popup Blocked"
     */
    @DefaultStringValue("Popup Blocked")
    @Key("popupBlockCaption")
    String popupBlockCaption();

    /**
     * Translated "Try Again".
     *
     * @return translated "Try Again"
     */
    @DefaultStringValue("Try Again")
    @Key("popupBlockTryAgainLabel")
    String popupBlockTryAgainLabel();

    /**
     * Translated "second".
     *
     * @return translated "second"
     */
    @DefaultStringValue("second")
    @Key("secondLabel")
    String secondLabel();

    /**
     * Translated "seconds".
     *
     * @return translated "seconds"
     */
    @DefaultStringValue("seconds")
    @Key("secondPluralLabel")
    String secondPluralLabel();

    /**
     * Translated "minute".
     *
     * @return translated "minute"
     */
    @DefaultStringValue("minute")
    @Key("minuteLabel")
    String minuteLabel();

    /**
     * Translated "minutes".
     *
     * @return translated "minutes"
     */
    @DefaultStringValue("minutes")
    @Key("minutePluralLabel")
    String minutePluralLabel();

    /**
     * Translated "hour".
     *
     * @return translated "hour"
     */
    @DefaultStringValue("hour")
    @Key("hourLabel")
    String hourLabel();

    /**
     * Translated "hours".
     *
     * @return translated "hours"
     */
    @DefaultStringValue("hours")
    @Key("hourPluralLabel")
    String hourPluralLabel();

    /**
     * Translated "Type shortcuts to see if they are bound to a command. Close this message bar when done.".
     *
     * @return translated "Type shortcuts to see if they are bound to a command. Close this message bar when done."
     */
    @DefaultStringValue("Type shortcuts to see if they are bound to a command. Close this message bar when done.")
    @Key("reportShortCutMessage")
    String reportShortCutMessage();

    /**
     * Translated "Multi-gesture shortcut pending".
     *
     * @return translated "Multi-gesture shortcut pending"
     */
    @DefaultStringValue("Multi-gesture shortcut pending")
    @Key("multiGestureMessage")
    String multiGestureMessage();

    /**
     * Translated "Shortcut not bound".
     *
     * @return translated "Shortcut not bound"
     */
    @DefaultStringValue("Shortcut not bound")
    @Key("shortcutUnBoundMessage")
    String shortcutUnBoundMessage();

    /**
     * Translated "Name is empty".
     *
     * @return translated "Name is empty"
     */
    @DefaultStringValue("Name is empty")
    @Key("nameEmptyMessage")
    String nameEmptyMessage();

    /**
     * Translated "Names should not start or end with spaces".
     *
     * @return translated "Names should not start or end with spaces"
     */
    @DefaultStringValue("Names should not start or end with spaces")
    @Key("nameStartWithMessage")
    String nameStartWithMessage();

    /**
     * Translated "Illegal character: /".
     *
     * @return translated "Illegal character: /"
     */
    @DefaultStringValue("Illegal character: /")
    @Key("nameIllegalCharacterMessage")
    String nameIllegalCharacterMessage();

    /**
     * Translated "Illegal name".
     *
     * @return translated "Illegal name"
     */
    @DefaultStringValue("Illegal name")
    @Key("illegalNameMessage")
    String illegalNameMessage();

    /**
     * Translated "Folder".
     *
     * @return translated "Folder"
     */
    @DefaultStringValue("Folder")
    @Key("fileNameLabel")
    String fileNameLabel();

    /**
     * Translated "File name".
     *
     * @return translated "File name"
     */
    @DefaultStringValue("File name")
    @Key("getFilenameLabel")
    String getFilenameLabel();

    /**
     * Translated "File does not exist".
     *
     * @return translated "File does not exist"
     */
    @DefaultStringValue("File does not exist")
    @Key("nonexistentFileMessage")
    String nonexistentFileMessage();

    /**
     * Translated "Open Project".
     *
     * @return translated "Open Project"
     */
    @DefaultStringValue("Open Project")
    @Key("openProjectTitle")
    String openProjectTitle();

    /**
     * Translated "Open".
     *
     * @return translated "Open"
     */
    @DefaultStringValue("Open")
    @Key("openButtonTitle")
    String openButtonTitle();

    /**
     * Translated "R Projects (*.RProj)".
     *
     * @return translated "R Projects (*.RProj)"
     */
    @DefaultStringValue("R Projects (*.RProj)")
    @Key("rProjectsFilter")
    String rProjectsFilter();

    /**
     * Translated "Open in new session".
     *
     * @return translated "Open in new session"
     */
    @DefaultStringValue("Open in new session")
    @Key("newSessionCheckLabel")
    String newSessionCheckLabel();

    /**
     * Translated "Create".
     *
     * @return translated "Create"
     */
    @DefaultStringValue("Create")
    @Key("createButtonTitle")
    String createButtonTitle();

    /**
     * Translated "Selected path breadcrumb".
     *
     * @return translated "Selected path breadcrumb"
     */
    @DefaultStringValue("Selected path breadcrumb")
    @Key("pathBreadCrumbSelectPath")
    String pathBreadCrumbSelectPath();

    /**
     * Translated "Selected path breadcrumb".
     *
     * @return translated "Go to directory"
     */
    @DefaultStringValue("Go to directory")
    @Key("pathBreadCrumbButtonTitle")
    String pathBreadCrumbButtonTitle();

    /**
     * Translated "Go to project directory".
     *
     * @return translated "Go to project directory"
     */
    @DefaultStringValue("Go to project directory")
    @Key("projectIconDesc")
    String projectIconDesc();

    /**
     * Translated "Home".
     *
     * @return translated "Home"
     */
    @DefaultStringValue("Home")
    @Key("anchorHomeText")
    String anchorHomeText();

    /**
     * Translated "Cloud".
     *
     * @return translated "Cloud"
     */
    @DefaultStringValue("Cloud")
    @Key("cloudHomeText")
    String cloudHomeText();

    /**
     * Translated "Go To Folder".
     *
     * @return translated "Go To Folder"
     */
    @DefaultStringValue("Go To Folder")
    @Key("browseFolderCaption")
    String browseFolderCaption();

    /**
     * Translated "Path to folder (use ~ for home directory):".
     *
     * @return translated "Path to folder (use ~ for home directory):"
     */
    @DefaultStringValue("Path to folder (use ~ for home directory):")
    @Key("browseFolderLabel")
    String browseFolderLabel();

    /**
     * Translated "Confirm Overwrite".
     *
     * @return translated "Confirm Overwrite"
     */
    @DefaultStringValue("Confirm Overwrite")
    @Key("showOverwriteCaption")
    String showOverwriteCaption();

    /**
     * Translated "This file already exists. Do you want to replace it?".
     *
     * @return translated "This file already exists. Do you want to replace it?"
     */
    @DefaultStringValue("This file already exists. Do you want to replace it?")
    @Key("showOverwriteMessage")
    String showOverwriteMessage();


    /**
     * Translated "R session".
     *
     * @return translated "R session"
     */
    @DefaultStringValue("R session")
    @Key("rSessionMessage")
    String rSessionMessage();

    /**
     * Translated "RStudio Server".
     *
     * @return translated "RStudio Server"
     */
    @DefaultStringValue("RStudio Server")
    @Key("rStudioServerMessage")
    String rStudioServerMessage();

    /**
     * Translated "Unable to establish connection with ".
     *
     * @return translated "Unable to establish connection with "
     */
    @DefaultStringValue("Unable to establish connection with ")
    @Key("statusCodeMessage")
    String statusCodeMessage();

    /**
     * Translated "OK".
     *
     * @return translated "OK"
     */
    @DefaultStringValue("OK")
    @Key("okButtonTitle")
    String okButtonTitle();

    /**
     * Translated "Apply".
     *
     * @return translated "Apply"
     */
    @DefaultStringValue("Apply")
    @Key("addButtonTitle")
    String addButtonTitle();

    /**
     * Translated "Saving...".
     *
     * @return translated "Saving..."
     */
    @DefaultStringValue("Saving...")
    @Key("progressIndicatorTitle")
    String progressIndicatorTitle();

    /**
     * Translated "Restart Required".
     *
     * @return translated "Restart Required"
     */
    @DefaultStringValue("Restart Required")
    @Key("restartRequiredCaption")
    String restartRequiredCaption();

    /**
     * Translated "Working...".
     *
     * @return translated "Working..."
     */
    @DefaultStringValue("Working...")
    @Key("promiseWithProgress")
    String promiseWithProgress();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultStringValue("Error")
    @Key("promiseWithProgressError")
    String promiseWithProgressError();

    /**
     * Translated "Documents".
     *
     * @return translated "Documents"
     */
    @DefaultStringValue("Documents")
    @Key("documentsTabList")
    String documentsTabList();

    /**
     * Translated "Rename".
     *
     * @return translated "Rename"
     */
    @DefaultStringValue("Rename")
    @Key("renameMenuItem")
    String renameMenuItem();

    /**
     * Translated "Copy Path".
     *
     * @return translated "Copy Path"
     */
    @DefaultStringValue("Copy Path")
    @Key("copyPathMenuItem")
    String copyPathMenuItem();

    /**
     * Translated "Set Working Directory".
     *
     * @return translated "Set Working Directory"
     */
    @DefaultStringValue("Set Working Directory")
    @Key("setWorkingDirMenuItem")
    String setWorkingDirMenuItem();

    /**
     * Translated "Close".
     *
     * @return translated "Close"
     */
    @DefaultStringValue("Close")
    @Key("closeMenuItem")
    String closeMenuItem();

    /**
     * Translated "Close All".
     *
     * @return translated "Close All"
     */
    @DefaultStringValue("Close All")
    @Key("closeAllMenuItem")
    String closeAllMenuItem();

    /**
     * Translated "Close All Others".
     *
     * @return translated "Close All Others"
     */
    @DefaultStringValue("Close All Others")
    @Key("closeOthersMenuItem")
    String closeOthersMenuItem();

    /**
     * Translated "Close document tab".
     *
     * @return translated "Close document tab"
     */
    @DefaultStringValue("Close document tab")
    @Key("closeTabText")
    String closeTabText();

    /**
     * Translated "Close".
     *
     * @return translated "Close"
     */
    @DefaultStringValue("Close")
    @Key("closeButtonText")
    String closeButtonText();

    /**
     * Translated "WindowFrameButton: for keyboard support use setClickHandler instead of addClickHandler".
     *
     * @return translated "WindowFrameButton: for keyboard support use setClickHandler instead of addClickHandler"
     */
    @DefaultStringValue("WindowFrameButton: for keyboard support use setClickHandler instead of addClickHandler")
    @Key("addClickHandlerMessage")
    String addClickHandlerMessage();

    /**
     * Translated "Could Not Change Setting"
     *
     * @return translated "Could Not Change Setting"
     */
    @DefaultStringValue("Could Not Change Setting")
    @Key("docPropErrorMessage")
    String docPropErrorMessage();

    /**
     * Translated "Close popup"
     *
     * @return translated "Close popup"
     */
    @DefaultStringValue("Close popup")
    @Key("closePopupText")
    String closePopupText();

    /**
     * Translated "Please use a complete file path."
     *
     * @return translated "Please use a complete file path."
     */
    @DefaultStringValue("Please use a complete file path.")
    @Key("themeButtonOnErrorMessage")
    String themeButtonOnErrorMessage();

    /**
     * Translated "Please use a complete file path."
     *
     * @return translated "Unexpected empty response from server"
     */
    @DefaultStringValue("Unexpected empty response from server")
    @Key("onSubmitErrorMessage")
    String onSubmitErrorMessage();

    /**
     * Translated "and "
     *
     * @return translated "and "
     */
    @DefaultStringValue("and ")
    @Key("andText")
    String andText();

    /**
     * Translated "Install"
     *
     * @return translated "Install"
     */
    @DefaultStringValue("Install")
    @Key("installText")
    String installText();

    /**
     * Translated "Don't Show Again"
     *
     * @return translated "Don't Show Again"
     */
    @DefaultStringValue("Don't Show Again")
    @Key("donnotShowAgain")
    String donnotShowAgain();

    /**
     * Translated "Markdown format changes require a reload of the visual editor."
     *
     * @return translated "Markdown format changes require a reload of the visual editor."
     */
    @DefaultStringValue("Markdown format changes require a reload of the visual editor.")
    @Key("showPanmirrorText")
    String showPanmirrorText();

    /**
     * Translated "Reload Now"
     *
     * @return translated "Reload Now"
     */
    @DefaultStringValue("Reload Now")
    @Key("reloadNowText")
    String reloadNowText();

    /**
     * Translated "Install TinyTeX"
     *
     * @return translated "Install TinyTeX"
     */
    @DefaultStringValue("Install TinyTeX")
    @Key("installTinyTexText")
    String installTinyTexText();

    /**
     * Translated "This document is read only."
     *
     * @return translated "This document is read only."
     */
    @DefaultStringValue("This document is read only.")
    @Key("showReadOnlyWarningText")
    String showReadOnlyWarningText();

    /**
     * Translated "This document is read only. Generated from:"
     *
     * @return translated "This document is read only. Generated from:"
     */
    @DefaultStringValue("This document is read only. Generated from:")
    @Key("showReadOnlyWarningGeneratedText")
    String showReadOnlyWarningGeneratedText();


    /**
     * Translated "Add"
     *
     * @return translated "Add"
     */
    @DefaultStringValue("Add")
    @Key("buttonAddCaption")
    String buttonAddCaption();

    /**
     * Translated "Remove"
     *
     * @return translated "Remove"
     */
    @DefaultStringValue("Remove")
    @Key("buttonRemoveCaption")
    String buttonRemoveCaption();

    /**
     * Translated "Local repositories:"
     *
     * @return translated "Local repositories:"
     */
    @DefaultStringValue("Local repositories:")
    @Key("localReposText")
    String localReposText();

    /**
     * Translated "Help on local Packrat repositories"
     *
     * @return translated "Help on local Packrat repositories"
     */
    @DefaultStringValue("Help on local Packrat repositories")
    @Key("localReposTitle")
    String localReposTitle();

    /**
     * Translated "Add Local Repository"
     *
     * @return translated "Add Local Repository"
     */
    @DefaultStringValue("Add Local Repository")
    @Key("addLocalRepoText")
    String addLocalRepoText();

    /**
     * Translated "Error"
     *
     * @return translated "Error"
     */
    @DefaultStringValue("Error")
    @Key("errorCaption")
    String errorCaption();


    /**
     * Translated "No bindings available"
     *
     * @return translated "No bindings available"
     */
    @DefaultStringValue("No bindings available")
    @Key("emptyLabel")
    String emptyLabel();

    /**
     * Translated "Keyboard Shortcuts"
     *
     * @return translated "Keyboard Shortcuts"
     */
    @DefaultStringValue("Keyboard Shortcuts")
    @Key("keyboardShortcutsText")
    String keyboardShortcutsText();

    /**
     * Translated "Apply"
     *
     * @return translated "Apply"
     */
    @DefaultStringValue("Apply")
    @Key("applyThemeButtonText")
    String applyThemeButtonText();

    /**
     * Translated "All"
     *
     * @return translated "All"
     */
    @DefaultStringValue("All")
    @Key("radioButtonLabel")
    String radioButtonLabel();

    /**
     * Translated "Customized"
     *
     * @return translated "Customized"
     */
    @DefaultStringValue("Customized")
    @Key("radioCustomizedLabel")
    String radioCustomizedLabel();

    /**
     * Translated "Filter keyboard shortcuts"
     *
     * @return translated "Filter keyboard shortcuts"
     */
    @DefaultStringValue("Filter keyboard shortcuts")
    @Key("filterWidgetLabel")
    String filterWidgetLabel();

    /**
     * Translated "Filter..."
     *
     * @return translated "Filter..."
     */
    @DefaultStringValue("Filter...")
    @Key("filterWidgetPlaceholderText")
    String filterWidgetPlaceholderText();

    /**
     * Translated "Reset..."
     *
     * @return translated "Reset..."
     */
    @DefaultStringValue("Reset...")
    @Key("resetButtonText")
    String resetButtonText();

    /**
     * Translated "Reset Keyboard Shortcuts"
     *
     * @return translated "Reset Keyboard Shortcuts"
     */
    @DefaultStringValue("Reset Keyboard Shortcuts")
    @Key("resetKeyboardShortcutsCaption")
    String resetKeyboardShortcutsCaption();

    /**
     * Translated "Resetting Keyboard Shortcuts..."
     *
     * @return translated "Resetting Keyboard Shortcuts..."
     */
    @DefaultStringValue("Resetting Keyboard Shortcuts...")
    @Key("resetKeyboardShortcutsProgress")
    String resetKeyboardShortcutsProgress();

    /**
     * Translated "Name"
     *
     * @return translated "Name"
     */
    @DefaultStringValue("Name")
    @Key("nameColumnText")
    String nameColumnText();

    /**
     * Translated "Shortcut"
     *
     * @return translated "Shortcut"
     */
    @DefaultStringValue("Shortcut")
    @Key("editableTextColumn")
    String editableTextColumn();

    /**
     * Translated "Scope"
     *
     * @return translated "Scope"
     */
    @DefaultStringValue("Scope")
    @Key("scopeTextColumn")
    String scopeTextColumn();

    /**
     * Translated "Failed to find <input> element in table"
     *
     * @return translated "Failed to find <input> element in table"
     */
    @DefaultStringValue("Failed to find <input> element in table")
    @Key("tagNameErrorMessage")
    String tagNameErrorMessage();

    /**
     * Translated "Show:"
     *
     * @return translated "Show:"
     */
    @DefaultStringValue("Show:")
    @Key("radioShowLabel")
    String radioShowLabel();

    /**
     * Translated "Customizing Keyboard Shortcuts"
     *
     * @return translated "Customizing Keyboard Shortcuts"
     */
    @DefaultStringValue("Customizing Keyboard Shortcuts")
    @Key("customizeKeyboardHelpLink")
    String customizeKeyboardHelpLink();

    /**
     * Translated "Masked by RStudio command: "
     *
     * @return translated "Masked by RStudio command: "
     */
    @DefaultStringValue("Masked by RStudio command: ")
    @Key("addMaskedCommandStylesText")
    String addMaskedCommandStylesText();

    /**
     * Translated "Conflicts with command: "
     *
     * @return translated "Conflicts with command: "
     */
    @DefaultStringValue("Conflicts with command: ")
    @Key("addConflictCommandStylesText")
    String addConflictCommandStylesText();

    /**
     * Translated "Refresh Automatically"
     *
     * @return translated "Refresh Automatically"
     */
    @DefaultStringValue("Refresh Automatically")
    @Key("refreshAutomaticallyLabel")
    String refreshAutomaticallyLabel();

    /**
     * Translated "Manual Refresh Only"
     *
     * @return translated "Manual Refresh Only"
     */
    @DefaultStringValue("Manual Refresh Only")
    @Key("manualRefreshOnlyLabel")
    String manualRefreshOnlyLabel();

    /**
     * Translated "must be greater than or equal to "
     *
     * @return translated "must be greater than or equal to "
     */
    @DefaultStringValue("must be greater than or equal to ")
    @Key("rStudioGinjectorGreaterThanError")
    String rStudioGinjectorGreaterThanError();

    /**
     * Translated "must be less than or equal to "
     *
     * @return translated "must be less than or equal to  "
     */
    @DefaultStringValue("must be less than or equal to ")
    @Key("rStudioGinjectorLessThanError")
    String rStudioGinjectorLessThanError();

    /**
     * Translated "Stop"
     *
     * @return translated "Stop"
     */
    @DefaultStringValue("Stop")
    @Key("stopButtonText")
    String stopButtonText();

    /**
     * Translated "Secondary Window"
     *
     * @return translated "Secondary Window"
     */
    @DefaultStringValue("Secondary Window")
    @Key("satelliteToolBarText")
    String satelliteToolBarText();

    /**
     * Translated "Clear text"
     *
     * @return translated "Clear text"
     */
    @DefaultStringValue("Clear text")
    @Key("searchWidgetClearText")
    String searchWidgetClearText();

    /**
     * Translated "(None)"
     *
     * @return translated "(None)"
     */
    @DefaultStringValue("(None)")
    @Key("selectWidgetListBoxNone")
    String selectWidgetListBoxNone();

    /**
     * Translated "Keyboard Shortcut Quick Reference"
     *
     * @return translated "Keyboard Shortcut Quick Reference"
     */
    @DefaultStringValue("Keyboard Shortcut Quick Reference")
    @Key("shortcutHeaderText")
    String shortcutHeaderText();

    /**
     * Translated "Tabs"
     *
     * @return translated "Tabs"
     */
    @DefaultStringValue("Tabs")
    @Key("tabsGroupName")
    String tabsGroupName();

    /**
     * Translated "Panes"
     *
     * @return translated "Panes"
     */
    @DefaultStringValue("Panes")
    @Key("panesGroupName")
    String panesGroupName();

    /**
     * Translated "Files"
     *
     * @return translated "Files"
     */
    @DefaultStringValue("Files")
    @Key("filesGroupName")
    String filesGroupName();

    /**
     * Translated "Main Menu (Server)"
     *
     * @return translated "Main Menu (Server)"
     */
    @DefaultStringValue("Main Menu (Server)")
    @Key("mainMenuGroupName")
    String mainMenuGroupName();

    /**
     * Translated "Source Navigation"
     *
     * @return translated "Source Navigation"
     */
    @DefaultStringValue("Source Navigation")
    @Key("sourceNavigationGroupName")
    String sourceNavigationGroupName();

    /**
     * Translated "Execute"
     *
     * @return translated "Execute"
     */
    @DefaultStringValue("Execute")
    @Key("executeGroupName")
    String executeGroupName();

    /**
     * Translated "Source Editor"
     *
     * @return translated "Source Editor"
     */
    @DefaultStringValue("Source Editor")
    @Key("sourceEditorGroupName")
    String sourceEditorGroupName();

    /**
     * Translated "Debug"
     *
     * @return translated "Debug"
     */
    @DefaultStringValue("Debug")
    @Key("debugGroupName")
    String debugGroupName();

    /**
     * Translated "Accessibility"
     *
     * @return translated "Accessibility"
     */
    @DefaultStringValue("Accessibility")
    @Key("accessibilityGroupName")
    String accessibilityGroupName();

    /**
     * Translated "Source Control"
     *
     * @return translated "Source Control"
     */
    @DefaultStringValue("Source Control")
    @Key("sourceControlGroupName")
    String sourceControlGroupName();

    /**
     * Translated "Build"
     *
     * @return translated "Build"
     */
    @DefaultStringValue("Build")
    @Key("buildGroupName")
    String buildGroupName();

    /**
     * Translated "Console"
     *
     * @return translated "Console"
     */
    @DefaultStringValue("Console")
    @Key("consoleGroupName")
    String consoleGroupName();

    /**
     * Translated "Terminal"
     *
     * @return translated "Terminal"
     */
    @DefaultStringValue("Terminal")
    @Key("terminalGroupName")
    String terminalGroupName();

    /**
     * Translated "Other"
     *
     * @return translated "Other"
     */
    @DefaultStringValue("Other")
    @Key("otherGroupName")
    String otherGroupName();

    /**
     * Translated "Add Shift to zoom (maximize) pane."
     *
     * @return translated "Add Shift to zoom (maximize) pane."
     */
    @DefaultStringValue("Add Shift to zoom (maximize) pane.")
    @Key("addShiftPTag")
    String addShiftPTag();

    /**
     * Translated "Invalid usage, cannot provide both label and existingLabel"
     *
     * @return translated "Invalid usage, cannot provide both label and existingLabel"
     */
    @DefaultStringValue("Invalid usage, cannot provide both label and existingLabel")
    @Key("existingLabelMessage")
    String existingLabelMessage();

    /**
     * Translated "[Use Default]"
     *
     * @return translated "[Use Default]"
     */
    @DefaultStringValue("[Use Default]")
    @Key("useDefaultPrefix")
    String useDefaultPrefix();

    /**
     * Translated "You must enter a value."
     *
     * @return translated "You must enter a value."
     */
    @DefaultStringValue("You must enter a value.")
    @Key("validateMessage")
    String validateMessage();

    /**
     * Translated "Not a valid number."
     *
     * @return translated "Not a valid number."
     */
    @DefaultStringValue("Not a valid number.")
    @Key("notValidNumberMessage")
    String notValidNumberMessage();

    /**
     * Translated "Vim Keyboard Shortcuts"
     *
     * @return translated "Vim Keyboard Shortcuts"
     */
    @DefaultStringValue("Vim Keyboard Shortcuts")
    @Key("vimKeyboardShortcutsText")
    String vimKeyboardShortcutsText();

    /**
     * Translated "Next"
     *
     * @return translated "Next"
     */
    @DefaultStringValue("Next")
    @Key("nextButtonText")
    String nextButtonText();

    /**
     * Translated "Back"
     *
     * @return translated "Back"
     */
    @DefaultStringValue("Back")
    @Key("backButtonText")
    String backButtonText();

    /**
     * Translated "Info"
     *
     * @return translated "Info"
     */
    @DefaultStringValue("Info")
    @Key("dialogInfoText")
    String dialogInfoText();

    /**
     * Translated "Directory Contents"
     *
     * @return translated "Directory Contents"
     */
    @DefaultStringValue("Directory Contents")
    @Key("directoryContentsLabel")
    String directoryContentsLabel();

    /**
     * Translated "New Folder"
     *
     * @return translated "New Folder"
     */
    @DefaultStringValue("New Folder")
    @Key("newFolderTitle")
    String newFolderTitle();

    /**
     * Translated "Folder name"
     *
     * @return translated "Folder name"
     */
    @DefaultStringValue("Folder name")
    @Key("folderNameLabel")
    String folderNameLabel();

    /**
     * Translated "for"
     *
     * @return translated "for"
     */
    @DefaultStringValue("for")
    @Key("forAttributeName")
    String forAttributeName();

    /**
     * Translated "spellcheck"
     *
     * @return translated "spellcheck"
     */
    @DefaultStringValue("spellcheck")
    @Key("spellCheckAttribute")
    String spellCheckAttribute();

    /**
     * Translated "Warning"
     *
     * @return translated "Warning"
     */
    @DefaultStringValue("Warning")
    @Key("dialogWarningText")
    String dialogWarningText();

    /**
     * Translated "Question"
     *
     * @return translated "Question"
     */
    @DefaultStringValue("Question")
    @Key("dialogQuestionText")
    String dialogQuestionText();

    /**
     * Translated "Popup Blocked"
     *
     * @return translated "Popup Blocked"
     */
    @DefaultStringValue("Popup Blocked")
    @Key("dialogPopupBlockedText")
    String dialogPopupBlockedText();

    /**
     * Translated "Error"
     *
     * @return translated "Error"
     */
    @DefaultStringValue("Error")
    @Key("dialogErrorText")
    String dialogErrorText();


    /**
     * Translated "minimized"
     *
     * @return translated "minimized"
     */
    @DefaultStringValue("minimized")
    @Key("minimizedTabListRole")
    String minimizedTabListRole();

    /**
     * Translated "No potentially focusable controls found in modal dialog"
     *
     * @return translated "No potentially focusable controls found in modal dialog"
     */
    @DefaultStringValue("No potentially focusable controls found in modal dialog")
    @Key("noFocusableControlsLog")
    String noFocusableControlsLog();

    /**
     * Translated "Manual Refresh Only"
     *
     * @return translated "Manual Refresh Only"
     */
    @DefaultStringValue("Manual Refresh Only")
    @Key("manualRefreshLabel")
    String manualRefreshLabel();

    /**
     * Translated "Can't create progress spinner (no HTML5 canvas support)"
     *
     * @return translated "Can't create progress spinner (no HTML5 canvas support)"
     */
    @DefaultStringValue("Can't create progress spinner (no HTML5 canvas support)")
    @Key("progressSpinnerLog")
    String progressSpinnerLog();

    /**
     * Translated "Busy"
     *
     * @return translated "Busy"
     */
    @DefaultStringValue("Busy")
    @Key("busyLabel")
    String busyLabel();

    /**
     * Translated "[REDACTED]"
     *
     * @return translated "[REDACTED]"
     */
    @DefaultStringValue("[REDACTED]")
    @Key("redactedText")
    String redactedText();


    /**
     * Translated "Vim keyboard shortcut help not screen reader accessible. Press any key to close."
     *
     * @return translated "Vim keyboard shortcut help not screen reader accessible. Press any key to close."
     */
    @DefaultStringValue("Vim keyboard shortcut help not screen reader accessible. Press any key to close.")
    @Key("vimKeyboardShortcutHelpMessage")
    String vimKeyboardShortcutHelpMessage();

    /**
     * Translated "We attempted to open an external browser window, but the action was prevented by your popup blocker. You can attempt to open the window again by pressing the "Try Again" button below. NOTE: To prevent seeing this message in the future, you should configure your browser to allow popup windows for {0}."
     *
     * @return translated "We attempted to open an external browser window, but the action was prevented by your popup blocker. You can attempt to open the window again by pressing the "Try Again" button below. NOTE: To prevent seeing this message in the future, you should configure your browser to allow popup windows for {0}."
     */
    @DefaultStringValue("We attempted to open an external browser window, but the action was prevented by your popup blocker. You can attempt to open the window again by pressing the \"Try Again\" button below. NOTE: To prevent seeing this message in the future, you should configure your browser to allow popup windows for {0}.")
    @Key("showPopupBlockMessage")
    String showPopupBlockMessage(String hostName);

    /**
     * Translated "Status code {0} returned by {1} when executing '{2}'"
     *
     * @return translated "Status code {0} returned by {1} when executing '{2}'"
     */
    @DefaultStringValue("Status code {0} returned by {1} when executing '{2}'")
    @Key("rpcErrorMessage")
    String rpcErrorMessage(String statusCode, String desktop, String method);

    /**
     * Translated "Unable to establish connection with {0} when executing '{1}'"
     *
     * @return translated "Unable to establish connection with {0} when executing '{1}'"
     */
    @DefaultStringValue("Unable to establish connection with {0} when executing '{1}'")
    @Key("rpcOverrideErrorMessage")
    String rpcOverrideErrorMessage(String desktop, String method);

    /**
     * Translated "You need to restart RStudio in order for these changes to take effect. Do you want to do this now?"
     *
     * @return translated "You need to restart RStudio in order for these changes to take effect. Do you want to do this now?"
     */
    @DefaultStringValue("You need to restart RStudio in order for these changes to take effect. Do you want to do this now?")
    @Key("restartRequiredMessage")
    String restartRequiredMessage();

    /**
     * Translated "{0} minimized"
     *
     * @return translated "{0} minimized"
     */
    @DefaultStringValue("{0} minimized")
    @Key("minimizedTabListRole")
    String minimizedTabListRole(String accessibleName);

    /**
     * Translated "Close {0} tab"
     *
     * @return translated "Close {0} tab"
     */
    @DefaultStringValue("Close {0} tab")
    @Key("closeButtonText")
    String closeButtonText(String title);

    /**
     * Translated "Minimize {0}"
     *
     * @return translated "Minimize {0}"
     */
    @DefaultStringValue("Minimize {0}")
    @Key("minimizeState")
    String minimizeState(String name);

    /**
     * Translated "Maximize {0}"
     *
     * @return translated "Maximize {0}"
     */
    @DefaultStringValue("Maximize {0}")
    @Key("maximizeState")
    String maximizeState(String name);

    /**
     * Translated "Restore {0}"
     *
     * @return translated "Restore {0}"
     */
    @DefaultStringValue("Restore {0}")
    @Key("normalState")
    String normalState(String name);

    /**
     * Translated "Hide {0}"
     *
     * @return translated "Hide {0}"
     */
    @DefaultStringValue("Hide {0}")
    @Key("hideState")
    String hideState(String name);

    /**
     * Translated "Exclusive {0}"
     *
     * @return translated "Exclusive {0}"
     */
    @DefaultStringValue("Exclusive {0}")
    @Key("exclusiveState")
    String exclusiveState(String name);

    /**
     * Translated "Package {0} required but is not installed."
     *
     * @return translated "Package {0} required but is not installed."
     */
    @DefaultStringValue("Package {0} required but is not installed.")
    @Key("package1Message")
    String package1Message(String packages);

    /**
     * Translated "Package {0} and {1} required but are not installed."
     *
     * @return translated "Package {0} and {1} required but are not installed."
     */
    @DefaultStringValue("Package {0} and {1} required but are not installed.")
    @Key("packages2Message")
    String packages2Message(String package0, String package1);

    /**
     * Translated "Packages {0} , {1} , and {2} required but are not installed."
     *
     * @return translated "Packages {0} , {1} , and {2} required but are not installed."
     */
    @DefaultStringValue("Packages {0} , {1} , and {2} required but are not installed.")
    @Key("packages3Message")
    String packages3Message(String package0, String package1, String package2);

    /**
     * Translated "Packages {0} , {1} , and {2} others required but are not installed."
     *
     * @return translated "Packages {0} , {1} , and {2} others required but are not installed."
     */
    @DefaultStringValue("Packages {0} , {1} , and {2} others required but are not installed.")
    @Key("otherPackagesMessage")
    String otherPackagesMessage(String package0, String package1, String package2);

    /**
     * Translated "Are you sure you want to reset keyboard shortcuts to their default values? This action cannot be undone."
     *
     * @return translated "Are you sure you want to reset keyboard shortcuts to their default values? This action cannot be undone."
     */
    @DefaultStringValue("Are you sure you want to reset keyboard shortcuts to their default values? This action cannot be undone.")
    @Key("resetKeyboardShortcutsMessage")
    String resetKeyboardShortcutsMessage();

    /**
     * Translated "{0} must be a valid number."
     *
     * @return translated "{0} must be a valid number."
     */
    @DefaultStringValue("{0} must be a valid number.")
    @Key("rStudioGinjectorErrorMessage")
    String rStudioGinjectorErrorMessage(String label);

    /**
     * Translated "{0} must be greater than or equal to {1}."
     *
     * @return translated "{0} must be greater than or equal to {1}."
     */
    @DefaultStringValue("{0} must be greater than or equal to {1}.")
    @Key("rStudioGinjectorGreaterThanError")
    String rStudioGinjectorGreaterThanError(String label, int minValue);

    /**
     * Translated "{0} must be less than or equal to {1}."
     *
     * @return translated "{0} must be less than or equal to {1}."
     */
    @DefaultStringValue("{0} must be less than or equal to {1}.")
    @Key("rStudioGinjectorLessThanError")
    String rStudioGinjectorLessThanError(String label, int maxValue);

    /**
     * Translated "Operation completed "
     *
     * @return translated "Operation completed "
     */
    @DefaultStringValue("Operation completed ")
    @Key("operationCompletedText")
    String operationCompletedText();

    /**
     * Translated "{0} completed"
     *
     * @return translated "{0} completed"
     */
    @DefaultStringValue("{0} completed")
    @Key("completedText")
    String completedText(String labelText);
}
