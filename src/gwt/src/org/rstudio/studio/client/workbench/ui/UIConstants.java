/*
 * UIConstants.java
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
package org.rstudio.studio.client.workbench.ui;

public interface UIConstants extends com.google.gwt.i18n.client.Messages {

    @Key("weavingRnwFilesText")
    String weavingRnwFilesText();

    @Key("latexTypesettingText")
    String latexTypesettingText();

    @Key("projectOptionUnchangedCaption")
    String projectOptionUnchangedCaption();

    @Key("projectOptionUnchangedMessage")
    String projectOptionUnchangedMessage(String valueName, String globalValue, String value);

    @Key("saveSelectedCaption")
    String saveSelectedCaption();

    @Key("dontSaveButtonText")
    String dontSaveButtonText();

    @Key("fileUnsavedChangesText")
    String fileUnsavedChangesText();

    @Key("filesUnsavedChangesText")
    String filesUnsavedChangesText(int size);

    @Key("cannotAddColumnText")
    String cannotAddColumnText();

    @Key("cannotAddMoreColumnsText")
    String cannotAddMoreColumnsText(int maxColumnCount);

    @Key("closeText")
    String closeText();

}
