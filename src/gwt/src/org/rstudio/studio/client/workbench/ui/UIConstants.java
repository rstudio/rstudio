/*
 * UIConstants.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.ui;

public interface UIConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "weaving Rnw files".
     *
     * @return translated "weaving Rnw files"
     */
    @DefaultMessage("weaving Rnw files")
    @Key("weavingRnwFilesText")
    String weavingRnwFilesText();

    /**
     * Translated "LaTeX typesetting".
     *
     * @return translated "LaTeX typesetting"
     */
    @DefaultMessage("LaTeX typesetting")
    @Key("latexTypesettingText")
    String latexTypesettingText();

    /**
     * Translated "Project Option Unchanged".
     *
     * @return translated "Project Option Unchanged"
     */
    @DefaultMessage("Project Option Unchanged")
    @Key("projectOptionUnchangedCaption")
    String projectOptionUnchangedCaption();

    /**
     * Translated "You changed the global option for {0} to {1}, however the current project is still configured to use {2}.\n\nDo you want to edit the options for the current project as well?".
     *
     * @return translated "You changed the global option for {0} to {1}, however the current project is still configured to use {2}.\n\nDo you want to edit the options for the current project as well?"
     */
    @DefaultMessage("You changed the global option for {0} to {1}, however the current project is still configured to use {2}.\\n\\nDo you want to edit the options for the current project as well?")
    @Key("projectOptionUnchangedMessage")
    String projectOptionUnchangedMessage(String valueName, String globalValue, String value);

    /**
     * Translated "Save Selected".
     *
     * @return translated "Save Selected"
     */
    @DefaultMessage("Save Selected")
    @Key("saveSelectedCaption")
    String saveSelectedCaption();

    /**
     * Translated "Don't Save".
     *
     * @return translated "Don't Save"
     */
    @DefaultMessage("Don''t Save")
    @Key("dontSaveButtonText")
    String dontSaveButtonText();

    /**
     * Translated "The following file has unsaved changes:".
     *
     * @return translated "The following file has unsaved changes:"
     */
    @DefaultMessage("The following file has unsaved changes:")
    @Key("fileUnsavedChangesText")
    String fileUnsavedChangesText();

    /**
     * Translated "The following {0} files have unsaved changes:".
     *
     * @return translated "The following {0} files have unsaved changes:"
     */
    @DefaultMessage("The following {0} files have unsaved changes:")
    @Key("filesUnsavedChangesText")
    String filesUnsavedChangesText(int size);

    /**
     * Translated "Cannot Add Column".
     *
     * @return translated "Cannot Add Column"
     */
    @DefaultMessage("Cannot Add Column")
    @Key("cannotAddColumnText")
    String cannotAddColumnText();

    /**
     * Translated "You can''t add more than {0} columns.".
     *
     * @return translated "You can''t add more than {0} columns."
     */
    @DefaultMessage("You can''t add more than {0} columns.")
    @Key("cannotAddMoreColumnsText")
    String cannotAddMoreColumnsText(int maxColumnCount);

    /**
     * Translated "Close".
     *
     * @return translated "Close"
     */
    @DefaultMessage("Close")
    @Key("closeText")
    String closeText();


}
