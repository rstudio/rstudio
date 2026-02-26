/*
 * ChatConstants.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.views.chat;

public interface ChatConstants extends com.google.gwt.i18n.client.Messages {
    String chatTitle();
    String chatTabLabel();
    String checkingInstallationMessage();
    String chatNotInstalledMessage();
    String startingChatMessage();
    String restartingChatMessage();
    String chatUpdating();
    String chatUpdateComplete();
    String chatUpdateFailed(String error);
    String chatUpdateCheckFailed();
    String chatRetry();
    String chatDismiss();
    String chatIgnore();
    String chatNotInstalledTitle();
    String chatNotInstalledWithVersionMessage(String version);
    String chatNotInstalledDescription();
    String chatLearnMore();
    String chatInstallButton();
    String chatUpdateAvailableTitle();
    String chatUpdateAvailableWithVersionsMessage(String currentVersion, String newVersion);
    String chatUpdateButton();
    String chatIncompatibleVersion();
    String chatErrorPrefix(String error);
    String chatUpdateRequiredTitle();
    String chatRStudioTooOldMessage();
    String chatAssistantTooOldMessage();
    String chatProcessExitedTitle();
    String chatProcessExitedMessage();
    String chatRestartButton();
    String chatBackendStartFailed(String error);
    String chatBackendStartError(String error);
    String chatUpdateTimeout();
    String chatUpdateNotStarted();
    String chatUpdateStatusUnknown(String status);
    String chatUpdateStatusCheckFailed(String error);
    String chatBackendStartTimeout();
    String chatBackendStatusCheckFailed(String error);
    String chatRestartFailed(String error);
    String chatAssistantNotEnabledTitle();
    String chatAssistantNotEnabledMessage();
    String chatGlobalOptionsButton();
}
