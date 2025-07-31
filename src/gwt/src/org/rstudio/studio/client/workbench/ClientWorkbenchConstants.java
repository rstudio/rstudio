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

    @DefaultMessage("Addins")
    @Key("addinCaption")
    String addinCaption();

    @DefaultMessage("Execute")
    @Key("executeButtonLabel")
    String executeButtonLabel();

    @DefaultMessage("Using RStudio Addins")
    @Key("rstudioAddinsCaption")
    String rstudioAddinsCaption();

    @DefaultMessage("Loading addins...")
    @Key("loadingAddinsCaption")
    String loadingAddinsCaption();

    @DefaultMessage("No addins available")
    @Key("noAddinsAvailableCaption")
    String noAddinsAvailableCaption();

    @DefaultMessage("Keyboard Shortcuts...")
    @Key("keyboardShortcutsTitle")
    String keyboardShortcutsTitle();

    @DefaultMessage("Filter addins:")
    @Key("filterAddinsText")
    String filterAddinsText();

    @DefaultMessage("Package")
    @Key("packageTextHeader")
    String packageTextHeader();

    @DefaultMessage("Name")
    @Key("nameTextHeader")
    String nameTextHeader();

    @DefaultMessage("Description")
    @Key("descTextHeader")
    String descTextHeader();

    @DefaultMessage("Found {0} addins matching {1}")
    @Key("foundAddinsMessage")
    String foundAddinsMessage(int size, String query);

    @DefaultMessage("You are {0} over your {1} file storage limit. Please remove files to continue working.")
    @Key("onQuotaMessage")
    String onQuotaMessage(String fileSize, String quota);

    @DefaultMessage("You are nearly over your {0} file storage limit.")
    @Key("quotaStatusMessage")
    String quotaStatusMessage(String quota);

    @DefaultMessage("Choose Working Directory")
    @Key("chooseWorkingDirCaption")
    String chooseWorkingDirCaption();

    @DefaultMessage("Source File")
    @Key("sourceFileCaption")
    String sourceFileCaption();

    @DefaultMessage("Reading RSA public key...")
    @Key("rsaKeyProgressMessage")
    String rsaKeyProgressMessage();

    @DefaultMessage("RSA Public Key")
    @Key("rsaPublicKeyCaption")
    String rsaPublicKeyCaption();

    @DefaultMessage("Error attempting to read key ''{0}'' ({1})")
    @Key("onErrorReadKey")
    String onErrorReadKey(String keyPath, String userMessage);

    @DefaultMessage("No")
    @Key("noLabel")
    String noLabel();

    @DefaultMessage("Yes")
    @Key("yesLabel")
    String yesLabel();

    @DefaultMessage("Admin Notification")
    @Key("adminNotificationCaption")
    String adminNotificationCaption();

}
