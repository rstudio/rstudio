/*
 * PrefsConstants.java
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

package org.rstudio.studio.client.workbench.prefs;

public interface PrefsConstants extends com.google.gwt.i18n.client.Messages  {

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
}
