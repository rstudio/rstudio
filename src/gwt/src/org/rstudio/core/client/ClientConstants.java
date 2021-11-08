package org.rstudio.core.client;

public interface ClientConstants extends com.google.gwt.i18n.client.Constants {

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
     * Translated "We attempted to open an external browser window, but".
     *
     * @return translated "We attempted to open an external browser window, but"
     */
    @DefaultStringValue("We attempted to open an external browser window, but")
    @Key("popupBlockMessage")
    String popupBlockMessage();

    /**
     * Translated "the action was prevented by your popup blocker. You".
     *
     * @return translated "the action was prevented by your popup blocker. You"
     */
    @DefaultStringValue("the action was prevented by your popup blocker. You")
    @Key("popupBlockActionMessage")
    String popupBlockActionMessage();

    /**
     * Translated "can attempt to open the window again by pressing the".
     *
     * @return translated "can attempt to open the window again by pressing the"
     */
    @DefaultStringValue("can attempt to open the window again by pressing the")
    @Key("popupBlockAttemptMessage")
    String popupBlockAttemptMessage();

    /**
     * Translated "Try Again" button below.\n\n".
     *
     * @return translated "can attempt Try Again" button below.\n\n"
     */
    @DefaultStringValue("Try Again\" button below.\\n\\n")
    @Key("popupBlockTryAgainMessage")
    String popupBlockTryAgainMessage();

    /**
     * Translated "NOTE: To prevent seeing this message in the future, you".
     *
     * @return translated "NOTE: To prevent seeing this message in the future, you"
     */
    @DefaultStringValue("NOTE: To prevent seeing this message in the future, you")
    @Key("popupBlockNote")
    String popupBlockNote();

    /**
     * Translated "should configure your browser to allow popup windows".
     *
     * @return translated "should configure your browser to allow popup windows"
     */
    @DefaultStringValue("should configure your browser to allow popup windows")
    @Key("popupBlockAllowMessage")
    String popupBlockAllowMessage();

    /**
     * Translated "for".
     *
     * @return translated "for"
     */
    @DefaultStringValue("for")
    @Key("popupBlockFor")
    String popupBlockFor();

    /**
     * Translated "Try Again".
     *
     * @return translated "Try Again"
     */
    @DefaultStringValue("Try Again")
    @Key("popupBlockTryAgainLabel")
    String popupBlockTryAgainLabel();

    /**
     * Translated "cannot add - already running".
     *
     * @return translated "cannot add - already running"
     */
    @DefaultStringValue("cannot add - already running")
    @Key("addCommandLabel")
    String addCommandLabel();

    /**
     * Translated "already running".
     *
     * @return translated "already running"
     */
    @DefaultStringValue("already running")
    @Key("runningLabel")
    String runningLabel();

    /**
     * Translated "finished cmd".
     *
     * @return translated "finished cmd"
     */
    @DefaultStringValue("finished cmd")
    @Key("finishedCmdLabel")
    String finishedCmdLabel();

    /**
     * Translated "countdown".
     *
     * @return translated "countdown"
     */
    @DefaultStringValue("countdown")
    @Key("countdownLabel")
    String countdownLabel();

    /**
     * Translated "done".
     *
     * @return translated "done"
     */
    @DefaultStringValue("done")
    @Key("doneLabel")
    String doneLabel();

    /**
     * Translated "size=".
     *
     * @return translated "size="
     */
    @DefaultStringValue("size=")
    @Key("logSizeLabel")
    String logSizeLabel();

    /**
     * Translated "second".
     *
     * @return translated "second"
     */
    @DefaultStringValue("second")
    @Key("secondLabel")
    String secondLabel();

    /**
     * Translated "minute".
     *
     * @return translated "minute"
     */
    @DefaultStringValue("minute")
    @Key("minuteLabel")
    String minuteLabel();

    /**
     * Translated "hour".
     *
     * @return translated "hour"
     */
    @DefaultStringValue("hour")
    @Key("hourLabel")
    String hourLabel();

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
     * Translated "This case should be covered by navigateIfDirectory".
     *
     * @return translated "This case should be covered by navigateIfDirectory"
     */
    @DefaultStringValue("This case should be covered by navigateIfDirectory")
    @Key("navigateIfDirectoryMessage")
    String navigateIfDirectoryMessage();

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
     * Translated "Status code ".
     *
     * @return translated "Status code "
     */
    @DefaultStringValue("Status code ")
    @Key("onResponseStatusCodeMessage")
    String onResponseStatusCodeMessage();

    /**
     * Translated "returned by ".
     *
     * @return translated "returned by "
     */
    @DefaultStringValue("returned by ")
    @Key("onResponseReturnedBy")
    String onResponseReturnedBy();

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
     * Translated "when executing '".
     *
     * @return translated "when executing '"
     */
    @DefaultStringValue("when executing '")
    @Key("whenExecutingMessage")
    String whenExecutingMessage();

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
     * Translated "You need to restart RStudio in order for these changes to take effect. ".
     *
     * @return translated "You need to restart RStudio in order for these changes to take effect. "
     */
    @DefaultStringValue("You need to restart RStudio in order for these changes to take effect. ")
    @Key("restartRequiredMessage")
    String restartRequiredMessage();

