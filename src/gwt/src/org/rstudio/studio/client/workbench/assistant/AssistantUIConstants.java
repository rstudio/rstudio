/*
 * AssistantUIConstants.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.assistant;

public interface AssistantUIConstants extends com.google.gwt.i18n.client.Messages
{
    String assistantDiagnosticsTitle(String assistantName);
    String assistantSignInDialogTitle(String assistantName);
    String assistantSignOutDialogTitle(String assistantName);
    String assistantAuthenticating();
    String assistantVerifyingInstallation(String assistantName);
    String assistantSignedIn(String user);
    String assistantSigningIn();
    String assistantSigningOut();
    String assistantError(int code, String message);
    String assistantAlreadySignedIn(String name);
    String assistantSignedOut(String assistantName);
    String assistantCheckingStatus();
    String assistantCheckStatusDialogTitle(String assistantName);
    String assistantStatusDialogTitle(String assistantName);
    String assistantEmptyResponse(String assistantName);
    String assistantNoOutput();
    String assistantErrorStartingAgent(String assistantName, String error, String output);
    String assistantNotSignedIn(String assistantName);
    String assistantNotSignedInShort();
    String assistantCurrentlySignedIn(String user);
    String assistantUnknownError();
    String assistantNotInstalledError(String assistantName);
    String assistantDisabledByAdministratorError(String assistantName);
    String assistantDisabledViaProjectPreferencesError(String assistantName);
    String assistantDisabledViaGlobalOptionsError(String assistantName);
    String assistantLaunchError(String assistantName);
    String assistantUnknownErrorShort();
}
