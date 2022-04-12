/*
 * PrefsConstants.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.prefs;

import com.google.gwt.i18n.client.Constants;

public interface PrefsConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Verify Key...".
     *
     * @return translated "Verify Key..."
     */
    @DefaultMessage("Verify Key...")
    @Key("verifyKey")
    String verifyKey();

    /**
     * Translated "Unable to verify Zotero API key.\n\nYou should verify that your API key is still valid, and if necessary create a new key.".
     *
     * @return translated "Unable to verify Zotero API key.\n\nYou should verify that your API key is still valid, and if necessary create a new key."
     */
    @DefaultMessage("Unable to verify Zotero API key.\n\nYou should verify that your API key is still valid, and if necessary create a new key.")
    @Key("zoteroVerifyKeyFailedMessage")
    String zoteroVerifyKeyFailedMessage();

    /**
     * Translated "Zotero Web API Key:".
     *
     * @return translated "Zotero Web API Key:"
     */
    @DefaultMessage("Zotero Web API Key:")
    @Key("zoteroWebApiKey")
    String zoteroWebApiKey();

    /**
     * Translated "Verifying Key...".
     *
     * @return translated "Verifying Key..."
     */
    @DefaultMessage("Verifying Key...")
    @Key("verifyingKey")
    String verifyingKey();

    /**
     * Translated "Zotero".
     *
     * @return translated "Zotero"
     */
    @DefaultMessage("Zotero")
    @Key("zotero")
    String zotero();

    /**
     * Translated "Zotero API key successfully verified.".
     *
     * @return translated "Zotero API key successfully verified."
     */
    @DefaultMessage("Zotero API key successfully verified.")
    @Key("zoteroKeyVerified")
    String zoteroKeyVerified();

    /**
     * Translated "Use libraries:".
     *
     * @return translated "Use libraries:"
     */
    @DefaultMessage("Use libraries:")
    @Key("useLibraries")
    String useLibraries();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    @Key("error")
    String error();

    /**
     * Translated "You must select at least one Zotero library".
     *
     * @return translated "You must select at least one Zotero library"
     */
    @DefaultMessage("You must select at least one Zotero library")
    @Key("selectOneZoteroLibrary")
    String selectOneZoteroLibrary();

    /**
     * Translated "(Default)".
     *
     * @return translated "(Default)"
     */
    @DefaultMessage("(Default)")
    @Key("defaultInParentheses")
    String defaultInParentheses();

    /**
     * Translated "My Library".
     *
     * @return translated "My Library"
     */
    @DefaultMessage("My Library")
    @Key("myLibrary")
    String myLibrary();

    /**
     * Translated "Selected Libraries".
     *
     * @return translated "Selected Libraries"
     */
    @DefaultMessage("Selected Libraries")
    @Key("selectedLibraries")
    String selectedLibraries();

    /**
     * Translated "Conda Environment".
     *
     * @return translated "Conda Environment"
     */
    @DefaultMessage("Conda Environment")
    @Key("condaEnvironment")
    String condaEnvironment();

    /**
     * Translated "Virtual Environment".
     *
     * @return translated "Virtual Environment"
     */
    @DefaultMessage("Virtual Environment")
    @Key("virtualEnvironment")
    String virtualEnvironment();

    /**
     * Translated "Python Interpreter".
     *
     * @return translated "Python Interpreter"
     */
    @DefaultMessage("Python Interpreter")
    @Key("pythonInterpreter")
    String pythonInterpreter();

    /**
     * Translated "System".
     *
     * @return translated "System"
     */
    @DefaultMessage("System")
    @Key("system")
    String system();

    /**
     * Translated "Virtual Environments".
     *
     * @return translated "Virtual Environments"
     */
    @DefaultMessage("Virtual Environments")
    @Key("virtualEnvironmentPlural")
    String virtualEnvironmentPlural();

    /**
     * Translated "Conda Environments".
     *
     * @return translated "Conda Environments"
     */
    @DefaultMessage("Conda Environments")
    @Key("condaEnvironmentPlural")
    String condaEnvironmentPlural();

    /**
     * Translated "Python Interpreters".
     *
     * @return translated "Python Interpreters"
     */
    @DefaultMessage("Python Interpreters")
    @Key("pythonInterpreterPlural")
    String pythonInterpreterPlural();

    /**
     * Translated "Select".
     *
     * @return translated "Select"
     */
    @DefaultMessage("Select")
    @Key("select")
    String select();

    /**
     * Translated "(None available)".
     *
     * @return translated "(None available)"
     */
    @DefaultMessage("(None available)")
    @Key("noneAvailableParentheses")
    String noneAvailableParentheses();

    /**
     * Translated "Editor Theme Preview".
     *
     * @return translated "Editor Theme Preview"
     */
    @DefaultMessage("Editor Theme Preview")
    @Key("editorThemePreview")
    String editorThemePreview();

    /**
     * Translated "Spelling Prefs".
     *
     * @return translated "Spelling Prefs"
     */
    @DefaultMessage("Spelling Prefs")
    @Key("spellingPrefsTitle")
    String spellingPrefsTitle();

    /**
     * Translated "The context for the user''s spelling preferences.".
     *
     * @return translated "The context for the user''s spelling preferences."
     */
    @DefaultMessage("The context for the user''s spelling preferences.")
    @Key("spellingPrefsDescription")
    String spellingPrefsDescription();

    /**
     * Translated "RSA Public Key Filename".
     *
     * @return translated "RSA Public Key Filename"
     */
    @DefaultMessage("RSA Public Key Filename")
    @Key("rsaKeyFileTitle")
    String rsaKeyFileTitle();

    /**
     * Translated "Filename of RSA public key".
     *
     * @return translated "Filename of RSA public key"
     */
    @DefaultMessage("Filename of RSA public key")
    @Key("rsaKeyFileDescription")
    String rsaKeyFileDescription();

    /**
     * Translated "Has RSA Key".
     *
     * @return translated "Has RSA Key"
     */
    @DefaultMessage("Has RSA Key")
    @Key("haveRSAKeyTitle")
    String haveRSAKeyTitle();

    /**
     * Translated "Whether the user has an RSA key".
     *
     * @return translated "Whether the user has an RSA key"
     */
    @DefaultMessage("Whether the user has an RSA key")
    @Key("haveRSAKeyDescription")
    String haveRSAKeyDescription();

    /**
     * Translated "Error Changing Setting".
     *
     * @return translated "Error Changing Setting"
     */
    @DefaultMessage("Error Changing Setting")
    @Key("errorChangingSettingCaption")
    String errorChangingSettingCaption();

    /**
     * Translated "The tab key moves focus setting could not be updated.".
     *
     * @return translated "The tab key moves focus setting could not be updated."
     */
    @DefaultMessage("The tab key moves focus setting could not be updated.")
    @Key("tabKeyErrorMessage")
    String tabKeyErrorMessage();

    /**
     * Translated "Tab key always moves focus on".
     *
     * @return translated "Tab key always moves focus on"
     */
    @DefaultMessage("Tab key always moves focus on")
    @Key("tabKeyFocusOnMessage")
    String tabKeyFocusOnMessage();

    /**
     * Translated "Tab key always moves focus off".
     *
     * @return translated "Tab key always moves focus off"
     */
    @DefaultMessage("Tab key always moves focus off")
    @Key("tabKeyFocusOffMessage")
    String tabKeyFocusOffMessage();

    /**
     * Translated "The screen reader support setting could not be changed.".
     *
     * @return translated "The screen reader support setting could not be changed."
     */
    @DefaultMessage("The screen reader support setting could not be changed.")
    @Key("toggleScreenReaderErrorMessage")
    String toggleScreenReaderErrorMessage();

    /**
     * Translated "Confirm Toggle Screen Reader Support".
     *
     * @return translated "Confirm Toggle Screen Reader Support"
     */
    @DefaultMessage("Confirm Toggle Screen Reader Support")
    @Key("toggleScreenReaderConfirmCaption")
    String toggleScreenReaderConfirmCaption();

    /**
     * Translated "Are you sure you want to {0} screen reader support? The application will reload to apply the change.".
     *
     * @return translated "Are you sure you want to {0} screen reader support? The application will reload to apply the change."
     */
    @DefaultMessage("Are you sure you want to {0} screen reader support? The application will reload to apply the change.")
    @Key("toggleScreenReaderMessageConfirmDialog")
    String toggleScreenReaderMessageConfirmDialog(String value);

    /**
     * Translated "disable".
     *
     * @return translated "disable"
     */
    @DefaultMessage("disable")
    @Key("disable")
    String disable();

    /**
     * Translated "enable".
     *
     * @return translated "enable"
     */
    @DefaultMessage("enable")
    @Key("enable")
    String enable();

    /**
     * Translated "Warning: screen reader mode not enabled. Turn on using shortcut {0}.".
     *
     * @return translated "Warning: screen reader mode not enabled. Turn on using shortcut {0}."
     */
    @DefaultMessage("Warning: screen reader mode not enabled. Turn on using shortcut {0}.")
    @Key("announceScreenReaderStateMessage")
    String announceScreenReaderStateMessage(String shortcut);

    /**
     * Translated "{0} (enabled)".
     *
     * @return translated "{0} (enabled)"
     */
    @DefaultMessage("{0} (enabled)")
    @Key("screenReaderStateEnabled")
    String screenReaderStateEnabled(String screenReaderLabel);

    /**
     * Translated "{0} (disabled)".
     *
     * @return translated "{0} (disabled)"
     */
    @DefaultMessage("{0} (disabled)")
    @Key("screenReaderStateDisabled")
    String screenReaderStateDisabled(String screenReaderLabel);

    /**
     * Translated "Clear Preferences".
     *
     * @return translated "Clear Preferences"
     */
    @DefaultMessage("Clear Preferences")
    @Key("onClearUserPrefsYesLabel")
    String onClearUserPrefsYesLabel();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    @DefaultMessage("Cancel")
    @Key("cancel")
    String cancel();

    /**
     * Translated "Restart R".
     *
     * @return translated "Restart R"
     */
    @DefaultMessage("Restart R")
    @Key("onClearUserPrefsRestartR")
    String onClearUserPrefsRestartR();

    /**
     * Translated "Preferences Cleared".
     *
     * @return translated "Preferences Cleared"
     */
    @DefaultMessage("Preferences Cleared")
    @Key("onClearUserPrefsResponseCaption")
    String onClearUserPrefsResponseCaption();

    /**
     * Translated "Your preferences have been cleared, and your R session will now be restarted.
     * A backup copy of your preferences can be found at: \n\n{0}".
     *
     * @return translated "Your preferences have been cleared, and your R session will now be restarted.
     * A backup copy of your preferences can be found at: \n\n{0}"
     */
    @DefaultMessage("Your preferences have been cleared, and your R session will now be restarted. " +
            "A backup copy of your preferences can be found at: \n\n{0}")
    @Key("onClearUserPrefsResponseMessage")
    String onClearUserPrefsResponseMessage(String path);

    /**
     * Translated "Confirm Clear Preferences".
     *
     * @return translated "Confirm Clear Preferences"
     */
    @DefaultMessage("Confirm Clear Preferences")
    @Key("onClearUserPrefsCaption")
    String onClearUserPrefsCaption();

    /**
     * Translated "Are you sure you want to clear your preferences?
     * All RStudio settings will be restored to their defaults, and your R session will be restarted.".
     *
     * @return translated "Are you sure you want to clear your preferences?
     * All RStudio settings will be restored to their defaults, and your R session will be restarted."
     */
    @DefaultMessage("Are you sure you want to clear your preferences? " +
            "All RStudio settings will be restored to their defaults, and your R session will be restarted.")
    @Key("onClearUserPrefsMessage")
    String onClearUserPrefsMessage();

    /**
     * Translated "Using Zotero".
     *
     * @return translated "Using Zotero"
     */
    @DefaultMessage("Using Zotero")
    @Key("usingZotero")
    String usingZotero();

    /**
     * Translated "Zotero Library:".
     *
     * @return translated "Zotero Library:"
     */
    @DefaultMessage("Zotero Library:")
    @Key("zoteroLibrary")
    String zoteroLibrary();

    /**
     * Translated "Web".
     *
     * @return translated "Web"
     */
    @DefaultMessage("Web")
    @Key("web")
    String web();

    /**
     * Translated "Local".
     *
     * @return translated "Local"
     */
    @DefaultMessage("Local")
    @Key("local")
    String local();

    /**
     * Translated "(None)".
     *
     * @return translated "(None)"
     */
    @DefaultMessage("(None)")
    @Key("noneParentheses")
    String noneParentheses();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    @DefaultMessage("General")
    @Key("general")
    String general();

    /**
     * Translated "Line ending conversion:".
     *
     * @return translated "Line ending conversion:"
     */
    @DefaultMessage("Line ending conversion:")
    @Key("lineEndingConversion")
    String lineEndingConversion();

    /**
     * Translated "(Use Default)".
     *
     * @return translated "(Use Default)"
     */
    @DefaultMessage("(Use Default)")
    @Key("useDefaultParentheses")
    String useDefaultParentheses();

    /**
     * Translated "None".
     *
     * @return translated "None"
     */
    @DefaultMessage("None")
    @Key("none")
    String none();

    /**
     * Translated "Platform Native".
     *
     * @return translated "Platform Native"
     */
    @DefaultMessage("Platform Native")
    @Key("platformNative")
    String platformNative();

    /**
     * Translated "Posix (LF)".
     *
     * @return translated "Posix (LF)"
     */
    @DefaultMessage("Posix (LF)")
    @Key("posixLF")
    String posixLF();

    /**
     * Translated "Windows (CR/LF)".
     *
     * @return translated "Windows (CR/LF)"
     */
    @DefaultMessage("Windows (CR/LF)")
    @Key("windowsCRLF")
    String windowsCRLF();

    /**
     * Translated "Options".
     *
     * @return translated "Options"
     */
    @DefaultMessage("Options")
    @Key("options")
    String options();

    /**
     * Translated "Assistive Tools".
     *
     * @return translated "Assistive Tools"
     */
    @DefaultMessage("Assistive Tools")
    @Key("generalHeaderPanel")
    String generalHeaderPanel();

    /**
     * Translated "Screen reader support (requires restart)".
     *
     * @return translated "Screen reader support (requires restart)"
     */
    @DefaultMessage("Screen reader support (requires restart)")
    @Key("chkScreenReaderLabel")
    String chkScreenReaderLabel();

    /**
     * Translated "Milliseconds after typing before speaking results".
     *
     * @return translated "Milliseconds after typing before speaking results"
     */
    @DefaultMessage("Milliseconds after typing before speaking results")
    @Key("typingStatusDelayLabel")
    String typingStatusDelayLabel();

    /**
     * Translated "Maximum number of console output lines to read".
     *
     * @return translated "Maximum number of console output lines to read"
     */
    @DefaultMessage("Maximum number of console output lines to read")
    @Key("maxOutputLabel")
    String maxOutputLabel();

    /**
     * Translated "Other".
     *
     * @return translated "Other"
     */
    @DefaultMessage("Other")
    @Key("displayLabel")
    String displayLabel();

    /**
     * Translated "Reduce user interface animations".
     *
     * @return translated "Reduce user interface animations"
     */
    @DefaultMessage("Reduce user interface animations")
    @Key("reducedMotionLabel")
    String reducedMotionLabel();

    /**
     * Translated "Tab key always moves focus".
     *
     * @return translated "Tab key always moves focus"
     */
    @DefaultMessage("Tab key always moves focus")
    @Key("chkTabMovesFocusLabel")
    String chkTabMovesFocusLabel();

    /**
     * Translated "Always show focus outlines (requires restart)".
     *
     * @return translated "Always show focus outlines (requires restart)"
     */
    @DefaultMessage("Always show focus outlines (requires restart)")
    @Key("chkShowFocusLabel")
    String chkShowFocusLabel();

    /**
     * Translated "Highlight focused panel".
     *
     * @return translated "Highlight focused panel"
     */
    @DefaultMessage("Highlight focused panel")
    @Key("generalPanelLabel")
    String generalPanelLabel();

    /**
     * Translated "RStudio accessibility help".
     *
     * @return translated "RStudio accessibility help"
     */
    @DefaultMessage("RStudio accessibility help")
    @Key("helpRStudioAccessibilityLinkLabel")
    String helpRStudioAccessibilityLinkLabel();

    /**
     * Translated "Enable / Disable Announcements".
     *
     * @return translated "Enable / Disable Announcements"
     */
    @DefaultMessage("Enable / Disable Announcements")
    @Key("announcementsLabel")
    String announcementsLabel();

    /**
     * Translated "Accessibility".
     *
     * @return translated "Accessibility"
     */
    @DefaultMessage("Accessibility")
    @Key("tabHeaderPanel")
    String tabHeaderPanel();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    @DefaultMessage("General")
    @Key("generalPanelText")
    String generalPanelText();

    /**
     * Translated "Announcements".
     *
     * @return translated "Announcements"
     */
    @DefaultMessage("Announcements")
    @Key("announcementsPanelText")
    String announcementsPanelText();

    /**
     * Translated "RStudio theme:".
     *
     * @return translated "RStudio theme:"
     */
    @DefaultMessage("RStudio theme:")
    @Key("appearanceRStudioThemeLabel")
    String appearanceRStudioThemeLabel();

    /**
     * Translated "Zoom:".
     *
     * @return translated "Zoom:"
     */
    @DefaultMessage("Zoom:")
    @Key("appearanceZoomLabelZoom")
    String appearanceZoomLabelZoom();

    /**
     * Translated "Editor font (loading...):".
     *
     * @return translated "Editor font (loading...):"
     */
    @DefaultMessage("Editor font (loading...):")
    @Key("fontFaceEditorFontLabel")
    String fontFaceEditorFontLabel();

    /**
     * Translated "Editor font:".
     *
     * @return translated "Editor font:"
     */
    @DefaultMessage("Editor font:")
    @Key("appearanceEditorFontLabel")
    String appearanceEditorFontLabel();

    /**
     * Translated "Editor font size:".
     *
     * @return translated "Editor font size:"
     */
    @DefaultMessage("Editor font size:")
    @Key("appearanceEditorFontSizeLabel")
    String appearanceEditorFontSizeLabel();

    /**
     * Translated "Editor theme:".
     *
     * @return translated "Editor theme:"
     */
    @DefaultMessage("Editor theme:")
    @Key("appearanceEditorThemeLabel")
    String appearanceEditorThemeLabel();

    /**
     * Translated "Add...".
     *
     * @return translated "Add..."
     */
    @DefaultMessage("Add...")
    @Key("addThemeButtonLabel")
    String addThemeButtonLabel();

    /**
     * Translated "Theme Files (*.tmTheme *.rstheme)".
     *
     * @return translated "Theme Files (*.tmTheme *.rstheme)"
     */
    @DefaultMessage("Theme Files (*.tmTheme *.rstheme)")
    @Key("addThemeButtonCaption")
    String addThemeButtonCaption();

    /**
     * Translated "Remove".
     *
     * @return translated "Remove"
     */
    @DefaultMessage("Remove")
    @Key("removeThemeButtonLabel")
    String removeThemeButtonLabel();

    /**
     * Translated "Converting a tmTheme to an rstheme".
     *
     * @return translated "Converting a tmTheme to an rstheme"
     */
    @DefaultMessage("Converting a tmTheme to an rstheme")
    @Key("addThemeUserActionLabel")
    String addThemeUserActionLabel();

    /**
     * Translated "The active theme "{0}" could not be found. It''s possible it was removed outside the context of RStudio. Switching to the {1} default theme: "".
     *
     * @return translated "The active theme "{0}" could not be found. It''s possible it was removed outside the context of RStudio. Switching to the {1} default theme: ""
     */
    @DefaultMessage("The active theme \"{0}\" could not be found. It''s possible it was removed outside the context of RStudio. Switching to the {1} default theme: \"")
    @Key("setThemeWarningMessage")
    String setThemeWarningMessage(String name, String currentTheme);

    /**
     * Translated "dark".
     *
     * @return translated "dark"
     */
    @DefaultMessage("dark")
    @Key("themeWarningMessageDarkLabel")
    String themeWarningMessageDarkLabel();

    /**
     * Translated "light".
     *
     * @return translated "light"
     */
    @DefaultMessage("light")
    @Key("themeWarningMessageLightLabel")
    String themeWarningMessageLightLabel();

    /**
     * Translated "A theme file with the same name, ''{0}'', already exists. Adding the theme will cause the existing file to be overwritten. Would you like to add the theme anyway?".
     *
     * @return translated "A theme file with the same name, ''{0}'', already exists. Adding the theme will cause the existing file to be overwritten. Would you like to add the theme anyway?"
     */
    @DefaultMessage("A theme file with the same name, ''{0}'', already exists. Adding the theme will cause the existing file to be overwritten. Would you like to add the theme anyway?")
    @Key("showThemeExistsDialogLabel")
    String showThemeExistsDialogLabel(String inputFileName);

    /**
     * Translated "Theme File Already Exists".
     *
     * @return translated "Theme File Already Exists"
     */
    @DefaultMessage("Theme File Already Exists")
    @Key("globalDisplayThemeExistsCaption")
    String globalDisplayThemeExistsCaption();

    /**
     * Translated "Unable to add the theme ''".
     *
     * @return translated "Unable to add the theme ''"
     */
    @DefaultMessage("Unable to add the theme ''")
    @Key("cantAddThemeMessage")
    String cantAddThemeMessage();


    /**
     * Translated "''. The following error occurred: ".
     *
     * @return translated "''. The following error occurred: "
     */
    @DefaultMessage("''. The following error occurred: ")
    @Key("cantAddThemeErrorCaption")
    String cantAddThemeErrorCaption();

    /**
     * Translated "Failed to Add Theme".
     *
     * @return translated "Failed to Add Theme"
     */
    @DefaultMessage("Failed to Add Theme")
    @Key("cantAddThemeGlobalMessage")
    String cantAddThemeGlobalMessage();

    /**
     * Translated "Unable to remove the theme ''{0}'': {1}".
     *
     * @return translated "Unable to remove the theme ''{0}'': {1}"
     */
    @DefaultMessage("Unable to remove the theme ''{0}'': {1}")
    @Key("showCantRemoveThemeDialogMessage")
    String showCantRemoveThemeDialogMessage(String themeName, String errorMessage);

    /**
     * Translated "Failed to Remove Theme".
     *
     * @return translated "Failed to Remove Theme"
     */
    @DefaultMessage("Failed to Remove Theme")
    @Key("showCantRemoveErrorMessage")
    String showCantRemoveErrorMessage();

    /**
     * Translated "The theme "{0}" cannot be removed because it is currently in use. To delete this theme, please change the active theme and retry.".
     *
     * @return translated "The theme "{0}" cannot be removed because it is currently in use. To delete this theme, please change the active theme and retry."
     */
    @DefaultMessage("The theme \"{0}\" cannot be removed because it is currently in use. To delete this theme, please change the active theme and retry.")
    @Key("showCantRemoveActiveThemeDialog")
    String showCantRemoveActiveThemeDialog(String themeName);

    /**
     * Translated "Cannot Remove Active Theme".
     *
     * @return translated "Cannot Remove Active Theme"
     */
    @DefaultMessage("Cannot Remove Active Theme")
    @Key("showCantRemoveThemeCaption")
    String showCantRemoveThemeCaption();

    /**
     * Translated "Taking this action will delete the theme "{0}" and cannot be undone. Are you sure you wish to continue?".
     *
     * @return translated "Taking this action will delete the theme "{0}" and cannot be undone. Are you sure you wish to continue?"
     */
    @DefaultMessage("Taking this action will delete the theme \"{0}\" and cannot be undone. Are you sure you wish to continue?")
    @Key("showRemoveThemeWarningMessage")
    String showRemoveThemeWarningMessage(String themeName);

    /**
     * Translated "Remove Theme".
     *
     * @return translated "Remove Theme"
     */
    @DefaultMessage("Remove Theme")
    @Key("showRemoveThemeGlobalMessage")
    String showRemoveThemeGlobalMessage();

    /**
     * Translated "There is an existing theme with the same name as the new theme in the current location. Would you like remove the existing theme, "{0}", and add the new theme?".
     *
     * @return translated "There is an existing theme with the same name as the new theme in the current location. Would you like remove the existing theme, "{0}", and add the new theme?"
     */
    @DefaultMessage("There is an existing theme with the same name as the new theme in the current location. Would you like remove the existing theme, \"{0}\", and add the new theme?")
    @Key("showDuplicateThemeErrorMessage")
    String showDuplicateThemeErrorMessage(String themeName);

    /**
     * Translated "Duplicate Theme In Same Location".
     *
     * @return translated "Duplicate Theme In Same Location"
     */
    @DefaultMessage("Duplicate Theme In Same Location")
    @Key("showDuplicateThemeDuplicateGlobalMessage")
    String showDuplicateThemeDuplicateGlobalMessage();

    /**
     * Translated "There is an existing theme with the same name as the new theme, ".
     *
     * @return translated "There is an existing theme with the same name as the new theme, "
     */
    @DefaultMessage("There is an existing theme with the same name as the new theme, \"{0}\" in another location. The existing theme will be hidden but not removed. Removing the new theme later will un-hide the existing theme. Would you like to continue?")
    @Key("showDuplicateThemeWarningMessage")
    String showDuplicateThemeWarningMessage(String themeName);

    /**
     * Translated "Duplicate Theme In Another Location".
     *
     * @return translated "Duplicate Theme In Another Location"
     */
    @DefaultMessage("Duplicate Theme In Another Location")
    @Key("showDuplicateThemeGlobalMessage")
    String showDuplicateThemeGlobalMessage();

    /**
     * Translated "Appearance".
     *
     * @return translated "Appearance"
     */
    @DefaultMessage("Appearance")
    @Key("appearanceLabel")
    String appearanceLabel();

    /**
     * Translated "Editor font:".
     *
     * @return translated "Editor font:"
     */
    @DefaultMessage("Editor font:")
    @Key("editorFontLabel")
    String editorFontLabel();

    /**
     * Translated "PDF Generation".
     *
     * @return translated "PDF Generation"
     */
    @DefaultMessage("PDF Generation")
    @Key("headerPDFGenerationLabel")
    String headerPDFGenerationLabel();

    /**
     * Translated "NOTE: The Rnw weave and LaTeX compilation options are also set on a per-project (and optionally per-file) basis. Click the help icons above for more details.".
     *
     * @return translated "NOTE: The Rnw weave and LaTeX compilation options are also set on a per-project (and optionally per-file) basis. Click the help icons above for more details."
     */
    @DefaultMessage("NOTE: The Rnw weave and LaTeX compilation options are also set on a per-project (and optionally per-file) basis. Click the help icons above for more details.")
    @Key("perProjectNoteLabel")
    String perProjectNoteLabel();

    /**
     * Translated "LaTeX Editing and Compilation".
     *
     * @return translated "LaTeX Editing and Compilation"
     */
    @DefaultMessage("LaTeX Editing and Compilation")
    @Key("perProjectHeaderLabel")
    String perProjectHeaderLabel();

    /**
     * Translated "Use tinytex when compiling .tex files".
     *
     * @return translated "Use tinytex when compiling .tex files"
     */
    @DefaultMessage("Use tinytex when compiling .tex files")
    @Key("chkUseTinytexLabel")
    String chkUseTinytexLabel();

    /**
     * Translated "Clean auxiliary output after compile".
     *
     * @return translated "Clean auxiliary output after compile"
     */
    @DefaultMessage("Clean auxiliary output after compile")
    @Key("chkCleanTexi2DviOutputLabel")
    String chkCleanTexi2DviOutputLabel();

    /**
     * Translated "Enable shell escape commands".
     *
     * @return translated "Enable shell escape commands"
     */
    @DefaultMessage("Enable shell escape commands")
    @Key("chkEnableShellEscapeLabel")
    String chkEnableShellEscapeLabel();

    /**
     * Translated "Insert numbered sections and subsections".
     *
     * @return translated "Insert numbered sections and subsections"
     */
    @DefaultMessage("Insert numbered sections and subsections")
    @Key("insertNumberedLatexSectionsLabel")
    String insertNumberedLatexSectionsLabel();

    /**
     * Translated "PDF Preview".
     *
     * @return translated "PDF Preview"
     */
    @DefaultMessage("PDF Preview")
    @Key("previewingOptionsHeaderLabel")
    String previewingOptionsHeaderLabel();

    /**
     * Translated "Always enable Rnw concordance (required for synctex)".
     *
     * @return translated "Always enable Rnw concordance (required for synctex)"
     */
    @DefaultMessage("Always enable Rnw concordance (required for synctex)")
    @Key("alwaysEnableRnwConcordanceLabel")
    String alwaysEnableRnwConcordanceLabel();

    /**
     * Translated "Preview PDF after compile using:".
     *
     * @return translated "Preview PDF after compile using:"
     */
    @DefaultMessage("Preview PDF after compile using:")
    @Key("pdfPreviewSelectWidgetLabel")
    String pdfPreviewSelectWidgetLabel();

    /**
     * Translated "Help on previewing PDF files".
     *
     * @return translated "Help on previewing PDF files"
     */
    @DefaultMessage("Help on previewing PDF files")
    @Key("pdfPreviewHelpButtonTitle")
    String pdfPreviewHelpButtonTitle();

    /**
     * Translated "Sweave".
     *
     * @return translated "Sweave"
     */
    @DefaultMessage("Sweave")
    @Key("preferencesPaneTitle")
    String preferencesPaneTitle();

    /**
     * Translated "(No Preview)".
     *
     * @return translated "(No Preview)"
     */
    @DefaultMessage("(No Preview)")
    @Key("pdfNoPreviewOption")
    String pdfNoPreviewOption();

    /**
     * Translated "(Recommended)".
     *
     * @return translated "(Recommended)"
     */
    @DefaultMessage("(Recommended)")
    @Key("pdfPreviewSumatraOption")
    String pdfPreviewSumatraOption();

    /**
     * Translated "RStudio Viewer".
     *
     * @return translated "RStudio Viewer"
     */
    @DefaultMessage("RStudio Viewer")
    @Key("pdfPreviewRStudioViewerOption")
    String pdfPreviewRStudioViewerOption();

    /**
     * Translated "System Viewer".
     *
     * @return translated "System Viewer"
     */
    @DefaultMessage("System Viewer")
    @Key("pdfPreviewSystemViewerOption")
    String pdfPreviewSystemViewerOption();

    /**
     * Translated "Display".
     *
     * @return translated "Display"
     */
    @DefaultMessage("Display")
    @Key("consoleDisplayLabel")
    String consoleDisplayLabel();

    /**
     * Translated "Show syntax highlighting in console input".
     *
     * @return translated "Show syntax highlighting in console input"
     */
    @DefaultMessage("Show syntax highlighting in console input")
    @Key("consoleSyntaxHighlightingLabel")
    String consoleSyntaxHighlightingLabel();

    /**
     * Translated "Different color for error or message output (requires restart)".
     *
     * @return translated "Different color for error or message output (requires restart)"
     */
    @DefaultMessage("Different color for error or message output (requires restart)")
    @Key("consoleDifferentColorLabel")
    String consoleDifferentColorLabel();

    /**
     * Translated "Limit visible console output (requires restart)".
     *
     * @return translated "Limit visible console output (requires restart)"
     */
    @DefaultMessage("Limit visible console output (requires restart)")
    @Key("consoleLimitVariableLabel")
    String consoleLimitVariableLabel();

    /**
     * Translated "Limit output line length to:".
     *
     * @return translated "Limit output line length to:"
     */
    @DefaultMessage("Limit output line length to:")
    @Key("consoleLimitOutputLengthLabel")
    String consoleLimitOutputLengthLabel();

    /**
     * Translated "ANSI Escape Codes:".
     *
     * @return translated "ANSI Escape Codes:"
     */
    @DefaultMessage("ANSI Escape Codes:")
    @Key("consoleANSIEscapeCodesLabel")
    String consoleANSIEscapeCodesLabel();

    /**
     * Translated "Show ANSI colors".
     *
     * @return translated "Show ANSI colors"
     */
    @DefaultMessage("Show ANSI colors")
    @Key("consoleColorModeANSIOption")
    String consoleColorModeANSIOption();

    /**
     * Translated "Remove ANSI codes".
     *
     * @return translated "Remove ANSI codes"
     */
    @DefaultMessage("Remove ANSI codes")
    @Key("consoleColorModeRemoveANSIOption")
    String consoleColorModeRemoveANSIOption();

    /**
     * Translated "Ignore ANSI codes (1.0 behavior)".
     *
     * @return translated "Ignore ANSI codes (1.0 behavior)"
     */
    @DefaultMessage("Ignore ANSI codes (1.0 behavior)")
    @Key("consoleColorModeIgnoreANSIOption")
    String consoleColorModeIgnoreANSIOption();

    /**
     * Translated "Console".
     *
     * @return translated "Console"
     */
    @DefaultMessage("Console")
    @Key("consoleLabel")
    String consoleLabel();

    /**
     * Translated "Debugging".
     *
     * @return translated "Debugging"
     */
    @DefaultMessage("Debugging")
    @Key("debuggingHeaderLabel")
    String debuggingHeaderLabel();

    /**
     * Translated "Automatically expand tracebacks in error inspector".
     *
     * @return translated "Automatically expand tracebacks in error inspector"
     */
    @DefaultMessage("Automatically expand tracebacks in error inspector")
    @Key("debuggingExpandTracebacksLabel")
    String debuggingExpandTracebacksLabel();

    /**
     * Translated "Other".
     *
     * @return translated "Other"
     */
    @DefaultMessage("Other")
    @Key("otherHeaderCaption")
    String otherHeaderCaption();

    /**
     * Translated "Double-click to select words".
     *
     * @return translated "Double-click to select words"
     */
    @DefaultMessage("Double-click to select words")
    @Key("otherDoubleClickLabel")
    String otherDoubleClickLabel();

    /**
     * Translated "Warn when automatic session suspension is paused".
     *
     * @return translated "Warn when automatic session suspension is paused"
     */
    @DefaultMessage("Warn when automatic session suspension is paused")
    @Key("WarnAutomaticSuspensionPaused")
    String warnAutoSuspendPausedLabel();

    /**
     * Translated "Number of seconds to delay warning".
     *
     * @return translated "Number of seconds to delay warning"
     */
    @DefaultMessage("Number of seconds to delay warning")
    @Key("numberOfSecondsToDelayWarning")
    String numSecondsToDelayWarningLabel();

    /**
     * Translated "R Sessions".
     *
     * @return translated "R Sessions"
     */
    @DefaultMessage("R Sessions")
    @Key("rSessionsTitle")
    String rSessionsTitle();

    /**
     * Translated "R version".
     *
     * @return translated "R version"
     */
    @DefaultMessage("R version")
    @Key("rVersionTitle")
    String rVersionTitle();

    /**
     * Translated "Change...".
     *
     * @return translated "Change..."
     */
    @DefaultMessage("Change...")
    @Key("rVersionChangeTitle")
    String rVersionChangeTitle();

    /**
     * Translated "Change R Version".
     *
     * @return translated "Change R Version"
     */
    @DefaultMessage("Change R Version")
    @Key("rChangeVersionMessage")
    String rChangeVersionMessage();

    /**
     * Translated "You need to quit and re-open RStudio in order for this change to take effect.".
     *
     * @return translated "You need to quit and re-open RStudio in order for this change to take effect."
     */
    @DefaultMessage("You need to quit and re-open RStudio in order for this change to take effect.")
    @Key("rQuitReOpenMessage")
    String rQuitReOpenMessage();

    /**
     * Translated "Loading...".
     *
     * @return translated "Loading..."
     */
    @DefaultMessage("Loading...")
    @Key("rVersionLoadingText")
    String rVersionLoadingText();

    /**
     * Translated "Restore last used R version for projects".
     *
     * @return translated "Restore last used R version for projects"
     */
    @DefaultMessage("Restore last used R version for projects")
    @Key("rRestoreLabel")
    String rRestoreLabel();

    /**
     * Translated "working directory (when not in a project):".
     *
     * @return translated "working directory (when not in a project):"
     */
    @DefaultMessage("Default working directory (when not in a project):")
    @Key("rDefaultDirectoryTitle")
    String rDefaultDirectoryTitle();

    /**
     * Translated "Restore most recently opened project at startup".
     *
     * @return translated "Restore most recently opened project at startup"
     */
    @DefaultMessage("Restore most recently opened project at startup")
    @Key("rRestorePreviousTitle")
    String rRestorePreviousTitle();

    /**
     * Translated "Restore previously open source documents at startup".
     *
     * @return translated "Restore previously open source documents at startup"
     */
    @DefaultMessage("Restore previously open source documents at startup")
    @Key("rRestorePreviousOpenTitle")
    String rRestorePreviousOpenTitle();

    /**
     * Translated "Run Rprofile when resuming suspended session".
     *
     * @return translated "Run Rprofile when resuming suspended session"
     */
    @DefaultMessage("Run Rprofile when resuming suspended session")
    @Key("rRunProfileTitle")
    String rRunProfileTitle();

    /**
     * Translated "Workspace".
     *
     * @return translated "Workspace"
     */
    @DefaultMessage("Workspace")
    @Key("workspaceCaption")
    String workspaceCaption();

    /**
     * Translated "Restore .RData into workspace at startup".
     *
     * @return translated "Restore .RData into workspace at startup"
     */
    @DefaultMessage("Restore .RData into workspace at startup")
    @Key("workspaceLabel")
    String workspaceLabel();

    /**
     * Translated "Save workspace to .RData on exit:".
     *
     * @return translated "Save workspace to .RData on exit:"
     */
    @DefaultMessage("Save workspace to .RData on exit:")
    @Key("saveWorkSpaceLabel")
    String saveWorkSpaceLabel();

    /**
     * Translated "Always".
     *
     * @return translated "Always"
     */
    @DefaultMessage("Always")
    @Key("saveWorkAlways")
    String saveWorkAlways();

    /**
     * Translated "Never".
     *
     * @return translated "Never"
     */
    @DefaultMessage("Never")
    @Key("saveWorkNever")
    String saveWorkNever();

    /**
     * Translated "Ask".
     *
     * @return translated "Ask"
     */
    @DefaultMessage("Ask")
    @Key("saveWorkAsk")
    String saveWorkAsk();

    /**
     * Translated "History".
     *
     * @return translated "History"
     */
    @DefaultMessage("History")
    @Key("historyCaption")
    String historyCaption();

    /**
     * Translated "Always save history (even when not saving .RData)".
     *
     * @return translated "Always save history (even when not saving .RData)"
     */
    @DefaultMessage("Always save history (even when not saving .RData)")
    @Key("alwaysSaveHistoryLabel")
    String alwaysSaveHistoryLabel();

    /**
     * Translated "Remove duplicate entries in history".
     *
     * @return translated "Remove duplicate entries in history"
     */
    @DefaultMessage("Remove duplicate entries in history")
    @Key("removeDuplicatesLabel")
    String removeDuplicatesLabel();

    /**
     * Translated "Other".
     *
     * @return translated "Other"
     */
    @DefaultMessage("Other")
    @Key("otherCaption")
    String otherCaption();

    /**
     * Translated "Wrap around when navigating to previous/next tab".
     *
     * @return translated "Wrap around when navigating to previous/next tab"
     */
    @DefaultMessage("Wrap around when navigating to previous/next tab")
    @Key("otherWrapAroundLabel")
    String otherWrapAroundLabel();

    /**
     * Translated "Automatically notify me of updates to RStudio".
     *
     * @return translated "Automatically notify me of updates to RStudio"
     */
    @DefaultMessage("Automatically notify me of updates to RStudio")
    @Key("otherNotifyMeLabel")
    String otherNotifyMeLabel();

    /**
     * Translated "Send automated crash reports to RStudio".
     *
     * @return translated "Send automated crash reports to RStudio"
     */
    @DefaultMessage("Send automated crash reports to RStudio")
    @Key("otherSendReportsLabel")
    String otherSendReportsLabel();

    /**
     * Translated "Graphics Device".
     *
     * @return translated "Graphics Device"
     */
    @DefaultMessage("Graphics Device")
    @Key("graphicsDeviceCaption")
    String graphicsDeviceCaption();

    /**
     * Translated "Antialiasing:".
     *
     * @return translated "Antialiasing:"
     */
    @DefaultMessage("Antialiasing:")
    @Key("graphicsAntialiasingLabel")
    String graphicsAntialiasingLabel();

    /**
     * Translated "(Default):".
     *
     * @return translated "(Default):"
     */
    @DefaultMessage("(Default)")
    @Key("antialiasingDefaultOption")
    String antialiasingDefaultOption();

    /**
     * Translated "None".
     *
     * @return translated "None"
     */
    @DefaultMessage("None")
    @Key("antialiasingNoneOption")
    String antialiasingNoneOption();

    /**
     * Translated "Gray".
     *
     * @return translated "Gray"
     */
    @DefaultMessage("Gray")
    @Key("antialiasingGrayOption")
    String antialiasingGrayOption();

    /**
     * Translated "Subpixel".
     *
     * @return translated "Subpixel"
     */
    @DefaultMessage("Subpixel")
    @Key("antialiasingSubpixelOption")
    String antialiasingSubpixelOption();

    /**
     * Translated "Show server home page:".
     *
     * @return translated "Show server home page:"
     */
    @DefaultMessage("Show server home page:")
    @Key("serverHomePageLabel")
    String serverHomePageLabel();

    /**
     * Translated "Multiple active sessions".
     *
     * @return translated "Multiple active sessions"
     */
    @DefaultMessage("Multiple active sessions")
    @Key("serverHomePageActiveSessionsOption")
    String serverHomePageActiveSessionsOption();

    /**
     * Translated "Always".
     *
     * @return translated "Always"
     */
    @DefaultMessage("Always")
    @Key("serverHomePageAlwaysOption")
    String serverHomePageAlwaysOption();

    /**
     * Translated "Never".
     *
     * @return translated "Never"
     */
    @DefaultMessage("Never")
    @Key("serverHomePageNeverOption")
    String serverHomePageNeverOption();

    /**
     * Translated "Re-use idle sessions for project links".
     *
     * @return translated "Re-use idle sessions for project links"
     */
    @DefaultMessage("Re-use idle sessions for project links")
    @Key("reUseIdleSessionLabel")
    String reUseIdleSessionLabel();

    /**
     * Translated "Home Page".
     *
     * @return translated "Home Page"
     */
    @DefaultMessage("Home Page")
    @Key("desktopCaption")
    String desktopCaption();

    /**
     * Translated "Debugging".
     *
     * @return translated "Debugging"
     */
    @DefaultMessage("Debugging")
    @Key("advancedDebuggingCaption")
    String advancedDebuggingCaption();

    /**
     * Translated "Use debug error handler only when my code contains errors".
     *
     * @return translated "Use debug error handler only when my code contains errors"
     */
    @DefaultMessage("Use debug error handler only when my code contains errors")
    @Key("advancedDebuggingLabel")
    String advancedDebuggingLabel();

    /**
     * Translated "OS Integration".
     *
     * @return translated "OS Integration"
     */
    @DefaultMessage("OS Integration")
    @Key("advancedOsIntegrationCaption")
    String advancedOsIntegrationCaption();

    /**
     * Translated "Rendering engine:".
     *
     * @return translated "Rendering engine:"
     */
    @DefaultMessage("Rendering engine:")
    @Key("advancedRenderingEngineLabel")
    String advancedRenderingEngineLabel();

    /**
     * Translated "Auto-detect (recommended)".
     *
     * @return translated "Auto-detect (recommended)"
     */
    @DefaultMessage("Auto-detect (recommended)")
    @Key("renderingEngineAutoDetectOption")
    String renderingEngineAutoDetectOption();

    /**
     * Translated "Desktop OpenGL".
     *
     * @return translated "Desktop OpenGL"
     */
    @DefaultMessage("Desktop OpenGL")
    @Key("renderingEngineDesktopOption")
    String renderingEngineDesktopOption();

    /**
     * Translated "OpenGL for Embedded Systems".
     *
     * @return translated "OpenGL for Embedded Systems"
     */
    @DefaultMessage("OpenGL for Embedded Systems")
    @Key("renderingEngineLinuxDesktopOption")
    String renderingEngineLinuxDesktopOption();

    /**
     * Translated "Software".
     *
     * @return translated "Software"
     */
    @DefaultMessage("Software")
    @Key("renderingEngineSoftwareOption")
    String renderingEngineSoftwareOption();

    /**
     * Translated "Use GPU exclusion list (recommended)".
     *
     * @return translated "Use GPU exclusion list (recommended)"
     */
    @DefaultMessage("Use GPU exclusion list (recommended)")
    @Key("useGpuExclusionListLabel")
    String useGpuExclusionListLabel();

    /**
     * Translated "Use GPU driver bug workarounds (recommended)".
     *
     * @return translated "Use GPU driver bug workarounds (recommended)"
     */
    @DefaultMessage("Use GPU driver bug workarounds (recommended)")
    @Key("useGpuDriverBugWorkaroundsLabel")
    String useGpuDriverBugWorkaroundsLabel();

    /**
     * Translated "Enable X11 clipboard monitoring".
     *
     * @return translated "Enable X11 clipboard monitoring"
     */
    @DefaultMessage("Enable X11 clipboard monitoring")
    @Key("clipboardMonitoringLabel")
    String clipboardMonitoringLabel();

    /**
     * Translated "Other".
     *
     * @return translated "Other"
     */
    @DefaultMessage("Other")
    @Key("otherLabel")
    String otherLabel();

    /**
     * Translated "Experimental Features".
     *
     * @return translated "Experimental Features"
     */
    @DefaultMessage("Experimental Features")
    @Key("experimentalLabel")
    String experimentalLabel();

    /**
     * Translated "English".
     *
     * @return translated "English"
     */
    @DefaultMessage("English")
    @Key("englishLabel")
    String englishLabel();

    /**
     * Translated "French (Français)".
     *
     * @return translated "French (Français)"
     */
    @DefaultMessage("French (Français)")
    @Key("frenchLabel")
    String frenchLabel();

    /**
     * Translated "Show .Last.value in environment listing".
     *
     * @return translated "Show .Last.value in environment listing"
     */
    @DefaultMessage("Show .Last.value in environment listing")
    @Key("otherShowLastDotValueLabel")
    String otherShowLastDotValueLabel();

    /**
     * Translated "Help panel font size:".
     *
     * @return translated "Help panel font size:"
     */
    @DefaultMessage("Help panel font size:")
    @Key("helpFontSizeLabel")
    String helpFontSizeLabel();

    /**
     * Translated "General".
     *
     * @return "General"
     */
    @DefaultMessage("General")
    @Key("generalTabListLabel")
    String generalTablistLabel();

    /**
     * Translated "Basic".
     *
     * @return "Basic"
     */
    @DefaultMessage("Basic")
    @Key("generalTabListBasicOption")
    String generalTablListBasicOption();

    /**
     * Translated "Graphics".
     *
     * @return "Graphics"
     */
    @DefaultMessage("Graphics")
    @Key("generalTabListGraphicsOption")
    String generalTablListGraphicsOption();

    /**
     * Translated "Advanced".
     *
     * @return "Advanced"
     */
    @DefaultMessage("Advanced")
    @Key("generalTabListAdvancedOption")
    String generalTabListAdvancedOption();

    /**
     * Translated " (Default)".
     *
     * @return " (Default)"
     */
    @DefaultMessage("(Default)")
    @Key("graphicsBackEndDefaultOption")
    String graphicsBackEndDefaultOption();

    /**
     * Translated "Quartz".
     *
     * @return "Quartz"
     */
    @DefaultMessage("Quartz")
    @Key("graphicsBackEndQuartzOption")
    String graphicsBackEndQuartzOption();

    /**
     * Translated "Windows".
     *
     * @return "Windows"
     */
    @DefaultMessage("Windows")
    @Key("graphicsBackEndWindowsOption")
    String graphicsBackEndWindowsOption();

    /**
     * Translated "Cairo".
     *
     * @return "Cairo"
     */
    @DefaultMessage("Cairo")
    @Key("graphicsBackEndCairoOption")
    String graphicsBackEndCairoOption();

    /**
     * Translated "Cairo PNG".
     *
     * @return "Cairo PNG"
     */
    @DefaultMessage("Cairo PNG")
    @Key("graphicsBackEndCairoPNGOption")
    String graphicsBackEndCairoPNGOption();

    /**
     * Translated "AGG".
     *
     * @return "AGG"
     */
    @DefaultMessage("AGG")
    @Key("graphicsBackEndAGGOption")
    String graphicsBackEndAGGOption();

    /**
     * Translated "Backend:".
     *
     * @return "Backend:"
     */
    @DefaultMessage("Backend:")
    @Key("graphicsBackendLabel")
    String graphicsBackendLabel();

    /**
     * Translated "Using the AGG renderer".
     *
     * @return "Using the AGG renderer"
     */
    @DefaultMessage("Using the AGG renderer")
    @Key("graphicsBackendUserAction")
    String graphicsBackendUserAction();

    /**
     * Translated "Browse...".
     *
     * @return "Browse..."
     */
    @DefaultMessage("Browse...")
    @Key("browseLabel")
    String browseLabel();

    /**
     * Translated "Choose Directory".
     *
     * @return "Choose Directory"
     */
    @DefaultMessage("Choose Directory")
    @Key("directoryLabel")
    String directoryLabel();
    /**
     * Translated "Code".
     *
     * @return translated "Code"
     */
    @DefaultMessage("Code")
    @Key("codePaneLabel")
    String codePaneLabel();

    /**
     * Translated "Package Management".
     *
     * @return translated "Package Management"
     */
    @DefaultMessage("Package Management")
    @Key("packageManagementTitle")
    String packageManagementTitle();

    /**
     * Translated "CRAN repositories modified outside package preferences.".
     *
     * @return translated "CRAN repositories modified outside package preferences."
     */
    @DefaultMessage("CRAN repositories modified outside package preferences.")
    @Key("packagesInfoBarText")
    String packagesInfoBarText();

    /**
     * Translated "Primary CRAN repository:".
     *
     * @return translated "Primary CRAN repository:"
     */
    @DefaultMessage("Primary CRAN repository:")
    @Key("cranMirrorTextBoxTitle")
    String cranMirrorTextBoxTitle();

    /**
     * Translated "Change...".
     *
     * @return translated "Change..."
     */
    @DefaultMessage("Change...")
    @Key("cranMirrorChangeLabel")
    String cranMirrorChangeLabel();

    /**
     * Translated "Secondary repositories:".
     *
     * @return translated "Secondary repositories:"
     */
    @DefaultMessage("Secondary repositories:")
    @Key("secondaryReposTitle")
    String secondaryReposTitle();

    /**
     * Translated "Enable packages pane".
     *
     * @return translated "Enable packages pane"
     */
    @DefaultMessage("Enable packages pane")
    @Key("chkEnablePackagesTitle")
    String chkEnablePackagesTitle();

    /**
     * Translated "Use secure download method for HTTP".
     *
     * @return translated "Use secure download method for HTTP"
     */
    @DefaultMessage("Use secure download method for HTTP")
    @Key("useSecurePackageDownloadTitle")
    String useSecurePackageDownloadTitle();

    /**
     * Translated "Help on secure package downloads for R".
     *
     * @return translated "Help on secure package downloads for R"
     */
    @DefaultMessage("Help on secure package downloads for R")
    @Key("useSecurePackageTitle")
    String useSecurePackageTitle();

    /**
     * Translated "Use Internet Explorer library/proxy for HTTP".
     *
     * @return translated "Use Internet Explorer library/proxy for HTTP"
     */
    @DefaultMessage("Use Internet Explorer library/proxy for HTTP")
    @Key("useInternetTitle")
    String useInternetTitle();

    /**
     * Translated "Managing Packages".
     *
     * @return translated "Managing Packages"
     */
    @DefaultMessage("Managing Packages")
    @Key("managePackagesTitle")
    String managePackagesTitle();

    /**
     * Translated "Package Development".
     *
     * @return translated "Package Development"
     */
    @DefaultMessage("Package Development")
    @Key("developmentTitle")
    String developmentTitle();

    /**
     * Translated "C/C++ Development".
     *
     * @return translated "C/C++ Development"
     */
    @DefaultMessage("C/C++ Development")
    @Key("cppDevelopmentTitle")
    String cppDevelopmentTitle();

    /**
     * Translated "Use devtools package functions if available".
     *
     * @return translated "Use devtools package functions if available"
     */
    @DefaultMessage("Use devtools package functions if available")
    @Key("useDevtoolsLabel")
    String useDevtoolsLabel();

    /**
     * Translated "Save all files prior to building packages".
     *
     * @return translated "Save all files prior to building packages"
     */
    @DefaultMessage("Save all files prior to building packages")
    @Key("developmentSaveLabel")
    String developmentSaveLabel();

    /**
     * Translated "Automatically navigate editor to build errors".
     *
     * @return translated "Automatically navigate editor to build errors"
     */
    @DefaultMessage("Automatically navigate editor to build errors")
    @Key("developmentNavigateLabel")
    String developmentNavigateLabel();

    /**
     * Translated "Hide object files in package src directory".
     *
     * @return translated "Hide object files in package src directory"
     */
    @DefaultMessage("Hide object files in package src directory")
    @Key("developmentHideLabel")
    String developmentHideLabel();

    /**
     * Translated "Cleanup output after successful R CMD check".
     *
     * @return translated "Cleanup output after successful R CMD check"
     */
    @DefaultMessage("Cleanup output after successful R CMD check")
    @Key("developmentCleanupLabel")
    String developmentCleanupLabel();

    /**
     * Translated "View Rcheck directory after failed R CMD check".
     *
     * @return translated "View Rcheck directory after failed R CMD check"
     */
    @DefaultMessage("View Rcheck directory after failed R CMD check")
    @Key("developmentViewLabel")
    String developmentViewLabel();

    /**
     * Translated "C++ template".
     *
     * @return translated "C++ template"
     */
    @DefaultMessage("C++ template")
    @Key("developmentCppTemplate")
    String developmentCppTemplate();

    /**
     * Translated "empty".
     *
     * @return translated "empty"
     */
    @DefaultMessage("empty")
    @Key("developmentEmptyLabel")
    String developmentEmptyLabel();

    /**
     * Translated "Always use LF line-endings in Unix Makefiles".
     *
     * @return translated "Always use LF line-endings in Unix Makefiles"
     */
    @DefaultMessage("Always use LF line-endings in Unix Makefiles")
    @Key("developmentUseLFLabel")
    String developmentUseLFLabel();

    /**
     * Translated "Packages".
     *
     * @return translated "Packages"
     */
    @DefaultMessage("Packages")
    @Key("tabPackagesPanelTitle")
    String tabPackagesPanelTitle();

    /**
     * Translated "Management".
     *
     * @return translated "Management"
     */
    @DefaultMessage("Management")
    @Key("managementPanelTitle")
    String managementPanelTitle();

    /**
     * Translated "Development".
     *
     * @return translated "Development"
     */
    @DefaultMessage("Development")
    @Key("developmentManagementPanelTitle")
    String developmentManagementPanelTitle();

    /**
     * Translated "C / C++".
     *
     * @return translated "C / C++"
     */
    @DefaultMessage("C / C++")
    @Key("C / C++")
    String cppPanelTitle();

    /**
     * Translated "Restart R Required".
     *
     * @return translated "Restart R Required"
     */
    @DefaultMessage("Restart R Required")
    @Key("cranMirrorTextBoxRestartCaption")
    String cranMirrorTextBoxRestartCaption();

    /**
     * Translated "You must restart your R session for this setting to take effect.".
     *
     * @return translated "You must restart your R session for this setting to take effect."
     */
    @DefaultMessage("You must restart your R session for this setting to take effect.")
    @Key("cranMirrorTextBoxRestartMessage")
    String cranMirrorTextBoxRestartMessage();

    /**
     * Translated "Retrieving list of CRAN mirrors...".
     *
     * @return translated "Retrieving list of CRAN mirrors..."
     */
    @DefaultMessage("Retrieving list of CRAN mirrors...")
    @Key("chooseMirrorDialogMessage")
    String chooseMirrorDialogMessage();

    /**
     * Translated "Error".
     *
     * @return translated "Error"
     */
    @DefaultMessage("Error")
    @Key("showDisconnectErrorCaption")
    String showDisconnectErrorCaption();

    /**
     * Translated "Please select a CRAN Mirror".
     *
     * @return translated "Please select a CRAN Mirror"
     */
    @DefaultMessage("Please select a CRAN Mirror")
    @Key("showDisconnectErrorMessage")
    String showDisconnectErrorMessage();

    /**
     * Translated "Validating CRAN repository...".
     *
     * @return translated "Validating CRAN repository..."
     */
    @DefaultMessage("Validating CRAN repository...")
    @Key("progressIndicatorMessage")
    String progressIndicatorMessage();

    /**
     * Translated "The given URL does not appear to be a valid CRAN repository".
     *
     * @return translated "The given URL does not appear to be a valid CRAN repository"
     */
    @DefaultMessage("The given URL does not appear to be a valid CRAN repository")
    @Key("progressIndicatorError")
    String progressIndicatorError();

    /**
     * Translated "Custom:".
     *
     * @return translated "Custom:"
     */
    @DefaultMessage("Custom:")
    @Key("customLabel")
    String customLabel();

    /**
     * Translated "CRAN Mirrors:".
     *
     * @return translated "CRAN Mirrors:"
     */
    @DefaultMessage("CRAN Mirrors:")
    @Key("mirrorsLabel")
    String mirrorsLabel();

    /**
     * Translated "Choose Primary Repository".
     *
     * @return translated "Choose Primary Repository"
     */
    @DefaultMessage("Choose Primary Repository")
    @Key("headerLabel")
    String headerLabel();

    /**
     * Translated "Add...".
     *
     * @return translated "Add..."
     */
    @DefaultMessage("Add...")
    @Key("buttonAddLabel")
    String buttonAddLabel();

    /**
     * Translated "Remove...".
     *
     * @return translated "Remove..."
     */
    @DefaultMessage("Remove...")
    @Key("buttonRemoveLabel")
    String buttonRemoveLabel();

    /**
     * Translated "Up".
     *
     * @return translated "Up"
     */
    @DefaultMessage("Up")
    @Key("buttonUpLabel")
    String buttonUpLabel();

    /**
     * Translated "Down".
     *
     * @return translated "Down"
     */
    @DefaultMessage("Down")
    @Key("buttonDownLabel")
    String buttonDownLabel();

    /**
     * Translated "Developing Packages".
     *
     * @return translated "Developing Packages"
     */
    @DefaultMessage("Developing Packages")
    @Key("developingPkgHelpLink")
    String developingPkgHelpLink();

    /**
     * Translated "Retrieving list of secondary repositories...".
     *
     * @return translated "Retrieving list of secondary repositories..."
     */
    @DefaultMessage("Retrieving list of secondary repositories...")
    @Key("secondaryReposDialog")
    String secondaryReposDialog();

    /**
     * Translated "Please select or input a CRAN repository".
     *
     * @return translated "Please select or input a CRAN repository"
     */
    @DefaultMessage("Please select or input a CRAN repository")
    @Key("validateSyncLabel")
    String validateSyncLabel();

    /**
     * Translated "The repository ".
     *
     * @return translated "The repository "
     */
    @DefaultMessage("The repository ")
    @Key("showErrorRepoMessage")
    String showErrorRepoMessage();

    /**
     * Translated "is already included".
     *
     * @return translated "is already included"
     */
    @DefaultMessage("is already included")
    @Key("alreadyIncludedMessage")
    String alreadyIncludedMessage();

    /**
     * Translated "Validating CRAN repository...".
     *
     * @return translated "Validating CRAN repository..."
     */
    @DefaultMessage("Validating CRAN repository...")
    @Key("validateAsyncProgress")
    String validateAsyncProgress();

    /**
     * Translated "The given URL does not appear to be a valid CRAN repository".
     *
     * @return translated "The given URL does not appear to be a valid CRAN repository"
     */
    @DefaultMessage("The given URL does not appear to be a valid CRAN repository")
    @Key("onResponseReceived")
    String onResponseReceived();

    /**
     * Translated "Name:".
     *
     * @return translated "Name:"
     */
    @DefaultMessage("Name:")
    @Key("nameLabel")
    String nameLabel();

    /**
     * Translated "Url:".
     *
     * @return translated "Url:"
     */
    @DefaultMessage("Url:")
    @Key("urlLabel")
    String urlLabel();

    /**
     * Translated "Available repositories:".
     *
     * @return translated "Available repositories:"
     */
    @DefaultMessage("Available repositories:")
    @Key("reposLabel")
    String reposLabel();

    /**
     * Translated "Add Secondary Repository".
     *
     * @return translated "Add Secondary Repository"
     */
    @DefaultMessage("Add Secondary Repository")
    @Key("secondaryRepoLabel")
    String secondaryRepoLabel();

    /**
     * Translated "Choose the layout of the panels in RStudio by selecting from the controls in each panel. Add up to three additional Source Columns to the left side of the layout. When a column is removed, all saved files within the column are closed and any unsaved files are moved to the main Source Pane.".
     *
     * @return translated "Choose the layout of the panels in RStudio by selecting from the controls in each panel. Add up to three additional Source Columns to the left side of the layout. When a column is removed, all saved files within the column are closed and any unsaved files are moved to the main Source Pane."
     */
    @DefaultMessage("Choose the layout of the panels in RStudio by selecting from the controls in each panel. Add up to three additional Source Columns to the left side of the layout. When a column is removed, all saved files within the column are closed and any unsaved files are moved to the main Source Pane.")
    @Key("paneLayoutText")
    String paneLayoutText();

    /**
     * Translated "Manage Column Display".
     *
     * @return translated "Manage Column Display"
     */
    @DefaultMessage("Manage Column Display")
    @Key("columnToolbarLabel")
    String columnToolbarLabel();

    /**
     * Translated "Add Column".
     *
     * @return translated "Add Column"
     */
    @DefaultMessage("Add Column")
    @Key("addButtonText")
    String addButtonText();

    /**
     * Translated "Add column".
     *
     * @return translated "Add column"
     */
    @DefaultMessage("Add column")
    @Key("addButtonLabel")
    String addButtonLabel();

    /**
     * Translated "Remove Column".
     *
     * @return translated "Remove Column"
     */
    @DefaultMessage("Remove Column")
    @Key("removeButtonText")
    String removeButtonText();

    /**
     * Translated "Remove column".
     *
     * @return translated "Remove column"
     */
    @DefaultMessage("Remove column")
    @Key("removeButtonLabel")
    String removeButtonLabel();

    /**
     * Translated "Bad config! Falling back to a reasonable default".
     *
     * @return translated "Bad config! Falling back to a reasonable default"
     */
    @DefaultMessage("Columns and Panes Layout")
    @Key("createGridLabel")
    String createGridLabel();

    /**
     * Translated "Additional source column".
     *
     * @return translated "Additional source column"
     */
    @DefaultMessage("Additional source column")
    @Key("createColumnLabel")
    String createColumnLabel();

    /**
     * Translated "Pane Layout".
     *
     * @return translated "Pane Layout"
     */
    @DefaultMessage("Pane Layout")
    @Key("paneLayoutLabel")
    String paneLayoutLabel();

    /**
     * Translated "Publishing Accounts".
     *
     * @return translated "Publishing Accounts"
     */
    @DefaultMessage("Publishing Accounts")
    @Key("accountListLabel")
    String accountListLabel();

    /**
     * Translated "Connect...".
     *
     * @return translated "Connect..."
     */
    @DefaultMessage("Connect...")
    @Key("connectButtonLabel")
    String connectButtonLabel();

    /**
     * Translated "Reconnect...".
     *
     * @return translated "Reconnect..."
     */
    @DefaultMessage("Reconnect...")
    @Key("reconnectButtonLabel")
    String reconnectButtonLabel();

    /**
     * Translated "Disconnect".
     *
     * @return translated "Disconnect"
     */
    @DefaultMessage("Disconnect")
    @Key("disconnectButtonLabel")
    String disconnectButtonLabel();

    /**
     * Translated "Account records appear to exist, but cannot be viewed because a required package is not installed.".
     *
     * @return translated "Account records appear to exist, but cannot be viewed because a required package is not installed."
     */
    @DefaultMessage("Account records appear to exist, but cannot be viewed because a required package is not installed.")
    @Key("missingPkgPanelMessage")
    String missingPkgPanelMessage();

    /**
     * Translated "Install Missing Packages".
     *
     * @return translated "Install Missing Packages"
     */
    @DefaultMessage("Install Missing Packages")
    @Key("installPkgsMessage")
    String installPkgsMessage();

    /**
     * Translated "Viewing publish accounts".
     *
     * @return translated "Viewing publish accounts"
     */
    @DefaultMessage("Viewing publish accounts")
    @Key("withRSConnectLabel")
    String withRSConnectLabel();

    /**
     * Translated "Enable publishing to RStudio Connect".
     *
     * @return translated "Enable publishing to RStudio Connect"
     */
    @DefaultMessage("Enable publishing to RStudio Connect")
    @Key("chkEnableRSConnectLabel")
    String chkEnableRSConnectLabel();

    /**
     * Translated "Information about RStudio Connect".
     *
     * @return translated "Information about RStudio Connect"
     */
    @DefaultMessage("Information about RStudio Connect")
    @Key("checkBoxWithHelpTitle")
    String checkBoxWithHelpTitle();

    /**
     * Translated "Settings".
     *
     * @return translated "Settings"
     */
    @DefaultMessage("Settings")
    @Key("settingsHeaderLabel")
    String settingsHeaderLabel();

    /**
     * Translated "Enable publishing documents, apps, and APIs".
     *
     * @return translated "Enable publishing documents, apps, and APIs"
     */
    @DefaultMessage("Enable publishing documents, apps, and APIs")
    @Key("chkEnablePublishingLabel")
    String chkEnablePublishingLabel();

    /**
     * Translated "Show diagnostic information when publishing".
     *
     * @return translated "Show diagnostic information when publishing"
     */
    @DefaultMessage("Show diagnostic information when publishing")
    @Key("showPublishDiagnosticsLabel")
    String showPublishDiagnosticsLabel();

    /**
     * Translated "SSL Certificates".
     *
     * @return translated "SSL Certificates"
     */
    @DefaultMessage("SSL Certificates")
    @Key("sSLCertificatesHeaderLabel")
    String sSLCertificatesHeaderLabel();

    /**
     * Translated "Check SSL certificates when publishing".
     *
     * @return translated "Check SSL certificates when publishing"
     */
    @DefaultMessage("Check SSL certificates when publishing")
    @Key("publishCheckCertificatesLabel")
    String publishCheckCertificatesLabel();

    /**
     * Translated "Check SSL certificates when publishing".
     *
     * @return translated "Check SSL certificates when publishing"
     */
    @DefaultMessage("Use custom CA bundle")
    @Key("usePublishCaBundleLabel")
    String usePublishCaBundleLabel();

    /**
     * Translated "(none)".
     *
     * @return translated "(none)"
     */
    @DefaultMessage("(none)")
    @Key("caBundlePath")
    String caBundlePath();

    /**
     * Translated "Troubleshooting Deployments".
     *
     * @return translated "Troubleshooting Deployments"
     */
    @DefaultMessage("Troubleshooting Deployments")
    @Key("helpLinkTroubleshooting")
    String helpLinkTroubleshooting();

    /**
     * Translated "Publishing".
     *
     * @return translated "Publishing"
     */
    @DefaultMessage("Publishing")
    @Key("publishingPaneHeader")
    String publishingPaneHeader();

    /**
     * Translated "Error Disconnecting Account".
     *
     * @return translated "Error Disconnecting Account"
     */
    @DefaultMessage("Error Disconnecting Account")
    @Key("showErrorCaption")
    String showErrorCaption();

    /**
     * Translated "Please select an account to disconnect.".
     *
     * @return translated "Please select an account to disconnect."
     */
    @DefaultMessage("Please select an account to disconnect.")
    @Key("showErrorMessage")
    String showErrorMessage();

    /**
     * Translated "Confirm Remove Account".
     *
     * @return translated "Confirm Remove Account"
     */
    @DefaultMessage("Confirm Remove Account")
    @Key("removeAccountGlobalDisplay")
    String removeAccountGlobalDisplay();

    /**
     * Translated "Are you sure you want to disconnect the ''{0}'' account on ''{1}''? This won''t delete the account on the server.".
     *
     * @return translated "Are you sure you want to disconnect the ''{0}'' account on ''{1}''? This won''t delete the account on the server."
     */
    @DefaultMessage("Are you sure you want to disconnect the ''{0}'' account on ''{1}''? This won''t delete the account on the server.")
    @Key("removeAccountMessage")
    String removeAccountMessage(String name, String server);

    /**
     * Translated "Disconnect Account".
     *
     * @return translated "Disconnect Account"
     */
    @DefaultMessage("Disconnect Account")
    @Key("onConfirmDisconnectYesLabel")
    String onConfirmDisconnectYesLabel();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    @DefaultMessage("Cancel")
    @Key("onConfirmDisconnectNoLabel")
    String onConfirmDisconnectNoLabel();

    /**
     * Translated "Error Disconnecting Account".
     *
     * @return translated "Error Disconnecting Account"
     */
    @DefaultMessage("Error Disconnecting Account")
    @Key("disconnectingErrorMessage")
    String disconnectingErrorMessage();

    /**
     * Translated "Connecting a publishing account".
     *
     * @return translated "Connecting a publishing account"
     */
    @DefaultMessage("Connecting a publishing account")
    @Key("getAccountCountLabel")
    String getAccountCountLabel();

    /**
     * Translated "(No interpreter selected)".
     *
     * @return translated "(No interpreter selected)"
     */
    @DefaultMessage("(No interpreter selected)")
    @Key("pythonPreferencesText")
    String pythonPreferencesText();

    /**
     * Translated "(NOTE: This project has already been configured with its own Python interpreter. Use the Project Options dialog to change the version of Python used in this project.".
     *
     * @return translated "(NOTE: This project has already been configured with its own Python interpreter. Use the Project Options dialog to change the version of Python used in this project."
     */
    @DefaultMessage("(NOTE: This project has already been configured with its own Python interpreter. Use the Project Options dialog to change the version of Python used in this project.")
    @Key("overrideText")
    String overrideText();


    /**
     * Translated "Python".
     *
     * @return translated "Python"
     */
    @DefaultMessage("Python")
    @Key("headerPythonLabel")
    String headerPythonLabel();

    /**
     * Translated "The active Python interpreter has been changed by an R startup script.".
     *
     * @return translated "The active Python interpreter has been changed by an R startup script."
     */
    @DefaultMessage("The active Python interpreter has been changed by an R startup script.")
    @Key("mismatchWarningBarText")
    String mismatchWarningBarText();

    /**
     * Translated "Finding interpreters...".
     *
     * @return translated "Finding interpreters..."
     */
    @DefaultMessage("Finding interpreters...")
    @Key("progressIndicatorText")
    String progressIndicatorText();

    /**
     * Translated "Finding interpreters...".
     *
     * @return translated "Finding interpreters..."
     */
    @DefaultMessage("Python interpreter:")
    @Key("tbPythonInterpreterText")
    String tbPythonInterpreterText();

    /**
     * Translated "Select...".
     *
     * @return translated "Select..."
     */
    @DefaultMessage("Select...")
    @Key("tbPythonActionText")
    String tbPythonActionText();

    /**
     * Translated "Error finding Python interpreters: ".
     *
     * @return translated "Error finding Python interpreters: "
     */
    @DefaultMessage("Error finding Python interpreters: ")
    @Key("onDependencyErrorMessage")
    String onDependencyErrorMessage();

    /**
     * Translated "The selected Python interpreter appears to be invalid.".
     *
     * @return translated "The selected Python interpreter appears to be invalid."
     */
    @DefaultMessage("The selected Python interpreter appears to be invalid.")
    @Key("invalidReasonLabel")
    String invalidReasonLabel();

    /**
     * Translated "Using Python in RStudio".
     *
     * @return translated "Using Python in RStudio"
     */
    @DefaultMessage("Using Python in RStudio")
    @Key("helpRnwButtonLabel")
    String helpRnwButtonLabel();

    /**
     * Translated "Automatically activate project-local Python environments".
     *
     * @return translated "Automatically activate project-local Python environments"
     */
    @DefaultMessage("Automatically activate project-local Python environments")
    @Key("cbAutoUseProjectInterpreter")
    String cbAutoUseProjectInterpreter();

    /**
     * Translated "When enabled, RStudio will automatically find and activate a Python environment located within the project root directory (if any).".
     *
     * @return translated "When enabled, RStudio will automatically find and activate a Python environment located within the project root directory (if any)."
     */
    @DefaultMessage("When enabled, RStudio will automatically find and activate a Python environment located within the project root directory (if any).")
    @Key("cbAutoUseProjectInterpreterMessage")
    String cbAutoUseProjectInterpreterMessage();


    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    @DefaultMessage("General")
    @Key("tabPanelCaption")
    String tabPanelCaption();

    /**
     * Translated "Clear".
     *
     * @return translated "Clear"
     */
    @DefaultMessage("Clear")
    @Key("clearLabel")
    String clearLabel();

    /**
     * Translated "System".
     *
     * @return translated "System"
     */
    @DefaultMessage("System")
    @Key("systemTab")
    String systemTab();

    /**
     * Translated "Virtual Environments".
     *
     * @return translated "Virtual Environments"
     */
    @DefaultMessage("Virtual Environments")
    @Key("virtualEnvTab")
    String virtualEnvTab();

    /**
     * Translated "Conda Environments".
     *
     * @return translated "Conda Environments"
     */
    @DefaultMessage("Conda Environments")
    @Key("condaEnvTab")
    String condaEnvTab();

    /**
     * Translated "[Unknown]".
     *
     * @return translated "[Unknown]"
     */
    @DefaultMessage("[Unknown]")
    @Key("unknownType")
    String unknownType();

    /**
     * Translated "Virtual Environment".
     *
     * @return translated "Virtual Environment"
     */
    @DefaultMessage("Virtual Environment")
    @Key("unknownType")
    String virtualEnvironmentType();

    /**
     * Translated "Conda Environment".
     *
     * @return translated "Conda Environment"
     */
    @DefaultMessage("Conda Environment")
    @Key("condaEnvironmentType")
    String condaEnvironmentType();

    /**
     * Translated "System Interpreter".
     *
     * @return translated "System Interpreter"
     */
    @DefaultMessage("System Interpreter")
    @Key("systemInterpreterType")
    String systemInterpreterType();

    /**
     * Get locale value for the Quarto preview label. Default value
     * is "This version of RStudio includes a preview of Quarto, a
     * new scientific and technical publishing system. "
     *
     * @return translated value for Quarto preview label
     */
    @DefaultMessage("This version of RStudio includes a preview of Quarto, a new scientific and technical publishing system. ")
    @Key("quartoPreviewLabel")
    String quartoPreviewLabel();

    /**
     * Get locale value of the label for the checkbox to enable the
     * Quarto preview. Default value is "Enable Quarto preview".
     *
     * @return the translated value for the label
     */
    @DefaultMessage("Enable Quarto preview")
    @Key("enableQuartoPreviewCheckboxLabel")
    String enableQuartoPreviewCheckboxLabel();

    /**
     * Get locale value for the help link caption. Default value
     * is "Learn more about Quarto".
     *
     * @return the translated value for the help link
     */
    @DefaultMessage("Learn more about Quarto")
    @Key("helpLinkCaption")
    String helpLinkCaption();

    /**
     * Translated "R Markdown".
     *
     * @return translated "R Markdown"
     */
    @DefaultMessage("R Markdown")
    @Key("rMarkdownHeaderLabel")
    String rMarkdownHeaderLabel();

    /**
     * Translated "Show document outline by default".
     *
     * @return translated "Show document outline by default"
     */
    @DefaultMessage("Show document outline by default")
    @Key("rMarkdownShowLabel")
    String rMarkdownShowLabel();

    /**
     * Translated "Soft-wrap R Markdown files".
     *
     * @return translated "Soft-wrap R Markdown files"
     */
    @DefaultMessage("Soft-wrap R Markdown files")
    @Key("rMarkdownSoftWrapLabel")
    String rMarkdownSoftWrapLabel();

    /**
     * Translated "Show in document outline: ".
     *
     * @return translated "Show in document outline: "
     */
    @DefaultMessage("Show in document outline: ")
    @Key("docOutlineDisplayLabel")
    String docOutlineDisplayLabel();

    /**
     * Translated "Sections Only".
     *
     * @return translated "Sections Only"
     */
    @DefaultMessage("Sections Only")
    @Key("docOutlineSectionsOption")
    String docOutlineSectionsOption();

    /**
     * Translated "Sections and Named Chunks".
     *
     * @return translated "Sections and Named Chunks"
     */
    @DefaultMessage("Sections and Named Chunks")
    @Key("docOutlineSectionsNamedChunksOption")
    String docOutlineSectionsNamedChunksOption();

    /**
     * Translated "Sections and All Chunks".
     *
     * @return translated "Sections and All Chunks"
     */
    @DefaultMessage("Sections and All Chunks")
    @Key("docOutlineSectionsAllChunksOption")
    String docOutlineSectionsAllChunksOption();

    /**
     * Translated "Show output preview in: ".
     *
     * @return translated "Show output preview in: "
     */
    @DefaultMessage("Show output preview in: ")
    @Key("rmdViewerModeLabel")
    String rmdViewerModeLabel();

    /**
     * Translated "Window".
     *
     * @return translated "Window"
     */
    @DefaultMessage("Window")
    @Key("rmdViewerModeWindowOption")
    String rmdViewerModeWindowOption();

    /**
     * Translated "Viewer Pane".
     *
     * @return translated "Viewer Pane"
     */
    @DefaultMessage("Viewer Pane")
    @Key("rmdViewerModeViewerPaneOption")
    String rmdViewerModeViewerPaneOption();

    /**
     * Translated "(None)".
     *
     * @return translated "(None)"
     */
    @DefaultMessage("(None)")
    @Key("rmdViewerModeNoneOption")
    String rmdViewerModeNoneOption();

    /**
     * Translated "Show output inline for all R Markdown documents".
     *
     * @return translated "Show output inline for all R Markdown documents"
     */
    @DefaultMessage("Show output inline for all R Markdown documents")
    @Key("rmdInlineOutputLabel")
    String rmdInlineOutputLabel();

    /**
     * Translated "Show equation and image previews: ".
     *
     * @return translated "Show equation and image previews: "
     */
    @DefaultMessage("Show equation and image previews: ")
    @Key("latexPreviewWidgetLabel")
    String latexPreviewWidgetLabel();

    /**
     * Translated "Never".
     *
     * @return translated "Never"
     */
    @DefaultMessage("Never")
    @Key("latexPreviewWidgetNeverOption")
    String latexPreviewWidgetNeverOption();

    /**
     * Translated "In a popup".
     *
     * @return translated "In a popup"
     */
    @DefaultMessage("In a popup")
    @Key("latexPreviewWidgetPopupOption")
    String latexPreviewWidgetPopupOption();

    /**
     * Translated "Inline".
     *
     * @return translated "Inline"
     */
    @DefaultMessage("Inline")
    @Key("latexPreviewWidgetInlineOption")
    String latexPreviewWidgetInlineOption();

    /**
     * Translated "Evaluate chunks in directory: ".
     *
     * @return translated "Evaluate chunks in directory: "
     */
    @DefaultMessage("Evaluate chunks in directory: ")
    @Key("knitWorkingDirLabel")
    String knitWorkingDirLabel();

    /**
     * Translated "Document".
     *
     * @return translated "Document"
     */
    @DefaultMessage("Document")
    @Key("knitWorkingDirDocumentOption")
    String knitWorkingDirDocumentOption();

    /**
     * Translated "Current".
     *
     * @return translated "Current"
     */
    @DefaultMessage("Current")
    @Key("knitWorkingDirCurrentOption")
    String knitWorkingDirCurrentOption();

    /**
     * Translated "Project".
     *
     * @return translated "Project"
     */
    @DefaultMessage("Project")
    @Key("knitWorkingDirProjectOption")
    String knitWorkingDirProjectOption();

    /**
     * Translated "R Notebooks".
     *
     * @return translated "R Notebooks"
     */
    @DefaultMessage("R Notebooks")
    @Key("rNotebooksCaption")
    String rNotebooksCaption();

    /**
     * Translated "Execute setup chunk automatically in notebooks".
     *
     * @return translated "Execute setup chunk automatically in notebooks"
     */
    @DefaultMessage("Execute setup chunk automatically in notebooks")
    @Key("autoExecuteSetupChunkLabel")
    String autoExecuteSetupChunkLabel();

    /**
     * Translated "Hide console automatically when executing notebook chunks".
     *
     * @return translated "Hide console automatically when executing notebook chunks"
     */
    @DefaultMessage("Hide console automatically when executing notebook chunks")
    @Key("notebookHideConsoleLabel")
    String notebookHideConsoleLabel();

    /**
     * Translated "Using R Notebooks".
     *
     * @return translated "Using R Notebooks"
     */
    @DefaultMessage("Using R Notebooks")
    @Key("helpRStudioLinkLabel")
    String helpRStudioLinkLabel();

    /**
     * Translated "Display".
     *
     * @return translated "Display"
     */
    @DefaultMessage("Display")
    @Key("advancedHeaderLabel")
    String advancedHeaderLabel();

    /**
     * Translated "Enable chunk background highlight".
     *
     * @return translated "Enable chunk background highlight"
     */
    @DefaultMessage("Enable chunk background highlight")
    @Key("advancedEnableChunkLabel")
    String advancedEnableChunkLabel();

    /**
     * Translated "Show inline toolbar for R code chunks".
     *
     * @return translated "Show inline toolbar for R code chunks"
     */
    @DefaultMessage("Show inline toolbar for R code chunks")
    @Key("advancedShowInlineLabel")
    String advancedShowInlineLabel();

    /**
     * Translated "Display render command in R Markdown tab".
     *
     * @return translated "Display render command in R Markdown tab"
     */
    @DefaultMessage("Display render command in R Markdown tab")
    @Key("advancedDisplayRender")
    String advancedDisplayRender();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    @DefaultMessage("General")
    @Key("visualModeGeneralCaption")
    String visualModeGeneralCaption();

    /**
     * Translated "Use visual editor by default for new documents".
     *
     * @return translated "Use visual editor by default for new documents"
     */
    @DefaultMessage("Use visual editor by default for new documents")
    @Key("visualModeUseVisualEditorLabel")
    String visualModeUseVisualEditorLabel();

    /**
     * Translated "Learn more about visual editing mode".
     *
     * @return translated "Learn more about visual editing mode"
     */
    @DefaultMessage("Learn more about visual editing mode")
    @Key("visualModeHelpLink")
    String visualModeHelpLink();

    /**
     * Translated "Display".
     *
     * @return translated "Display"
     */
    @DefaultMessage("Display")
    @Key("visualModeHeaderLabel")
    String visualModeHeaderLabel();

    /**
     * Translated "Show document outline by default".
     *
     * @return translated "Show document outline by default"
     */
    @DefaultMessage("Show document outline by default")
    @Key("visualEditorShowOutlineLabel")
    String visualEditorShowOutlineLabel();

    /**
     * Translated "Show margin column indicator in code blocks".
     *
     * @return translated "Show margin column indicator in code blocks"
     */
    @DefaultMessage("Show margin column indicator in code blocks")
    @Key("visualEditorShowMarginLabel")
    String visualEditorShowMarginLabel();

    /**
     * Translated "Editor content width (px):".
     *
     * @return translated "Editor content width (px):"
     */
    @DefaultMessage("Editor content width (px):")
    @Key("visualModeContentWidthLabel")
    String visualModeContentWidthLabel();

    /**
     * Translated "Editor font size:".
     *
     * @return translated "Editor font size:"
     */
    @DefaultMessage("Editor font size:")
    @Key("visualModeFontSizeLabel")
    String visualModeFontSizeLabel();

    /**
     * Translated "Markdown".
     *
     * @return translated "Markdown"
     */
    @DefaultMessage("Markdown")
    @Key("visualModeOptionsMarkdownCaption")
    String visualModeOptionsMarkdownCaption();

    /**
     * Translated "Default spacing between list items: ".
     *
     * @return translated "Default spacing between list items: "
     */
    @DefaultMessage("Default spacing between list items: ")
    @Key("visualModeListSpacingLabel")
    String visualModeListSpacingLabel();

    /**
     * Translated "Automatic text wrapping (line breaks): ".
     *
     * @return translated "Automatic text wrapping (line breaks): "
     */
    @DefaultMessage("Automatic text wrapping (line breaks): ")
    @Key("visualModeWrapLabel")
    String visualModeWrapLabel();

    /**
     * Translated "Learn more about automatic line wrapping".
     *
     * @return translated "Learn more about automatic line wrapping"
     */
    @DefaultMessage("Learn more about automatic line wrapping")
    @Key("visualModeWrapHelpLabel")
    String visualModeWrapHelpLabel();

    /**
     * Translated "Wrap at column:".
     *
     * @return translated "Wrap at column:"
     */
    @DefaultMessage("Wrap at column:")
    @Key("visualModeOptionsLabel")
    String visualModeOptionsLabel();

    /**
     * Translated "Write references at end of current: ".
     *
     * @return translated "Write references at end of current: "
     */
    @DefaultMessage("Write references at end of current: ")
    @Key("visualModeReferencesLabel")
    String visualModeReferencesLabel();

    /**
     * Translated "Write canonical visual mode markdown in source mode".
     *
     * @return translated "Write canonical visual mode markdown in source mode"
     */
    @DefaultMessage("Write canonical visual mode markdown in source mode")
    @Key("visualModeCanonicalLabel")
    String visualModeCanonicalLabel();

    /**
     * Translated "Visual Mode Preferences".
     *
     * @return translated "Visual Mode Preferences"
     */
    @DefaultMessage("Visual Mode Preferences")
    @Key("visualModeCanonicalMessageCaption")
    String visualModeCanonicalMessageCaption();

    /**
     * Translated "Are you sure you want to write canonical markdown from source mode for all R Markdown files?\n\nThis preference should generally only be used at a project level (to prevent re-writing of markdown source that you or others don''t intend to use with visual mode).\n\nChange this preference now?".
     *
     * @return translated "Are you sure you want to write canonical markdown from source mode for all R Markdown files?\n\nThis preference should generally only be used at a project level (to prevent re-writing of markdown source that you or others don''t intend to use with visual mode).\n\nChange this preference now?"
     */
    @DefaultMessage("Are you sure you want to write canonical markdown from source mode for all R Markdown files?\n\nThis preference should generally only be used at a project level (to prevent re-writing of markdown source that you or others don''t intend to use with visual mode).\n\nChange this preference now?")
    @Key("visualModeCanonicalPreferenceMessage")
    String visualModeCanonicalPreferenceMessage();

    /**
     * Translated "Learn more about markdown writer options".
     *
     * @return translated "Learn more about markdown writer options"
     */
    @DefaultMessage("Learn more about markdown writer options")
    @Key("markdownPerFileOptionsHelpLink")
    String markdownPerFileOptionsHelpLink();

    /**
     * Translated "Citation features are available within visual editing mode.".
     *
     * @return translated "Citation features are available within visual editing mode."
     */
    @DefaultMessage("Citation features are available within visual editing mode.")
    @Key("citationsLabel")
    String citationsLabel();

    /**
     * Translated "Learn more about using citations with visual editing mode".
     *
     * @return translated "Learn more about using citations with visual editing mode"
     */
    @DefaultMessage("Learn more about using citations with visual editing mode")
    @Key("citationsHelpLink")
    String citationsHelpLink();

    /**
     * Translated "Zotero".
     *
     * @return translated "Zotero"
     */
    @DefaultMessage("Zotero")
    @Key("zoteroHeaderLabel")
    String zoteroHeaderLabel();

    /**
     * Translated "Zotero Data Directory:".
     *
     * @return translated "Zotero Data Directory:"
     */
    @DefaultMessage("Zotero Data Directory:")
    @Key("zoteroDataDirLabel")
    String zoteroDataDirLabel();

    /**
     * Translated "(None Detected)".
     *
     * @return translated "(None Detected)"
     */
    @DefaultMessage("(None Detected)")
    @Key("zoteroDataDirNotDectedLabel")
    String zoteroDataDirNotDectedLabel();

    /**
     * Translated "Use Better BibTeX for citation keys and BibTeX export".
     *
     * @return translated "Use Better BibTeX for citation keys and BibTeX export"
     */
    @DefaultMessage("Use Better BibTeX for citation keys and BibTeX export")
    @Key("zoteroUseBetterBibtexLabel")
    String zoteroUseBetterBibtexLabel();

    /**
     * Translated "R Markdown".
     *
     * @return translated "R Markdown"
     */
    @DefaultMessage("R Markdown")
    @Key("tabPanelTitle")
    String tabPanelTitle();

    /**
     * Translated "Basic".
     *
     * @return translated "Basic"
     */
    @DefaultMessage("Basic")
    @Key("tabPanelBasic")
    String tabPanelBasic();

    /**
     * Translated "Advanced".
     *
     * @return translated "Advanced"
     */
    @DefaultMessage("Advanced")
    @Key("tabPanelAdvanced")
    String tabPanelAdvanced();

    /**
     * Translated "Visual".
     *
     * @return translated "Visual"
     */
    @DefaultMessage("Visual")
    @Key("tabPanelVisual")
    String tabPanelVisual();

    /**
     * Translated "Citations".
     *
     * @return translated "Citations"
     */
    @DefaultMessage("Citations")
    @Key("tabPanelCitations")
    String tabPanelCitations();

    /**
     * Translated "Web".
     *
     * @return translated "Web"
     */
    @DefaultMessage("Web")
    @Key("webOption")
    String webOption();

    /**
     * Translated "Show line numbers in code blocks".
     *
     * @return translated "Show line numbers in code blocks"
     */
    @DefaultMessage("Show line numbers in code blocks")
    @Key("showLinkNumbersLabel")
    String showLinkNumbersLabel();

    /**
     * Translated "Enable version control interface for RStudio projects".
     *
     * @return translated "Enable version control interface for RStudio projects"
     */
    @DefaultMessage("Enable version control interface for RStudio projects")
    @Key("chkVcsEnabledLabel")
    String chkVcsEnabledLabel();

    /**
     * Translated "Enable".
     *
     * @return translated "Enable"
     */
    @DefaultMessage("Enable")
    @Key("globalDisplayEnable")
    String globalDisplayEnable();

    /**
     * Translated "Disable".
     *
     * @return translated "Disable"
     */
    @DefaultMessage("Disable")
    @Key("globalDisplayDisable")
    String globalDisplayDisable();

    /**
     * Translated "{0} Version Control ".
     *
     * @return translated "{0} Version Control "
     */
    @DefaultMessage("{0} Version Control ")
    @Key("globalDisplayVC")
    String globalDisplayVC(String displayEnable);

    /**
     * Translated "You must restart RStudio for this change to take effect.".
     *
     * @return translated "You must restart RStudio for this change to take effect."
     */
    @DefaultMessage("You must restart RStudio for this change to take effect.")
    @Key("globalDisplayVCMessage")
    String globalDisplayVCMessage();

    /**
     * Translated "The program ''{0}'' is unlikely to be a valid git executable.\nPlease select a git executable called '''git.exe''.".
     *
     * @return translated "The program ''{0}'' is unlikely to be a valid git executable.\nPlease select a git executable called ''git.exe''."
     */
    @DefaultMessage("The program ''{0}'' is unlikely to be a valid git executable.\nPlease select a git executable called ''git.exe''.")
    @Key("gitExePathMessage")
    String gitExePathMessage(String gitPath);

    /**
     * Translated "Invalid Git Executable".
     *
     * @return translated "Invalid Git Executable."
     */
    @DefaultMessage("Invalid Git Executable")
    @Key("gitGlobalDisplay")
    String gitGlobalDisplay();

    /**
     * Translated "Git executable:".
     *
     * @return translated "Git executable:"
     */
    @DefaultMessage("Git executable:")
    @Key("gitExePathLabel")
    String gitExePathLabel();

    /**
     * Translated "(Not Found)".
     *
     * @return translated "(Not Found)"
     */
    @DefaultMessage("(Not Found)")
    @Key("gitExePathNotFoundLabel")
    String gitExePathNotFoundLabel();

    /**
     * Translated "SVN executable:".
     *
     * @return translated "SVN executable:"
     */
    @DefaultMessage("SVN executable:")
    @Key("svnExePathLabel")
    String svnExePathLabel();

    /**
     * Translated "Terminal executable:".
     *
     * @return translated "Terminal executable:"
     */
    @DefaultMessage("Terminal executable:")
    @Key("terminalPathLabel")
    String terminalPathLabel();

    /**
     * Translated "Git/SVN".
     *
     * @return translated "Git/SVN"
     */
    @DefaultMessage("Git/SVN")
    @Key("gitSVNPaneHeader")
    String gitSVNPaneHeader();

    /**
     * Translated "Dictionaries".
     *
     * @return translated "Dictionaries"
     */
    @DefaultMessage("Dictionaries")
    @Key("spellingPreferencesPaneHeader")
    String spellingPreferencesPaneHeader();

    /**
     * Translated "Ignore".
     *
     * @return translated "Ignore"
     */
    @DefaultMessage("Ignore")
    @Key("ignoreHeader")
    String ignoreHeader();

    /**
     * Translated "Ignore words in UPPERCASE".
     *
     * @return translated "Ignore words in UPPERCASE"
     */
    @DefaultMessage("Ignore words in UPPERCASE")
    @Key("ignoreWordsUppercaseLabel")
    String ignoreWordsUppercaseLabel();

    /**
     * Translated "Ignore words with numbers".
     *
     * @return translated "Ignore words with numbers"
     */
    @DefaultMessage("Ignore words with numbers")
    @Key("ignoreWordsNumbersLabel")
    String ignoreWordsNumbersLabel();

    /**
     * Translated "Checking".
     *
     * @return translated "Checking"
     */
    @DefaultMessage("Checking")
    @Key("checkingHeader")
    String checkingHeader();

    /**
     * Translated "Use real time spell-checking".
     *
     * @return translated "Use real time spell-checking"
     */
    @DefaultMessage("Use real time spell-checking")
    @Key("realTimeSpellcheckingCheckboxLabel")
    String realTimeSpellcheckingCheckboxLabel();


    /**
     * Translated "User dictionary: ".
     *
     * @return translated "User dictionary: "
     */
    @DefaultMessage("User dictionary: ")
    @Key("kUserDictionaryLabel")
    String kUserDictionaryLabel();

    /**
     * Translated "{0}{1} words".
     *
     * @return translated "{0}{1} words"
     */
    @DefaultMessage("{0}{1} words")
    @Key("kUserDictionaryWordsLabel")
    String kUserDictionaryWordsLabel(String kUserDictionary, String entries);

    /**
     * Translated "Edit User Dictionary...".
     *
     * @return translated "Edit User Dictionary..."
     */
    @DefaultMessage("Edit User Dictionary...")
    @Key("editUserDictLabel")
    String editUserDictLabel();

    /**
     * Translated "Edit User Dictionary".
     *
     * @return translated "Edit User Dictionary"
     */
    @DefaultMessage("Edit User Dictionary")
    @Key("editUserDictCaption")
    String editUserDictCaption();

    /**
     * Translated "Save".
     *
     * @return translated "Save"
     */
    @DefaultMessage("Save")
    @Key("editUserDictSaveCaption")
    String editUserDictSaveCaption();

    /**
     * Translated "Spelling".
     *
     * @return translated "Spelling"
     */
    @DefaultMessage("Spelling")
    @Key("spellingPaneLabel")
    String spellingPaneLabel();

    /**
     * Translated "Edit".
     *
     * @return translated "Edit"
     */
    @DefaultMessage("Edit")
    @Key("editDialog")
    String editDialog();

    /**
     * Translated "Save".
     *
     * @return translated "Save"
     */
    @DefaultMessage("Save")
    @Key("saveDialog")
    String saveDialog();

    /**
     * Translated "Save".
     *
     * @return translated "Save"
     */
    @DefaultMessage("Cancel")
    @Key("cancelButton")
    String cancelButton();

    /**
     * Translated "Shell".
     *
     * @return translated "Shell"
     */
    @DefaultMessage("Shell")
    @Key("shellHeaderLabel")
    String shellHeaderLabel();

    /**
     * Translated "Initial directory:".
     *
     * @return translated "Initial directory:"
     */
    @DefaultMessage("Initial directory:")
    @Key("initialDirectoryLabel")
    String initialDirectoryLabel();

    /**
     * Translated "Project directory".
     *
     * @return translated "Project directory"
     */
    @DefaultMessage("Project directory")
    @Key("projectDirectoryOption")
    String projectDirectoryOption();

    /**
     * Translated "Current directory".
     *
     * @return translated "Current directory"
     */
    @DefaultMessage("Current directory")
    @Key("currentDirectoryOption")
    String currentDirectoryOption();

    /**
     * Translated "Home directory".
     *
     * @return translated "Home directory"
     */
    @DefaultMessage("Home directory")
    @Key("homeDirectoryOption")
    String homeDirectoryOption();

    /**
     * Translated "New terminals open with:".
     *
     * @return translated "New terminals open with:"
     */
    @DefaultMessage("New terminals open with:")
    @Key("terminalShellLabel")
    String terminalShellLabel();

    /**
     * Translated "The program ''{0}'' is unlikely to be a valid shell executable.".
     *
     * @return translated "The program ''{0}'' is unlikely to be a valid shell executable."
     */
    @DefaultMessage("The program ''{0}'' is unlikely to be a valid shell executable.")
    @Key("shellExePathMessage")
    String shellExePathMessage(String shellExePath);
    /**
     * Translated "Invalid Shell Executable".
     *
     * @return translated "Invalid Shell Executable"
     */
    @DefaultMessage("Invalid Shell Executable")
    @Key("shellExeCaption")
    String shellExeCaption();

    /**
     * Translated "Custom shell binary:".
     *
     * @return translated "Custom shell binary:"
     */
    @DefaultMessage("Custom shell binary:")
    @Key("customShellPathLabel")
    String customShellPathLabel();

    /**
     * Translated "(Not Found)".
     *
     * @return translated "(Not Found)"
     */
    @DefaultMessage("(Not Found)")
    @Key("customShellChooserEmptyLabel")
    String customShellChooserEmptyLabel();

    /**
     * Translated "Custom shell command-line options:".
     *
     * @return translated "Custom shell command-line options:"
     */
    @DefaultMessage("Custom shell command-line options:")
    @Key("customShellOptionsLabel")
    String customShellOptionsLabel();

    /**
     * Translated "Connection".
     *
     * @return translated "Connection"
     */
    @DefaultMessage("Connection")
    @Key("perfLabel")
    String perfLabel();

    /**
     * Translated "Local terminal echo".
     *
     * @return translated "Local terminal echo"
     */
    @DefaultMessage("Local terminal echo")
    @Key("chkTerminalLocalEchoLabel")
    String chkTerminalLocalEchoLabel();

    /**
     * Translated "Local echo is more responsive but may get out of sync with some line-editing modes or custom shells.".
     *
     * @return translated "Local echo is more responsive but may get out of sync with some line-editing modes or custom shells."
     */
    @DefaultMessage("Local echo is more responsive but may get out of sync with some line-editing modes or custom shells.")
    @Key("chkTerminalLocalEchoTitle")
    String chkTerminalLocalEchoTitle();

    /**
     * Translated "Connect with WebSockets".
     *
     * @return translated "Connect with WebSockets"
     */
    @DefaultMessage("Connect with WebSockets")
    @Key("chkTerminalWebsocketLabel")
    String chkTerminalWebsocketLabel();

    /**
     * Translated "WebSockets are generally more responsive; try turning off if terminal won''t connect.".
     *
     * @return translated "WebSockets are generally more responsive; try turning off if terminal won''t connect."
     */
    @DefaultMessage("WebSockets are generally more responsive; try turning off if terminal won''t connect.")
    @Key("chkTerminalWebsocketTitle")
    String chkTerminalWebsocketTitle();

    /**
     * Translated "Display".
     *
     * @return translated "Display"
     */
    @DefaultMessage("Display")
    @Key("displayHeaderLabel")
    String displayHeaderLabel();

    /**
     * Translated "Hardware acceleration".
     *
     * @return translated "Hardware acceleration"
     */
    @DefaultMessage("Hardware acceleration")
    @Key("chkHardwareAccelerationLabel")
    String chkHardwareAccelerationLabel();

    /**
     * Translated "Audible bell".
     *
     * @return translated "Audible bell"
     */
    @DefaultMessage("Audible bell")
    @Key("chkAudibleBellLabel")
    String chkAudibleBellLabel();

    /**
     * Translated "Clickable web links".
     *
     * @return translated "Clickable web links"
     */
    @DefaultMessage("Clickable web links")
    @Key("chkWebLinksLabel")
    String chkWebLinksLabel();

    /**
     * Translated "Using the RStudio terminal".
     *
     * @return translated "Using the RStudio terminal"
     */
    @DefaultMessage("Using the RStudio terminal")
    @Key("helpLinkLabel")
    String helpLinkLabel();

    /**
     * Translated "Miscellaneous".
     *
     * @return translated "Miscellaneous"
     */
    @DefaultMessage("Miscellaneous")
    @Key("miscLabel")
    String miscLabel();

    /**
     * Translated "When shell exits:".
     *
     * @return translated "When shell exits:"
     */
    @DefaultMessage("When shell exits:")
    @Key("autoClosePrefLabel")
    String autoClosePrefLabel();

    /**
     * Translated "Close the pane".
     *
     * @return translated "Close the pane"
     */
    @DefaultMessage("Close the pane")
    @Key("closePaneOption")
    String closePaneOption();

    /**
     * Translated "Don''t close the pane".
     *
     * @return translated "Don''t close the pane"
     */
    @DefaultMessage("Don''t close the pane")
    @Key("doNotClosePaneOption")
    String doNotClosePaneOption();

    /**
     * Translated "Close pane if shell exits cleanly".
     *
     * @return translated "Close pane if shell exits cleanly"
     */
    @DefaultMessage("Close pane if shell exits cleanly")
    @Key("shellExitsPaneOption")
    String shellExitsPaneOption();

    /**
     * Translated "Save and restore environment variables".
     *
     * @return translated "Save and restore environment variables"
     */
    @DefaultMessage("Save and restore environment variables")
    @Key("chkCaptureEnvLabel")
    String chkCaptureEnvLabel();

    /**
     * Translated "Terminal occasionally runs a hidden command to capture state of environment variables.".
     *
     * @return translated "Terminal occasionally runs a hidden command to capture state of environment variables."
     */
    @DefaultMessage("Terminal occasionally runs a hidden command to capture state of environment variables.")
    @Key("chkCaptureEnvTitle")
    String chkCaptureEnvTitle();

    /**
     * Translated "Process Termination".
     *
     * @return translated "Process Termination"
     */
    @DefaultMessage("Process Termination")
    @Key("shutdownLabel")
    String shutdownLabel();

    /**
     * Translated "Ask before killing processes:".
     *
     * @return translated "Ask before killing processes:"
     */
    @DefaultMessage("Ask before killing processes:")
    @Key("busyModeLabel")
    String busyModeLabel();

    /**
     * Translated "Don''t ask before killing:".
     *
     * @return translated "Don''t ask before killing:"
     */
    @DefaultMessage("Don''t ask before killing:")
    @Key("busyWhitelistLabel")
    String busyWhitelistLabel();

    /**
     * Translated "Terminal".
     *
     * @return translated "Terminal"
     */
    @DefaultMessage("Terminal")
    @Key("terminalPaneLabel")
    String terminalPaneLabel();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    @DefaultMessage("General")
    @Key("tabGeneralPanelLabel")
    String tabGeneralPanelLabel();

    /**
     * Translated "Closing".
     *
     * @return translated "Closing"
     */
    @DefaultMessage("Closing")
    @Key("tabClosingPanelLabel")
    String tabClosingPanelLabel();

    /**
     * Translated "Always".
     *
     * @return translated "Always"
     */
    @DefaultMessage("Always")
    @Key("busyModeAlwaysOption")
    String busyModeAlwaysOption();

    /**
     * Translated "Never".
     *
     * @return translated "Never"
     */
    @DefaultMessage("Never")
    @Key("busyModeNeverOption")
    String busyModeNeverOption();

    /**
     * Translated "Always except for list".
     *
     * @return translated "Always except for list"
     */
    @DefaultMessage("Always except for list")
    @Key("busyModeListOption")
    String busyModeListOption();

    /**
     * Translated "Enable Python integration".
     *
     * @return translated "Enable Python integration"
     */
    @DefaultMessage("Enable Python integration")
    @Key("chkPythonIntegration")
    String chkPythonIntegration();

    /**
     * Translated "When enabled, the active version of Python will be placed on the PATH for new terminal sessions. Only bash and zsh are supported.".
     *
     * @return translated "When enabled, the active version of Python will be placed on the PATH for new terminal sessions. Only bash and zsh are supported."
     */
    @DefaultMessage("When enabled, the active version of Python will be placed on the PATH for new terminal sessions. Only bash and zsh are supported.")
    @Key("chkPythonIntegrationTitle")
    String chkPythonIntegrationTitle();

    /**
     * Translated "History".
     *
     * @return translated "History"
     */
    @DefaultMessage("History")
    @Key("historyTab")
    String historyTab();

    /**
     * Translated "Files".
     *
     * @return translated "Files"
     */
    @DefaultMessage("Files")
    @Key("filesTab")
    String filesTab();


    /**
     * Translated "Plots".
     *
     * @return translated "Plots"
     */
    @DefaultMessage("Plots")
    @Key("plotsTab")
    String plotsTab();

    /**
     * Translated "Connections".
     *
     * @return translated "Connections"
     */
    @DefaultMessage("Connections")
    @Key("connectionsTab")
    String connectionsTab();

    /**
     * Translated "Packages".
     *
     * @return translated "Packages"
     */
    @DefaultMessage("Packages")
    @Key("packagesTab")
    String packagesTab();

    /**
     * Translated "Help".
     *
     * @return translated "Help"
     */
    @DefaultMessage("Help")
    @Key("helpTab")
    String helpTab();

    /**
     * Translated "Build".
     *
     * @return translated "Build"
     */
    @DefaultMessage("Build")
    @Key("buildTab")
    String buildTab();

    /**
     * Translated "VCS".
     *
     * @return translated "VCS"
     */
    @DefaultMessage("VCS")
    @Key("vcsTab")
    String vcsTab();

    /**
     * Translated "Tutorial".
     *
     * @return translated "Tutorial"
     */
    @DefaultMessage("Tutorial")
    @Key("tutorialTab")
    String tutorialTab();

    /**
     * Translated "Viewer".
     *
     * @return translated "Viewer"
     */
    @DefaultMessage("Viewer")
    @Key("viewerTab")
    String viewerTab();

    /**
     * Translated "Presentation".
     *
     * @return translated "Presentation"
     */
    @DefaultMessage("Presentation")
    @Key("presentationTab")
    String presentationTab();

    /**
     * Translated "Confirm Remove".
     *
     * @return translated "Confirm Remove"
     */
    @DefaultMessage("Confirm Remove")
    @Key("confirmRemoveCaption")
    String confirmRemoveCaption();

    /**
     * Translated "Are you sure you want to remove the {0} repository?".
     *
     * @return translated "Are you sure you want to remove the {0} repository?"
     */
    @DefaultMessage("Are you sure you want to remove the {0} repository?")
    @Key("confirmRemoveMessage")
    String confirmRemoveMessage(String repo);

    /**
     * Translated "Modern".
     *
     * @return translated "Modern"
     */
    @DefaultMessage("Modern")
    @Key("modernThemeLabel")
    String modernThemeLabel();

    /**
     * Translated "Sky".
     *
     * @return translated "Sky"
     */
    @DefaultMessage("Sky")
    @Key("skyThemeLabel")
    String skyThemeLabel();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    @DefaultMessage("General")
    @Key("generalHeaderLabel")
    String generalHeaderLabel();

    /**
     * Translated "Edit Snippets...".
     *
     * @return translated "Edit Snippets..."
     */
    @DefaultMessage("Edit Snippets...")
    @Key("editSnippetsButtonLabel")
    String editSnippetsButtonLabel();

    /**
     * Translated "tight".
     *
     * @return translated "tight"
     */
    @DefaultMessage("tight")
    @Key("listSpacingTight")
    String listSpacingTight();

    /**
     * Translated "spaced".
     *
     * @return translated "spaced"
     */
    @DefaultMessage("spaced")
    @Key("listSpacingSpaced")
    String listSpacingSpaced();

    /**
     * Translated "(none)".
     *
     * @return translated "(none)"
     */
    @DefaultMessage("(none)")
    @Key("editingWrapNone")
    String editingWrapNone();

    /**
     * Translated "(column)".
     *
     * @return translated "(column)"
     */
    @DefaultMessage("(column)")
    @Key("editingWrapColumn")
    String editingWrapColumn();

    /**
     * Translated "(sentence)".
     *
     * @return translated "(sentence)"
     */
    @DefaultMessage("(sentence)")
    @Key("editingWrapSentence")
    String editingWrapSentence();

    /**
     * Translated "block".
     *
     * @return translated "block"
     */
    @DefaultMessage("block")
    @Key("refLocationBlock")
    String refLocationBlock();

    /**
     * Translated "section".
     *
     * @return translated "section"
     */
    @DefaultMessage("section")
    @Key("refLocationSection")
    String refLocationSection();

    /**
     * Translated "document"".
     *
     * @return translated "document"
     */
    @DefaultMessage("document")
    @Key("refLocationDocument")
    String refLocationDocument();

    /**
     * Translated "Other Languages".
     *
     * @return translated "Other Languages"
     */
    @DefaultMessage("Other Languages")
    @Key("editingDiagOtherLabel")
    String editingDiagOtherLabel();

    /**
     * Translated "Show Diagnostics".
     *
     * @return translated "Show Diagnostics"
     */
    @DefaultMessage("Show Diagnostics")
    @Key("editingDiagShowLabel")
    String editingDiagShowLabel();

    /**
     * Translated "R Diagnostics".
     *
     * @return translated "R Diagnostics"
     */
    @DefaultMessage("R Diagnostics")
    @Key("editingDiagnosticsPanel")
    String editingDiagnosticsPanel();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    @DefaultMessage("General")
    @Key("editingDisplayPanel")
    String editingDisplayPanel();

    /**
     * Translated "Modify Keyboard Shortcuts...".
     *
     * @return translated "Modify Keyboard Shortcuts..."
     */
    @DefaultMessage("Modify Keyboard Shortcuts...")
    @Key("editingEditShortcuts")
    String editingEditShortcuts();

    /**
     * Translated "Execution".
     *
     * @return translated "Execution"
     */
    @DefaultMessage("Execution")
    @Key("editingExecutionLabel")
    String editingExecutionLabel();

    /**
     * Translated "Completion Delay".
     *
     * @return translated "Completion Delay"
     */
    @DefaultMessage("Completion Delay")
    @Key("editingHeaderLabel")
    String editingHeaderLabel();

    /**
     * Translated "Other Languages".
     *
     * @return translated "Other Languages"
     */
    @DefaultMessage("Other Languages")
    @Key("editingOtherLabel")
    String editingOtherLabel();

    /**
     * Translated "Keyword and text-based completions are supported for several other languages including JavaScript, HTML, CSS, Python, and SQL.".
     *
     * @return translated "Keyword and text-based completions are supported for several other languages including JavaScript, HTML, CSS, Python, and SQL."
     */
    @DefaultMessage("Keyword and text-based completions are supported for several other languages including JavaScript, HTML, CSS, Python, and SQL.")
    @Key("editingOtherTip")
    String editingOtherTip();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    @DefaultMessage("General")
    @Key("editingSavePanel")
    String editingSavePanel();

    /**
     * Translated "Change...".
     *
     * @return translated "Change..."
     */
    @DefaultMessage("Change...")
    @Key("editingSavePanelAction")
    String editingSavePanelAction();

    /**
     * Translated "Auto-save".
     *
     * @return translated "Auto-save"
     */
    @DefaultMessage("Auto-save")
    @Key("editingSavePanelAutosave")
    String editingSavePanelAutosave();

    /**
     * Translated "Serialization".
     *
     * @return translated "Serialization"
     */
    @DefaultMessage("Serialization")
    @Key("editingSerializationLabel")
    String editingSerializationLabel();

    /**
     * Translated "Help on code snippets".
     *
     * @return translated "Help on code snippets"
     */
    @DefaultMessage("Help on code snippets")
    @Key("editingSnippetHelpTitle")
    String editingSnippetHelpTitle();

    /**
     * Translated "Snippets".
     *
     * @return translated "Snippets"
     */
    @DefaultMessage("Snippets")
    @Key("editingSnippetsLabel")
    String editingSnippetsLabel();

    /**
     * Translated "Editing".
     *
     * @return translated "Editing"
     */
    @DefaultMessage("Editing")
    @Key("editingTabPanel")
    String editingTabPanel();

    /**
     * Translated "Completion".
     *
     * @return translated "Completion"
     */
    @DefaultMessage("Completion")
    @Key("editingTabPanelCompletionPanel")
    String editingTabPanelCompletionPanel();

    /**
     * Translated "Diagnostics".
     *
     * @return translated "Diagnostics"
     */
    @DefaultMessage("Diagnostics")
    @Key("editingTabPanelDiagnosticsPanel")
    String editingTabPanelDiagnosticsPanel();

    /**
     * Translated "Display".
     *
     * @return translated "Display"
     */
    @DefaultMessage("Display")
    @Key("editingTabPanelDisplayPanel")
    String editingTabPanelDisplayPanel();

    /**
     * Translated "Saving".
     *
     * @return translated "Saving"
     */
    @DefaultMessage("Saving")
    @Key("editingTabPanelSavePanel")
    String editingTabPanelSavePanel();

    /**
     * Translated "R and C/C++".
     *
     * @return translated "R and C/C++"
     */
    @DefaultMessage("R and C/C++")
    @Key("editingCompletionPanel")
    String editingCompletionPanel();

    /**
     * Translated "General".
     *
     * @return translated "General"
     */
    @DefaultMessage("General")
    @Key("editingHeader")
    String editingHeader();

    /**
     * Translated "No bindings available".
     *
     * @return translated "No bindings available"
     */
    @DefaultMessage("No bindings available")
    @Key("editingKeyboardShortcuts")
    String editingKeyboardShortcuts();

    /**
     * Translated "Keyboard Shortcuts".
     *
     * @return translated "Keyboard Shortcuts"
     */
    @DefaultMessage("Keyboard Shortcuts")
    @Key("editingKeyboardText")
    String editingKeyboardText();

    /**
     * Translated "Customized".
     *
     * @return translated "Customized"
     */
    @DefaultMessage("Customized")
    @Key("editingRadioCustomized")
    String editingRadioCustomized();

    /**
     * Translated "Filter...".
     *
     * @return translated "Filter..."
     */
    @DefaultMessage("Filter...")
    @Key("editingFilterWidget")
    String editingFilterWidget();

    /**
     * Translated "Reset...".
     *
     * @return translated "Reset..."
     */
    @DefaultMessage("Reset...")
    @Key("editingResetText")
    String editingResetText();

    /**
     * Translated "Reset Keyboard Shortcuts".
     *
     * @return translated "Reset Keyboard Shortcuts"
     */
    @DefaultMessage("Reset Keyboard Shortcuts")
    @Key("editingGlobalDisplay")
    String editingGlobalDisplay();

    /**
     * Translated "Are you sure you want to reset keyboard shortcuts to their default values? ".
     *
     * @return translated "Are you sure you want to reset keyboard shortcuts to their default values? "
     */
    @DefaultMessage("Are you sure you want to reset keyboard shortcuts to their default values? ")
    @Key("editingGlobalCaption")
    String editingGlobalCaption();

    /**
     * Translated "This action cannot be undone.".
     *
     * @return translated "This action cannot be undone."
     */
    @DefaultMessage("This action cannot be undone.")
    @Key("editingGlobalMessage")
    String editingGlobalMessage();

    /**
     * Translated "Resetting Keyboard Shortcuts...".
     *
     * @return translated "Resetting Keyboard Shortcuts..."
     */
    @DefaultMessage("Resetting Keyboard Shortcuts...")
    @Key("editingProgressMessage")
    String editingProgressMessage();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    @DefaultMessage("Cancel")
    @Key("editingCancelShortcuts")
    String editingCancelShortcuts();

    /**
     * Translated "Tab width"
     *
     * @return translated "Tab width"
     */
    @DefaultMessage("Tab width")
    @Key("editingTabWidthLabel")
    String editingTabWidthLabel();

    /**
     * Translated "Auto-detect code indentation"
     *
     * @return translated "Auto-detect code indentation"
     */
    @DefaultMessage("Auto-detect code indentation")
    @Key("editingAutoDetectIndentationLabel")
    String editingAutoDetectIndentationLabel();

    /**
     * Translated "Auto-detect code indentation"
     *
     * @return translated "When enabled, the indentation for documents not part of an RStudio project will be automatically detected."
     */
    @DefaultMessage("When enabled, the indentation for documents not part of an RStudio project will be automatically detected.")
    @Key("editingAutoDetectIndentationDesc")
    String editingAutoDetectIndentationDesc();

    /**
     * Translated "Insert matching parens/quotes"
     *
     * @return translated "Insert matching parens/quotes"
     */
    @DefaultMessage("Insert matching parens/quotes")
    @Key("editingInsertMatchingLabel")
    String editingInsertMatchingLabel();

    /**
     * Translated "Use native pipe operator, |> (requires R 4.1+)"
     *
     * @return translated "Use native pipe operator, |> (requires R 4.1+)"
     */
    @DefaultMessage("Use native pipe operator, |> (requires R 4.1+)")
    @Key("editingUseNativePipeOperatorLabel")
    String editingUseNativePipeOperatorLabel();

    /**
     * Translated "NOTE: Some of these settings may be overridden by project-specific preferences."
     *
     * @return translated "NOTE: Some of these settings may be overridden by project-specific preferences."
     */
    @DefaultMessage("NOTE: Some of these settings may be overridden by project-specific preferences.")
    @Key("editingProjectOverrideInfoText")
    String editingProjectOverrideInfoText();

    /**
     * Translated "Edit Project Preferences..."
     *
     * @return translated "Edit Project Preferences..."
     */
    @DefaultMessage("Edit Project Preferences...")
    @Key("editProjectPreferencesButtonLabel")
    String editProjectPreferencesButtonLabel();

    /**
     * Translated "Auto-indent code after paste"
     *
     * @return translated "Auto-indent code after paste"
     */
    @DefaultMessage("Auto-indent code after paste")
    @Key("editingReindentOnPasteLabel")
    String editingReindentOnPasteLabel();

    /**
     * Translated "Vertically align arguments in auto-indent"
     *
     * @return translated "Vertically align arguments in auto-indent"
     */
    @DefaultMessage("Vertically align arguments in auto-indent")
    @Key("editingVerticallyAlignArgumentsIndentLabel")
    String editingVerticallyAlignArgumentsIndentLabel();

    /**
     * Translated "Continue comment when inserting new line"
     *
     * @return translated "Continue comment when inserting new line"
     */
    @DefaultMessage("Continue comment when inserting new line")
    @Key("editingContinueCommentsOnNewlineLabel")
    String editingContinueCommentsOnNewlineLabel();

    /**
     * Translated "When enabled, pressing Enter will continue comments on new lines. Press Shift + Enter to exit a comment."
     *
     * @return translated "When enabled, pressing Enter will continue comments on new lines. Press Shift + Enter to exit a comment."
     */
    @DefaultMessage("When enabled, pressing Enter will continue comments on new lines. Press Shift + Enter to exit a comment.")
    @Key("editingContinueCommentsOnNewlineDesc")
    String editingContinueCommentsOnNewlineDesc();

    /**
     * Translated "Enable hyperlink highlighting in editor"
     *
     * @return translated "Enable hyperlink highlighting in editor"
     */
    @DefaultMessage("Enable hyperlink highlighting in editor")
    @Key("editingHighlightWebLinkLabel")
    String editingHighlightWebLinkLabel();

    /**
     * Translated "When enabled, hyperlinks in comments will be underlined and clickable."
     *
     * @return translated "When enabled, hyperlinks in comments will be underlined and clickable."
     */
    @DefaultMessage("When enabled, hyperlinks in comments will be underlined and clickable.")
    @Key("editingHighlightWebLinkDesc")
    String editingHighlightWebLinkDesc();

    /**
     * Translated "Surround selection on text insertion:"
     *
     * @return translated "Surround selection on text insertion:"
     */
    @DefaultMessage("Surround selection on text insertion:")
    @Key("editingSurroundSelectionLabel")
    String editingSurroundSelectionLabel();

    /**
     * Translated "Keybindings:"
     *
     * @return translated "Keybindings:"
     */
    @DefaultMessage("Keybindings:")
    @Key("editingKeybindingsLabel")
    String editingKeybindingsLabel();

    /**
     * Translated "Focus console after executing from source"
     *
     * @return translated "Focus console after executing from source"
     */
    @DefaultMessage("Focus console after executing from source")
    @Key("editingFocusConsoleAfterExecLabel")
    String editingFocusConsoleAfterExecLabel();

    /**
     * Translated "Ctrl+Enter executes:"
     *
     * @return translated "Ctrl+Enter executes:"
     */
    @DefaultMessage("Ctrl+Enter executes:")
    @Key("editingExecutionBehaviorLabel")
    String editingExecutionBehaviorLabel();

    /**
     * Translated "Highlight selected word"
     *
     * @return translated "Highlight selected word"
     */
    @DefaultMessage("Highlight selected word")
    @Key("displayHighlightSelectedWordLabel")
    String displayHighlightSelectedWordLabel();

    /**
     * Translated "Highlight selected line"
     *
     * @return translated "Highlight selected line"
     */
    @DefaultMessage("Highlight selected line")
    @Key("displayHighlightSelectedLineLabel")
    String displayHighlightSelectedLineLabel();

    /**
      * Translated "Show line numbers"
      *
      * @return translated "Show line numbers"
      */
    @DefaultMessage("Show line numbers")
    @Key("displayShowLineNumbersLabel")
    String displayShowLineNumbersLabel();

    /**
     * Translated "Relative line numbers"
     *
     * @return translated "Relative line numbers"
     */
    @DefaultMessage("Relative line numbers")
    @Key("displayRelativeLineNumbersLabel")
    String displayRelativeLineNumbersLabel();

    /**
     * Translated "Show margin"
     *
     * @return translated "Show margin"
     */
    @DefaultMessage("Show margin")
    @Key("displayShowMarginLabel")
    String displayShowMarginLabel();

    /**
     * Translated "Show whitespace characters"
     *
     * @return translated "Show whitespace characters"
     */
    @DefaultMessage("Show whitespace characters")
    @Key("displayShowInvisiblesLabel")
    String displayShowInvisiblesLabel();

    /**
     * Translated "Show indent guides"
     *
     * @return translated "Show indent guides"
     */
    @DefaultMessage("Show indent guides")
    @Key("displayShowIndentGuidesLabel")
    String displayShowIndentGuidesLabel();

    /**
     * Translated "Blinking cursor"
     *
     * @return translated "Blinking cursor"
     */
    @DefaultMessage("Blinking cursor")
    @Key("displayBlinkingCursorLabel")
    String displayBlinkingCursorLabel();

    /**
     * Translated "Allow scroll past end of document"
     *
     * @return translated "Allow scroll past end of document"
     */
    @DefaultMessage("Allow scroll past end of document")
    @Key("displayScrollPastEndOfDocumentLabel")
    String displayScrollPastEndOfDocumentLabel();


    /**
     * Translated "Allow drag and drop of text"
     *
     * @return translated "Allow drag and drop of text"
     */
    @DefaultMessage("Allow drag and drop of text")
    @Key("displayEnableTextDragLabel")
    String displayEnableTextDragLabel();

    /**
     * Translated "Fold Style:"
     *
     * @return translated "Fold Style:"
     */
    @DefaultMessage("Fold Style:")
    @Key("displayFoldStyleLabel")
    String displayFoldStyleLabel();

    /**
     * Translated "Ensure that source files end with newline"
     *
     * @return translated "Ensure that source files end with newline"
     */
    @DefaultMessage("Ensure that source files end with newline")
    @Key("savingAutoAppendNewLineLabel")
    String savingAutoAppendNewLineLabel();

    /**
     * Translated "Strip trailing horizontal whitespace when saving"
     *
     * @return translated "Strip trailing horizontal whitespace when saving"
     */
    @DefaultMessage("Strip trailing horizontal whitespace when saving")
    @Key("savingStripTrailingWhitespaceLabel")
    String savingStripTrailingWhitespaceLabel();

    /**
     * Translated "Restore last cursor position when opening file"
     *
     * @return translated "Restore last cursor position when opening file"
     */
    @DefaultMessage("Restore last cursor position when opening file")
    @Key("savingRestoreSourceDocumentCursorPositionLabel")
    String savingRestoreSourceDocumentCursorPositionLabel();

    /**
     * Translated "Default text encoding:"
     *
     * @return translated "Default text encoding:"
     */
    @DefaultMessage("Default text encoding:")
    @Key("savingDefaultEncodingLabel")
    String savingDefaultEncodingLabel();

    /**
     * Translated "Always save R scripts before sourcing"
     *
     * @return translated "Always save R scripts before sourcing"
     */
    @DefaultMessage("Always save R scripts before sourcing")
    @Key("savingSaveBeforeSourcingLabel")
    String savingSaveBeforeSourcingLabel();

    /**
     * Translated "Automatically save when editor loses focus"
     *
     * @return translated "Automatically save when editor loses focus"
     */
    @DefaultMessage("Automatically save when editor loses focus")
    @Key("savingAutoSaveOnBlurLabel")
    String savingAutoSaveOnBlurLabel();

    /**
     * Translated "When editor is idle:"
     *
     * @return translated "When editor is idle:"
     */
    @DefaultMessage("When editor is idle:")
    @Key("savingAutoSaveOnIdleLabel")
    String savingAutoSaveOnIdleLabel();

    /**
     * Translated "Idle period:"
     *
     * @return translated "Idle period:"
     */
    @DefaultMessage("Idle period:")
    @Key("savingAutoSaveIdleMsLabel")
    String savingAutoSaveIdleMsLabel();

    /**
     * Translated "Show code completions:"
     *
     * @return translated "Show code completions:"
     */
    @DefaultMessage("Show code completions:")
    @Key("completionCodeCompletionLabel")
    String completionCodeCompletionLabel();

    /**
     * Translated "Show code completions:"
     *
     * @return translated "Show code completions:"
     */
    @DefaultMessage("Show code completions:")
    @Key("completionCodeCompletionOtherLabel")
    String completionCodeCompletionOtherLabel();

    /**
     * Translated "Allow automatic completions in console"
     *
     * @return translated "Allow automatic completions in console"
     */
    @DefaultMessage("Allow automatic completions in console")
    @Key("completionConsoleCodeCompletionLabel")
    String completionConsoleCodeCompletionLabel();

    /**
     * Translated "Insert parentheses after function completions"
     *
     * @return translated "Insert parentheses after function completions"
     */
    @DefaultMessage("Insert parentheses after function completions")
    @Key("completionInsertParensAfterFunctionCompletion")
    String completionInsertParensAfterFunctionCompletion();

    /**
     * Translated "Show help tooltip after function completions"
     *
     * @return translated "Show help tooltip after function completions"
     */
    @DefaultMessage("Show help tooltip after function completions")
    @Key("completionShowFunctionSignatureTooltipsLabel")
    String completionShowFunctionSignatureTooltipsLabel();

    /**
     * Translated "Show help tooltip on cursor idle"
     *
     * @return translated "Show help tooltip on cursor idle"
     */
    @DefaultMessage("Show help tooltip on cursor idle")
    @Key("completionShowHelpTooltipOnIdleLabel")
    String completionShowHelpTooltipOnIdleLabel();

    /**
     * Translated "Insert spaces around equals for argument completions"
     *
     * @return translated "Insert spaces around equals for argument completions"
     */
    @DefaultMessage("Insert spaces around equals for argument completions")
    @Key("completionInsertSpacesAroundEqualsLabel")
    String completionInsertSpacesAroundEqualsLabel();

    /**
     * Translated "Use tab for autocompletions"
     *
     * @return translated "Use tab for autocompletions"
     */
    @DefaultMessage("Use tab for autocompletions")
    @Key("completionTabCompletionLabel")
    String completionTabCompletionLabel();

    /**
     * Translated "Use tab for multiline autocompletions"
     *
     * @return translated "Use tab for multiline autocompletions"
     */
    @DefaultMessage("Use tab for multiline autocompletions")
    @Key("completionTabMultilineCompletionLabel")
    String completionTabMultilineCompletionLabel();

    /**
     * Translated "Show completions after characters entered:"
     *
     * @return translated "Show completions after characters entered:"
     */
    @DefaultMessage("Show completions after characters entered:")
    @Key("completionCodeCompletionCharactersLabel")
    String completionCodeCompletionCharactersLabel();

    /**
     * Translated "Show completions after keyboard idle (ms):"
     *
     * @return translated "Show completions after keyboard idle (ms):"
     */
    @DefaultMessage("Show completions after keyboard idle (ms):")
    @Key("completionCodeCompletionDelayLabel")
    String completionCodeCompletionDelayLabel();

    /**
     * Translated "Show diagnostics for R"
     *
     * @return translated "Show diagnostics for R"
     */
    @DefaultMessage("Show diagnostics for R")
    @Key("diagnosticsShowDiagnosticsRLabel")
    String diagnosticsShowDiagnosticsRLabel();

    /**
     * Translated "Enable diagnostics within R function calls"
     *
     * @return translated "Enable diagnostics within R function calls"
     */
    @DefaultMessage("Enable diagnostics within R function calls")
    @Key("diagnosticsInRFunctionCallsLabel")
    String diagnosticsInRFunctionCallsLabel();

    /**
     * Translated "Check arguments to R function calls"
     *
     * @return translated "Check arguments to R function calls"
     */
    @DefaultMessage("Check arguments to R function calls")
    @Key("diagnosticsCheckArgumentsToRFunctionCallsLabel")
    String diagnosticsCheckArgumentsToRFunctionCallsLabel();

    /**
     * Translated "Check usage of '<-' in function call"
     *
     * @return translated "Check usage of '<-' in function call"
     */
    @DefaultMessage("Check usage of '<-' in function call")
    @Key("diagnosticsCheckUnexpectedAssignmentInFunctionCallLabel")
    String diagnosticsCheckUnexpectedAssignmentInFunctionCallLabel();

    /**
     * Translated "Warn if variable used has no definition in scope"
     *
     * @return translated "Warn if variable used has no definition in scope"
     */
    @DefaultMessage("Warn if variable used has no definition in scope")
    @Key("diagnosticsWarnIfNoSuchVariableInScopeLabel")
    String diagnosticsWarnIfNoSuchVariableInScopeLabel();

    /**
     * Translated "Warn if variable is defined but not used"
     *
     * @return translated "Warn if variable is defined but not used"
     */
    @DefaultMessage("Warn if variable is defined but not used")
    @Key("diagnosticsWarnVariableDefinedButNotUsedLabel")
    String diagnosticsWarnVariableDefinedButNotUsedLabel();

    /**
     * Translated "Provide R style diagnostics (e.g. whitespace)"
     *
     * @return translated "Provide R style diagnostics (e.g. whitespace)"
     */
    @DefaultMessage("Provide R style diagnostics (e.g. whitespace)")
    @Key("diagnosticsStyleDiagnosticsLabel")
    String diagnosticsStyleDiagnosticsLabel();

    /**
     * Translated "Prompt to install missing R packages discovered in R source files"
     *
     * @return translated "Prompt to install missing R packages discovered in R source files"
     */
    @DefaultMessage("Prompt to install missing R packages discovered in R source files")
    @Key("diagnosticsAutoDiscoverPackageDependenciesLabel")
    String diagnosticsAutoDiscoverPackageDependenciesLabel();

    /**
     * Translated "Show diagnostics for C/C++"
     *
     * @return translated "Show diagnostics for C/C++"
     */
    @DefaultMessage("Show diagnostics for C/C++")
    @Key("diagnosticsShowDiagnosticsCppLabel")
    String diagnosticsShowDiagnosticsCppLabel();

    /**
     * Translated "Show diagnostics for YAML"
     *
     * @return translated "Show diagnostics for YAML"
     */
    @DefaultMessage("Show diagnostics for YAML")
    @Key("diagnosticsShowDiagnosticsYamlLabel")
    String diagnosticsShowDiagnosticsYamlLabel();

    /**
     * Translated "Show diagnostics for JavaScript, HTML, and CSS"
     *
     * @return translated "Show diagnostics for JavaScript, HTML, and CSS"
     */
    @DefaultMessage("Show diagnostics for JavaScript, HTML, and CSS")
    @Key("diagnosticsShowDiagnosticsOtherLabel")
    String diagnosticsShowDiagnosticsOtherLabel();

    /**
     * Translated "Show diagnostics whenever source files are saved"
     *
     * @return translated "Show diagnostics whenever source files are saved"
     */
    @DefaultMessage("Show diagnostics whenever source files are saved")
    @Key("diagnosticsOnSaveLabel")
    String diagnosticsOnSaveLabel();

    /**
     * Translated "Show diagnostics after keyboard is idle for a period of time"
     *
     * @return translated "Show diagnostics after keyboard is idle for a period of time"
     */
    @DefaultMessage("Show diagnostics after keyboard is idle for a period of time")
    @Key("diagnosticsBackgroundDiagnosticsLabel")
    String diagnosticsBackgroundDiagnosticsLabel();

    /**
     * Translated "Keyboard idle time (ms):"
     *
     * @return translated "Keyboard idle time (ms):"
     */
    @DefaultMessage("Keyboard idle time (ms):")
    @Key("diagnosticsBackgroundDiagnosticsDelayMsLabel")
    String diagnosticsBackgroundDiagnosticsDelayMsLabel();

    /**
     * Translated "Show full path to project in window title"
     *
     * @return translated "Show full path to project in window title"
     */
    @DefaultMessage("Show full path to project in window title")
    @Key("fullProjectPathInWindowTitleLabel")
    String fullProjectPathInWindowTitleLabel();
}
