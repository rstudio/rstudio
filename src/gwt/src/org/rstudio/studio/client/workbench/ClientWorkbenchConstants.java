/*
 * ClientWorkbenchConstants.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench;

public interface ClientWorkbenchConstants extends com.google.gwt.i18n.client.Messages {
    String addinCaption();
    String executeButtonLabel();
    String rstudioAddinsCaption();
    String loadingAddinsCaption();
    String noAddinsAvailableCaption();
    String keyboardShortcutsTitle();
    String filterAddinsText();
    String packageTextHeader();
    String nameTextHeader();
    String descTextHeader();
    String foundAddinsMessage(int size, String query);
    String onQuotaMessage(String fileSize, String quota);
    String quotaStatusMessage(String quota);
    String chooseWorkingDirCaption();
    String sourceFileCaption();
    String rsaKeyProgressMessage();
    String rsaPublicKeyCaption();
    String onErrorReadKey(String keyPath, String userMessage);
    String noLabel();
    String yesLabel();
    String adminNotificationCaption();
}
