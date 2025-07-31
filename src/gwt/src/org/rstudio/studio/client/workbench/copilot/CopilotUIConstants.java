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

    @Key("copilotDiagnosticsTitle")
    String copilotDiagnosticsTitle();
   

    @Key("copilotSignInDialogTitle")
    String copilotSignInDialogTitle();

    @Key("copilotSignOutDialogTitle")
    String copilotSignOutDialogTitle();

    @Key("copilotAuthenticating")
    String copilotAuthenticating();

    @Key("copilotVerifyingInstallation")
    String copilotVerifyingInstallation();

    @Key("copilotSignedIn")
    String copilotSignedIn(String user);

    @Key("copilotSigningIn")
    String copilotSigningIn();

    @Key("copilotSigningOut")
    String copilotSigningOut();

    @Key("copilotError")
    String copilotError(int code, String message);

    @Key("copilotAlreadySignedIn")
    String copilotAlreadySignedIn(String name);

    @Key("copilotSignedOut")
    String copilotSignedOut();

    @Key("copilotCheckingStatus")
    String copilotCheckingStatus();

    @Key("copilotCheckStatusDialogTitle")
    String copilotCheckStatusDialogTitle();

    @Key("copilotStatusDialogTitle")
    String copilotStatusDialogTitle();

    @Key("copilotEmptyResponse")
    String copilotEmptyResponse();

    @Key("copilotNoOutput")
    String copilotNoOutput();

    @Key("copilotErrorStartingAgent")
    String copilotErrorStartingAgent(String error, String output);

    @Key("copilotNotSignedIn")
    String copilotNotSignedIn();

    @Key("copilotNotSignedInShort")
    String copilotNotSignedInShort();

    @Key("copilotCurrentlySignedIn")
    String copilotCurrentlySignedIn(String user);

    @Key("copilotUnknownError")
    String copilotUnknownError();

    @Key("copilotNotInstalledError")
    String copilotNotInstalledError();

    @Key("copilotDisabledByAdministratorError")
    String copilotDisabledByAdministratorError();

    @Key("copilotDisabledViaProjectPreferencesError")
    String copilotDisabledViaProjectPreferencesError();

    @Key("copilotDisabledViaGlobalOptionsError")
    String copilotDisabledViaGlobalOptionsError();

    @Key("copilotLaunchError")
    String copilotLaunchError();

    @Key("copilotUnknownErrorShort")
    String copilotUnknownErrorShort();

}
