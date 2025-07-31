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

    @DefaultMessage("GitHub Copilot: Diagnostic Report")
    @Key("copilotDiagnosticsTitle")
    String copilotDiagnosticsTitle();
   

    @DefaultMessage("GitHub Copilot: Sign in")
    @Key("copilotSignInDialogTitle")
    String copilotSignInDialogTitle();

    @DefaultMessage("GitHub Copilot: Sign out")
    @Key("copilotSignOutDialogTitle")
    String copilotSignOutDialogTitle();

    @DefaultMessage("Authenticating...")
    @Key("copilotAuthenticating")
    String copilotAuthenticating();

    @DefaultMessage("Verifying copilot installation...")
    @Key("copilotVerifyingInstallation")
    String copilotVerifyingInstallation();

    @DefaultMessage("You are now signed in as ''{0}''.")
    @Key("copilotSignedIn")
    String copilotSignedIn(String user);

    @DefaultMessage("Signing in...")
    @Key("copilotSigningIn")
    String copilotSigningIn();

    @DefaultMessage("Signing out...")
    @Key("copilotSigningOut")
    String copilotSigningOut();

    @DefaultMessage("Error {0}: {1}")
    @Key("copilotError")
    String copilotError(int code, String message);

    @DefaultMessage("You are already signed in as ''{0}''.\n\nIf you''d like to sign in as a different user, please sign out from this account first.")
    @Key("copilotAlreadySignedIn")
    String copilotAlreadySignedIn(String name);

    @DefaultMessage("You have successfully signed out from GitHub Copilot")
    @Key("copilotSignedOut")
    String copilotSignedOut();

    @DefaultMessage("Checking status...")
    @Key("copilotCheckingStatus")
    String copilotCheckingStatus();

    @DefaultMessage("GitHub Copilot: Check Status")
    @Key("copilotCheckStatusDialogTitle")
    String copilotCheckStatusDialogTitle();

    @DefaultMessage("GitHub Copilot: Status")
    @Key("copilotStatusDialogTitle")
    String copilotStatusDialogTitle();

    @DefaultMessage("RStudio received an unexpected empty response from the GitHub Copilot agent.")
    @Key("copilotEmptyResponse")
    String copilotEmptyResponse();

    @DefaultMessage("(no output available)")
    @Key("copilotNoOutput")
    String copilotNoOutput();

    @DefaultMessage("An error occurred while starting the GitHub Copilot agent.\n\nError: {0}\n\nOutput: {1}")
    @Key("copilotErrorStartingAgent")
    String copilotErrorStartingAgent(String error, String output);

    @DefaultMessage("The GitHub Copilot agent is running, but you have not yet signed in.")
    @Key("copilotNotSignedIn")
    String copilotNotSignedIn();

    @DefaultMessage("Not signed in.")
    @Key("copilotNotSignedInShort")
    String copilotNotSignedInShort();

    @DefaultMessage("You are currently signed in as: {0}")
    @Key("copilotCurrentlySignedIn")
    String copilotCurrentlySignedIn(String user);

    @DefaultMessage("An unknown error occurred.")
    @Key("copilotUnknownError")
    String copilotUnknownError();

    @DefaultMessage("The GitHub Copilot Language Server could not be located.")
    @Key("copilotNotInstalledError")
    String copilotNotInstalledError();

    @DefaultMessage("GitHub Copilot has been disabled by the system administrator.")
    @Key("copilotDisabledByAdministratorError")
    String copilotDisabledByAdministratorError();

    @DefaultMessage("GitHub Copilot has been disabled via project preferences.")
    @Key("copilotDisabledViaProjectPreferencesError")
    String copilotDisabledViaProjectPreferencesError();

    @DefaultMessage("GitHub Copilot has been disabled via global options.")
    @Key("copilotDisabledViaGlobalOptionsError")
    String copilotDisabledViaGlobalOptionsError();

    @DefaultMessage("An error occurred while attempting to launch GitHub Copilot.")
    @Key("copilotLaunchError")
    String copilotLaunchError();

    @DefaultMessage("[unknown]")
    @Key("copilotUnknownErrorShort")
    String copilotUnknownErrorShort();

}
