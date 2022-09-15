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

    /**
     * Translated "Addins".
     *
     * @return translated "Addins"
     */
    @DefaultMessage("Addins")
    @Key("addinCaption")
    String addinCaption();

    /**
     * Translated "Execute".
     *
     * @return translated "Execute"
     */
    @DefaultMessage("Execute")
    @Key("executeButtonLabel")
    String executeButtonLabel();

    /**
     * Translated "Using RStudio Addins".
     *
     * @return translated "Using RStudio Addins"
     */
    @DefaultMessage("Using RStudio Addins")
    @Key("rstudioAddinsCaption")
    String rstudioAddinsCaption();

    /**
     * Translated "Loading addins...".
     *
     * @return translated "Loading addins..."
     */
    @DefaultMessage("Loading addins...")
    @Key("loadingAddinsCaption")
    String loadingAddinsCaption();

    /**
     * Translated "No addins available".
     *
     * @return translated "No addins available"
     */
    @DefaultMessage("No addins available")
    @Key("noAddinsAvailableCaption")
    String noAddinsAvailableCaption();

    /**
     * Translated "Keyboard Shortcuts...".
     *
     * @return translated "Keyboard Shortcuts..."
     */
    @DefaultMessage("Keyboard Shortcuts...")
    @Key("keyboardShortcutsTitle")
    String keyboardShortcutsTitle();

    /**
     * Translated "Filter addins:".
     *
     * @return translated "Filter addins:"
     */
    @DefaultMessage("Filter addins:")
    @Key("filterAddinsText")
    String filterAddinsText();

    /**
     * Translated "Package".
     *
     * @return translated "Package"
     */
    @DefaultMessage("Package")
    @Key("packageTextHeader")
    String packageTextHeader();

    /**
     * Translated "Name".
     *
     * @return translated "Name"
     */
    @DefaultMessage("Name")
    @Key("nameTextHeader")
    String nameTextHeader();

    /**
     * Translated "Description".
     *
     * @return translated "Description"
     */
    @DefaultMessage("Description")
    @Key("descTextHeader")
    String descTextHeader();

    /**
     * Translated "Found {0} addins matching {1}".
     *
     * @return translated "Found {0} addins matching {1}"
     */
    @DefaultMessage("Found {0} addins matching {1}")
    @Key("foundAddinsMessage")
    String foundAddinsMessage(int size, String query);

    /**
     * Translated "You are {0} over your {1} file storage limit. Please remove files to continue working.".
     *
     * @return translated "You are {0} over your {1} file storage limit. Please remove files to continue working."
     */
    @DefaultMessage("You are {0} over your {1} file storage limit. Please remove files to continue working.")
    @Key("onQuotaMessage")
    String onQuotaMessage(String fileSize, String quota);

    /**
     * Translated "You are nearly over your {0} file storage limit.".
     *
     * @return translated "You are nearly over your {0} file storage limit."
     */
    @DefaultMessage("You are nearly over your {0} file storage limit.")
    @Key("quotaStatusMessage")
    String quotaStatusMessage(String quota);

    /**
     * Translated "Choose Working Directory".
     *
     * @return translated "Choose Working Directory"
     */
    @DefaultMessage("Choose Working Directory")
    @Key("chooseWorkingDirCaption")
    String chooseWorkingDirCaption();

    /**
     * Translated "Source File".
     *
     * @return translated "Source File"
     */
    @DefaultMessage("Source File")
    @Key("sourceFileCaption")
    String sourceFileCaption();

    /**
     * Translated "Reading RSA public key...".
     *
     * @return translated "Reading RSA public key..."
     */
    @DefaultMessage("Reading RSA public key...")
    @Key("rsaKeyProgressMessage")
    String rsaKeyProgressMessage();

    /**
     * Translated "RSA Public Key".
     *
     * @return translated "RSA Public Key"
     */
    @DefaultMessage("RSA Public Key")
    @Key("rsaPublicKeyCaption")
    String rsaPublicKeyCaption();

    /**
     * Translated "Error attempting to read key ''{0}' ({1})".
     *
     * @return translated "Error attempting to read key ''{0}' ({1})"
     */
    @DefaultMessage("Error attempting to read key ''{0}'' ({1})")
    @Key("onErrorReadKey")
    String onErrorReadKey(String keyPath, String userMessage);

    /**
     * Translated "May we upload crash reports to RStudio automatically?\n\nCrash reports don't include any personal information, except for IP addresses which are used to determine how many users are affected by each crash.\n\nCrash reporting can be disabled at any time under the Global Options.".
     *
     * @return translated "May we upload crash reports to RStudio automatically?\n\nCrash reports don't include any personal information, except for IP addresses which are used to determine how many users are affected by each crash.\n\nCrash reporting can be disabled at any time under the Global Options."
     */
    @DefaultMessage("May we upload crash reports to RStudio automatically?\\n\\nCrash reports don''t include any personal information, except for IP addresses which are used to determine how many users are affected by each crash.\\n\\nCrash reporting can be disabled at any time under the Global Options.")
    @Key("checkForCrashHandlerPermissionMessage")
    String checkForCrashHandlerPermissionMessage();

    /**
     * Translated "Enable Automated Crash Reporting".
     *
     * @return translated "Enable Automated Crash Reporting"
     */
    @DefaultMessage("Enable Automated Crash Reporting")
    @Key("enableCrashReportingCaption")
    String enableCrashReportingCaption();

    /**
     * Translated "No".
     *
     * @return translated "No"
     */
    @DefaultMessage("No")
    @Key("noLabel")
    String noLabel();

    /**
     * Translated "No".
     *
     * @return translated "Yes"
     */
    @DefaultMessage("Yes")
    @Key("yesLabel")
    String yesLabel();

    /**
     * Translated "Admin Notification".
     *
     * @return translated "Admin Notification"
     */
    @DefaultMessage("Admin Notification")
    @Key("adminNotificationCaption")
    String adminNotificationCaption();

}
