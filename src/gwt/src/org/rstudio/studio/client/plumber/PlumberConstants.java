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

package org.rstudio.studio.client.plumber;

import com.google.gwt.i18n.client.Constants;

public interface PlumberConstants extends Constants {

    /**
     * Translate "Running Plumber API".
     *
     * @return the translated value
     */
    @DefaultStringValue("Running Plumber API")
    @Key("runningPlumberApiUserAction")
    String runningPlumberApiUserAction();

    /**
     * Translate "Plumber API Launch Failed".
     *
     * @return the translated value
     */
    @DefaultStringValue("Plumber API Launch Failed")
    @Key("apiLaunchFailedCaption")
    String apiLaunchFailedCaption();

    /**
     * Translate "Plumber API Panel".
     *
     * @return the translated value
     */
    @DefaultStringValue("Plumber API Panel")
    @Key("plumberApiPanelTitle")
    String plumberApiPanelTitle();

    /**
     * Translate "Open in Browser".
     *
     * @return the translated value
     */
    @DefaultStringValue("Open in Browser")
    @Key("openInBrowserButtonText")
    String openInBrowserButtonText();

}