    /**
     * Translated "Do you want to do this now?".
     *
     * @return translated "Do you want to do this now?"
     */
    @DefaultStringValue("Do you want to do this now?")
    @Key("restartNowMessage")
    String restartNowMessage();

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
     * Translated "tab".
     *
     * @return translated "tab"
     */
    @DefaultStringValue("tab")
    @Key("closeAltTabText")
    String closeAltTabText();

    /**
     * Translated "WindowFrameButton: for keyboard support use setClickHandler instead of addClickHandler".
     *
     * @return translated "WindowFrameButton: for keyboard support use setClickHandler instead of addClickHandler"
     */
    @DefaultStringValue("WindowFrameButton: for keyboard support use setClickHandler instead of addClickHandler")
    @Key("addClickHandlerMessage")
    String addClickHandlerMessage();

    /**
     * Translated "Minimize".
     *
     * @return translated "Minimize"
     */
    @DefaultStringValue("Minimize")
    @Key("minimizeState")
    String minimizeState();

    /**
     * Translated "Maximize".
     *
     * @return translated "Maximize"
     */
    @DefaultStringValue("Maximize")
    @Key("maximizeState")
    String maximizeState();

    /**
     * Translated "Restore".
     *
     * @return translated "Restore"
     */
    @DefaultStringValue("Restore")
    @Key("normalState")
    String normalState();

    /**
     * Translated "Hide".
     *
     * @return translated "Hide"
     */
    @DefaultStringValue("Hide")
    @Key("hideState")
    String hideState();

    /**
     * Translated "Exclusive".
     *
     * @return translated "Exclusive"
     */
    @DefaultStringValue("Exclusive")
    @Key("exclusiveState")
    String exclusiveState();

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
     * Translated "Package ".
     *
     * @return translated "Package "
     */
    @DefaultStringValue("Package ")
    @Key("packageMessage")
    String packageMessage();

    /**
     * Translated "required but is not installed."
     *
     * @return translated "required but is not installed."
     */
    @DefaultStringValue("required but is not installed.")
    @Key("packageNotInstalledMessage")
    String packageNotInstalledMessage();

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
     * Translated "others required but are not installed."
     *
     * @return translated "others required but are not installed."
     */
    @DefaultStringValue("others required but are not installed.")
    @Key("packageRequiredMessage")
    String packageRequiredMessage();

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
     * Translated "Are you sure you want to reset keyboard shortcuts to their default values? "
     *
     * @return translated "Are you sure you want to reset keyboard shortcuts to their default values? "
     */
    @DefaultStringValue("Are you sure you want to reset keyboard shortcuts to their default values? ")
    @Key("resetKeyboardShortcutsMessage")
    String resetKeyboardShortcutsMessage();

    /**
     * Translated "This action cannot be undone."
     *
     * @return translated "This action cannot be undone."
     */
    @DefaultStringValue("This action cannot be undone.")
    @Key("cannotUndoShortcutsMessage")
    String cannotUndoShortcutsMessage();

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
     * Translated "must be a valid number."
     *
     * @return translated "must be a valid number."
     */
    @DefaultStringValue("must be a valid number.")
    @Key("rStudioGinjectorErrorMessage")
    String rStudioGinjectorErrorMessage();

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
     * Translated "Operation completed"
     *
     * @return translated "Operation completed"
     */
    @DefaultStringValue("Operation completed")
    @Key("operationCompletedText")
    String operationCompletedText();

    /**
     * Translated "completed"
     *
     * @return translated "completed"
     */
    @DefaultStringValue("completed")
    @Key("completedText")
    String completedText();

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
     * Translated "Unexpected element: "
     *
     * @return translated "Unexpected element: "
     */
    @DefaultStringValue("Unexpected element: ")
    @Key("unexpectedElementMessage")
    String unexpectedElementMessage();

    /**
     * Translated "Required attribute shortcut was missing \n"
     *
     * @return translated "Required attribute shortcut was missing \n"
     */
    @DefaultStringValue("Required attribute shortcut was missing\\n")
    @Key("shortcutMissingLog")
    String shortcutMissingLog();

    /**
     * Translated "Invalid modifier '"
     *
     * @return translated "Invalid modifier '"
     */
    @DefaultStringValue("Invalid modifier '")
    @Key("invalidModifierErrorLog")
    String invalidModifierErrorLog();

    /**
     * Translated "'; expected one of "
     *
     * @return translated "'; expected one of "
     */
    @DefaultStringValue("'; expected one of ")
    @Key("expectedOneOfLog")
    String expectedOneOfLog();

    /**
     * Translated "Invalid key sequence: sequences must be of length 1 or 2"
     *
     * @return translated "Invalid key sequence: sequences must be of length 1 or 2"
     */
    @DefaultStringValue("Invalid key sequence: sequences must be of length 1 or 2")
    @Key("invalidKeySequenceLog")
    String invalidKeySequenceLog();

    /**
     * Translated "Returning null from toKeyCode for key "
     *
     * @return translated "Returning null from toKeyCode for key "
     */
    @DefaultStringValue("Returning null from toKeyCode for key ")
    @Key("returningNullLog")
    String returningNullLog();

    /**
     * Translated "Error attempting to stringify some XML"
     *
     * @return translated "Error attempting to stringify some XML"
     */
    @DefaultStringValue("Error attempting to stringify some XML")
    @Key("errorStringifyLog")
    String errorStringifyLog();

}
