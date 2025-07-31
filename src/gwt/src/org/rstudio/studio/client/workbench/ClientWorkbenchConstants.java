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

    @Key("addinCaption")
    String addinCaption();

    @Key("executeButtonLabel")
    String executeButtonLabel();

    @Key("rstudioAddinsCaption")
    String rstudioAddinsCaption();

    @Key("loadingAddinsCaption")
    String loadingAddinsCaption();

    @Key("noAddinsAvailableCaption")
    String noAddinsAvailableCaption();

    @Key("keyboardShortcutsTitle")
    String keyboardShortcutsTitle();

    @Key("filterAddinsText")
    String filterAddinsText();

    @Key("packageTextHeader")
    String packageTextHeader();

    @Key("nameTextHeader")
    String nameTextHeader();

    @Key("descTextHeader")
    String descTextHeader();

    @Key("foundAddinsMessage")
    String foundAddinsMessage(int size, String query);

    @Key("onQuotaMessage")
    String onQuotaMessage(String fileSize, String quota);

    @Key("quotaStatusMessage")
    String quotaStatusMessage(String quota);

    @Key("chooseWorkingDirCaption")
    String chooseWorkingDirCaption();

    @Key("sourceFileCaption")
    String sourceFileCaption();

    @Key("rsaKeyProgressMessage")
    String rsaKeyProgressMessage();

    @Key("rsaPublicKeyCaption")
    String rsaPublicKeyCaption();

    @Key("onErrorReadKey")
    String onErrorReadKey(String keyPath, String userMessage);

    @Key("noLabel")
    String noLabel();

    @Key("yesLabel")
    String yesLabel();

    @Key("adminNotificationCaption")
    String adminNotificationCaption();

}
