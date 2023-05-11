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

package org.rstudio.studio.client.shiny;

import com.google.gwt.i18n.client.Messages;

public interface ShinyConstants extends Messages {

    /**
     * Translate "Running Shiny applications".
     *
     * @return the translated value
     */
    @DefaultMessage("Running Shiny applications")
    @Key("runningShinyUserAction")
    String runningShinyUserAction();

    /**
     * Translate "Failed to reload".
     *
     * @return the translated value
     */
    @DefaultMessage("Failed to reload")
    @Key("reloadFailErrorCaption")
    String reloadFailErrorCaption();

    /**
     * Translate "Could not reload the Shiny application.\n\n{0}".
     *
     * @return the translated value
     */
    @DefaultMessage("Could not reload the Shiny application.\\n\\n{0}")
    @Key("reloadFailErrorMsg")
    String reloadFailErrorMsg(String error);

    /**
     * Translate "Shiny App Launch Failed".
     *
     * @return the translated value
     */
    @DefaultMessage("Shiny App Launch Failed")
    @Key("launchFailedErrorCaption")
    String launchFailedErrorCaption();

    /**
     * Translate "Shiny App Background Launch Failed".
     *
     * @return the translated value
     */
    @DefaultMessage("Shiny App Background Launch Failed")
    @Key("backgroundLaunchFailedErrorCaption")
    String backgroundLaunchFailedErrorCaption();

    /**
     * Translate "Failed to Stop".
     *
     * @return the translated value
     */
    @DefaultMessage("Failed to Stop")
    @Key("failedToStopErrorCaption")
    String failedToStopErrorCaption();

    /**
     * Translate "Could not stop the Shiny application.\n\n{0}".
     *
     * @return the translated value
     */
    @DefaultMessage("Could not stop the Shiny application.\\n\\n{0}")
    @Key("failedToStopErrorMsg")
    String failedToStopErrorMsg(String error);

    /**
     * Translate "Shiny Application".
     *
     * @return the translated value
     */
    @DefaultMessage("Shiny Application")
    @Key("shinyApplicationTitle")
    String shinyApplicationTitle();

    /**
     * Translate "Open in Browser".
     *
     * @return the translated value
     */
    @DefaultMessage("Open in Browser")
    @Key("openInBrowserButtonText")
    String openInBrowserButtonText();

    /**
     * Translate "In R Console".
     *
     * @return the translated value
     */
    @DefaultMessage("In R Console")
    @Key("inRConsoleLabel")
    String inRConsoleLabel();

    /**
     * Translate "In Background Job".
     *
     * @return the translated value
     */
    @DefaultMessage("In Background Job")
    @Key("inBackgroundJobLabel")
    String inBackgroundJobLabel();
}
