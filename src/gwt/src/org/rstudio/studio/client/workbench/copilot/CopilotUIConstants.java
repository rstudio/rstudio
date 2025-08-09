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
    String copilotDiagnosticsTitle();
    String copilotSignInDialogTitle();
    String copilotSignOutDialogTitle();
    String copilotAuthenticating();
    String copilotVerifyingInstallation();
    String copilotSignedIn(String user);
    String copilotSigningIn();
    String copilotSigningOut();
    String copilotError(int code, String message);
    String copilotAlreadySignedIn(String name);
    String copilotSignedOut();
    String copilotCheckingStatus();
    String copilotCheckStatusDialogTitle();
    String copilotStatusDialogTitle();
    String copilotEmptyResponse();
    String copilotNoOutput();
    String copilotErrorStartingAgent(String error, String output);
    String copilotNotSignedIn();
    String copilotNotSignedInShort();
    String copilotCurrentlySignedIn(String user);
    String copilotUnknownError();
    String copilotNotInstalledError();
    String copilotDisabledByAdministratorError();
    String copilotDisabledViaProjectPreferencesError();
    String copilotDisabledViaGlobalOptionsError();
    String copilotLaunchError();
    String copilotUnknownErrorShort();
}
