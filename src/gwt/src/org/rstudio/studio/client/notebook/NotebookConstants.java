/*
 * DataViewerConstants.java
 *
 * Copyright (C) 2022 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.notebook;

import com.google.gwt.i18n.client.Constants;

public interface NotebookConstants extends Constants {

    /**
     * Translate "Compile Report from R Script".
     *
     * @return the translated value
     */
    @DefaultStringValue("Compile Report from R Script")
    @Key("compileNotebookOptionsDialogCaption")
    String compileNotebookOptionsDialogCaption();

    /**
     * Translate "Help on report types".
     *
     * @return the translated value
     */
    @DefaultStringValue("Help on report types")
    @Key("helpButtonTitle")
    String helpButtonTitle();

    /**
     * Translate "Compile".
     *
     * @return the translated value
     */
    @DefaultStringValue("Compile")
    @Key("okButtonCaption")
    String okButtonCaption();
}
