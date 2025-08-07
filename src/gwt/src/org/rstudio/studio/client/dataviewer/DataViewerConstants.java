/*
 * DataViewerConstants.java
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

package org.rstudio.studio.client.dataviewer;

import com.google.gwt.i18n.client.Constants;

public interface DataViewerConstants extends Constants {

    /**
     * Translate "Cols:".
     *
     * @return the translated value
     */
    @DefaultStringValue("Cols:")
    String colsLabel();

    /**
     * Translate "Filter".
     *
     * @return the translated value
     */
    @DefaultStringValue("Filter")
    String filterButtonText();

    /**
     * Translate "Search data table".
     *
     * @return the translated value
     */
    @DefaultStringValue("Search data table")
    String searchWidgetLabel();

    /**
     * Translate "(Displaying up to 1,000 records)".
     *
     * @return the translated value
     */
    @DefaultStringValue("(Displaying up to 1,000 records)")
    String toolbarLabel();
}
