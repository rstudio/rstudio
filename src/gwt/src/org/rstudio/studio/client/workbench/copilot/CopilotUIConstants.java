/*
 * CopilotConstants.java
 *
 * Copyright (C) 2024 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.copilot;

public interface CopilotUIConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "GitHub Copilot: Diagnostic Report"
     *
     * @return translated "GitHub Copilot: Diagnostic Report"
     */
    @DefaultMessage("GitHub Copilot: Diagnostic Report")
    @Key("copilotDiagnosticsTitle")
    String copilotDiagnosticsTitle();
   
    /**
     * Translated "GitHub Copilot: Sign in"
     *
     * @return translated "GitHub Copilot: Sign in"
     */
    @DefaultMessage("GitHub Copilot: Sign in")
    @Key("copilotSignInDialogTitle")
    String copilotSignInDialogTitle();

    /**
     * Translated "GitHub Copilot: Sign out"
     *
     * @return translated "GitHub Copilot: Sign out"
     */
    @DefaultMessage("GitHub Copilot: Sign out")
    @Key("copilotSignOutDialogTitle")
    String copilotSignOutDialogTitle();

    /**
     * Translated "Authenticating..."
     *
     * @return translated "Authenticating..."
     */
    @DefaultMessage("Authenticating...")
    @Key("copilotAuthenticating")
    String copilotAuthenticating();

    /**
     * Translated "Verifying copilot installation..."
     *
     * @return translated "Verifying copilot installation..."
     */
    @DefaultMessage("Verifying copilot installation...")
    @Key("copilotVerifyingInstallation")
    String copilotVerifyingInstallation();

    /**
     * Translated "You are now signed in as ''{0}''."
     *
     * @return translated "You are now signed in as ''{0}''."
     */
    @DefaultMessage("You are now signed in as ''{0}''.")
    @Key("copilotSignedIn")
    String copilotSignedIn(String user);

    /**
     * Translated "Signing in..."
     *
     * @return translated "Signing in..."
     */
    @DefaultMessage("Signing in...")
    @Key("copilotSigningIn")
    String copilotSigningIn();

    /**
     * Translated "Signing out..."
     *
     * @return translated "Signing out..."
     */
    @DefaultMessage("Signing out...")
    @Key("copilotSigningOut")
    String copilotSigningOut();

    /**
     * Translated "Error {0}: {1}"
     *
     * @return translated "Error {0}: {1}"
     */
    @DefaultMessage("Error {0}: {1}")
    @Key("copilotError")
    String copilotError(int code, String message);

    /**
     * Translated "You are already signed in as ''{0}''.\n\nIf you''d like to sign in as a different user, please sign out from this account first."
     *
     * @return translated "You are already signed in as ''{0}''.\n\nIf you''d like to sign in as a different user, please sign out from this account first."
     */
    @DefaultMessage("You are already signed in as ''{0}''.\n\nIf you''d like to sign in as a different user, please sign out from this account first.")
    @Key("copilotAlreadySignedIn")
    String copilotAlreadySignedIn(String name);

    /**
     * Translated "You have successfully signed out from GitHub Copilot"
     *
     * @return translated "You have successfully signed out from GitHub Copilot"
     */
    @DefaultMessage("You have successfully signed out from GitHub Copilot")
    @Key("copilotSignedOut")
    String copilotSignedOut();

    /**
     * Translated "Checking status..."
     *
     * @return translated "Checking status..."
     */
    @DefaultMessage("Checking status...")
    @Key("copilotCheckingStatus")
    String copilotCheckingStatus();

    /**
     * Translated "GitHub Copilot: Check Status"
     *
     * @return translated "GitHub Copilot: Check Status"
     */
    @DefaultMessage("GitHub Copilot: Check Status")
    @Key("copilotCheckStatusDialogTitle")
    String copilotCheckStatusDialogTitle();

    /**
     * Translated "GitHub Copilot: Status"
     *
     * @return translated "GitHub Copilot: Status"
     */
    @DefaultMessage("GitHub Copilot: Status")
    @Key("copilotStatusDialogTitle")
    String copilotStatusDialogTitle();

    /**
     * Translated "RStudio received an unexpected empty response from the GitHub Copilot agent."
     *
     * @return translated "RStudio received an unexpected empty response from the GitHub Copilot agent."
     */
    @DefaultMessage("RStudio received an unexpected empty response from the GitHub Copilot agent.")
    @Key("copilotEmptyResponse")
    String copilotEmptyResponse();

    /**
     * Translated "(no output available)"
     *
     * @return translated "(no output available)"
     */
    @DefaultMessage("(no output available)")
    @Key("copilotNoOutput")
    String copilotNoOutput();

    /**
     * Translated "An error occurred while starting the GitHub Copilot agent.\n\nError: {0}\n\nOutput: {1}"
     *
     * @return translated "An error occurred while starting the GitHub Copilot agent.\n\nError: {0}\n\nOutput: {1}"
     */
    @DefaultMessage("An error occurred while starting the GitHub Copilot agent.\n\nError: {0}\n\nOutput: {1}")
    @Key("copilotErrorStartingAgent")
    String copilotErrorStartingAgent(String error, String output);

    /**
     * Translated "The GitHub Copilot agent is running, but you have not yet signed in."
     *
     * @return translated "The GitHub Copilot agent is running, but you have not yet signed in."
     */
    @DefaultMessage("The GitHub Copilot agent is running, but you have not yet signed in.")
    @Key("copilotNotSignedIn")
    String copilotNotSignedIn();

    /**
     * Translated "Not signed in."
     *
     * @return translated "Not signed in."
     */
    @DefaultMessage("Not signed in.")
    @Key("copilotNotSignedInShort")
    String copilotNotSignedInShort();

    /**
     * Translated "You are currently signed in as: {0}"
     *
     * @return translated "You are currently signed in as: {0}"
     */
    @DefaultMessage("You are currently signed in as: {0}")
    @Key("copilotCurrentlySignedIn")
    String copilotCurrentlySignedIn(String user);

    /**
     * Translated "An unknown error occurred."
     *
     * @return translated "An unknown error occurred."
     */
    @DefaultMessage("An unknown error occurred.")
    @Key("copilotUnknownError")
    String copilotUnknownError();

    /**
     * Translated "The GitHub Copilot agent is not installed."
     *
     * @return translated "The GitHub Copilot agent is not installed."
     */
    @DefaultMessage("The GitHub Copilot agent is not installed.")
    @Key("copilotNotInstalledError")
    String copilotNotInstalledError();

    /**
     * Translated "GitHub Copilot has been disabled by the system administrator."
     *
     * @return translated "GitHub Copilot has been disabled by the system administrator."
     */
    @DefaultMessage("GitHub Copilot has been disabled by the system administrator.")
    @Key("copilotDisabledByAdministratorError")
    String copilotDisabledByAdministratorError();

    /**
     * Translated "GitHub Copilot has been disabled via project preferences."
     *
     * @return translated "GitHub Copilot has been disabled via project preferences."
     */
    @DefaultMessage("GitHub Copilot has been disabled via project preferences.")
    @Key("copilotDisabledViaProjectPreferencesError")
    String copilotDisabledViaProjectPreferencesError();

    /**
     * Translated "GitHub Copilot has been disabled via global options."
     *
     * @return translated "GitHub Copilot has been disabled via global options."
     */
    @DefaultMessage("GitHub Copilot has been disabled via global options.")
    @Key("copilotDisabledViaGlobalOptionsError")
    String copilotDisabledViaGlobalOptionsError();

    /**
     * Translated "An error occurred while attempting to launch GitHub Copilot."
     *
     * @return translated "An error occurred while attempting to launch GitHub Copilot."
     */
    @DefaultMessage("An error occurred while attempting to launch GitHub Copilot.")
    @Key("copilotLaunchError")
    String copilotLaunchError();

    /**
     * Translated "[unknown]"
     *
     * @return translated "[unknown]"
     */
    @DefaultMessage("[unknown]")
    @Key("copilotUnknownErrorShort")
    String copilotUnknownErrorShort();

}
