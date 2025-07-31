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

    @DefaultMessage("weaving Rnw files")
    @Key("weavingRnwFilesText")
    String weavingRnwFilesText();

    @DefaultMessage("LaTeX typesetting")
    @Key("latexTypesettingText")
    String latexTypesettingText();

    @DefaultMessage("Project Option Unchanged")
    @Key("projectOptionUnchangedCaption")
    String projectOptionUnchangedCaption();

    @DefaultMessage("You changed the global option for {0} to {1}, however the current project is still configured to use {2}.\\n\\nDo you want to edit the options for the current project as well?")
    @Key("projectOptionUnchangedMessage")
    String projectOptionUnchangedMessage(String valueName, String globalValue, String value);

    @DefaultMessage("Save Selected")
    @Key("saveSelectedCaption")
    String saveSelectedCaption();

    @DefaultMessage("Don''t Save")
    @Key("dontSaveButtonText")
    String dontSaveButtonText();

    @DefaultMessage("The following file has unsaved changes:")
    @Key("fileUnsavedChangesText")
    String fileUnsavedChangesText();

    @DefaultMessage("The following {0} files have unsaved changes:")
    @Key("filesUnsavedChangesText")
    String filesUnsavedChangesText(int size);

    @DefaultMessage("Cannot Add Column")
    @Key("cannotAddColumnText")
    String cannotAddColumnText();

    @DefaultMessage("You can''t add more than {0} columns.")
    @Key("cannotAddMoreColumnsText")
    String cannotAddMoreColumnsText(int maxColumnCount);

    @DefaultMessage("Close")
    @Key("closeText")
    String closeText();

}
