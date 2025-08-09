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

package org.rstudio.studio.client.palette;

import com.google.gwt.i18n.client.Messages;

public interface PaletteConstants extends Messages {

    /**
     * Translate "Command Not Available".
     *
     * @return the translated value
     */
    String commandNotAvailableCaption();

    /**
     * Translate "The command ''{0}'' is not currently available.".
     *
     * @return the translated value
     */
    String commandNotAvailableMessage(String commandLabel);

    /**
     * Translate "Command Disabled".
     *
     * @return the translated value
     */
    String commandDisabledCaption();

    /**
     * Translate "The command ''{0}'' cannot be used right now. It may be unavailable in this project, file, or view.".
     *
     * @return the translated value
     */
    String commandDisabledMessage(String commandLabel);

    /**
     * Translate "Command Execution Failed".
     *
     * @return the translated value
     */
    String commandExecutionFailedCaption();

    /**
     * Translate "The command ''{0}'' could not be executed.\n\n".
     *
     * @return the translated value
     */
    String commandExecutionFailedMessage(String commandLabel, String errMsg);

    /**
     * Translate "Command Palette Cleared".
     *
     * @return the translated value
     */
    String cmdPaletteClearedCaption();

    /**
     * Translate "The Command Palette's list of recently used items has been cleared.".
     *
     * @return the translated value
     */
    String cmdPaletteClearedMessage();

    /**
     * Translate "Search commands and settings".
     *
     * @return the translated value
     */
    String searchCmdsAriaLabelProperty();

    /**
     * Translate "Matching commands and settings".
     *
     * @return the translated value
     */
    String matchCmdsAriaLabelProperty();

    /**
     * Translate "Search for commands and settings".
     *
     * @return the translated value
     */
    String searchForCmdsAriaLabelProperty();

    /**
     * Translate "commands found, press up and down to navigate".
     *
     * @return the translated value
     */
    String cmdsFoundReportStatusMsg();

    /**
     * Translate "On".
     *
     * @return the translated value
     */
    String checkboxLabelOn();

    /**
     * Translate "Off".
     *
     * @return the translated value
     */
    String checkboxLabelOff();

    /**
     * Translate "Setting".
     *
     * @return the translated value
     */
    String settingText();

    /**
     * Translate "Ctrl".
     *
     * @return the translated value
     */
    String commandCtrl();

    /**
     * Translate "Alt".
     *
     * @return the translated value
     */
    String commandAlt();

    /**
     * Translate "Shift".
     *
     * @return the translated value
     */
    String commandShift();

    /**
     * Translate "Cmd".
     *
     * @return the translated value
     */
    String commandCmd();
}
