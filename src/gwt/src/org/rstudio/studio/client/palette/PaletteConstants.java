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
    @DefaultMessage("Command Not Available")
    @Key("commandNotAvailableCaption")
    String commandNotAvailableCaption();

    /**
     * Translate "The command ''{0}'' is not currently available.".
     *
     * @return the translated value
     */
    @DefaultMessage("The command ''{0}'' is not currently available.")
    @Key("commandNotAvailableMessage")
    String commandNotAvailableMessage(String commandLabel);

    /**
     * Translate "Command Disabled".
     *
     * @return the translated value
     */
    @DefaultMessage("Command Disabled")
    @Key("commandDisabledCaption")
    String commandDisabledCaption();

    /**
     * Translate "The command ''{0}'' cannot be used right now. It may be unavailable in this project, file, or view.".
     *
     * @return the translated value
     */
    @DefaultMessage("The command ''{0}'' cannot be used right now. It may be unavailable in this project, file, or view.")
    @Key("commandDisabledMessage")
    String commandDisabledMessage(String commandLabel);

    /**
     * Translate "Command Execution Failed".
     *
     * @return the translated value
     */
    @DefaultMessage("Command Execution Failed")
    @Key("commandExecutionFailedCaption")
    String commandExecutionFailedCaption();

    /**
     * Translate "The command ''{0}'' could not be executed.\n\n".
     *
     * @return the translated value
     */
    @DefaultMessage("The command ''{0}'' could not be executed.\\n\\n {1}")
    @Key("commandExecutionFailedMessage")
    String commandExecutionFailedMessage(String commandLabel, String errMsg);

    /**
     * Translate "Command Palette Cleared".
     *
     * @return the translated value
     */
    @DefaultMessage("Command Palette Cleared")
    @Key("cmdPaletteClearedCaption")
    String cmdPaletteClearedCaption();

    /**
     * Translate "The Command Palette's list of recently used items has been cleared.".
     *
     * @return the translated value
     */
    @DefaultMessage("The Command Palette''s list of recently used items has been cleared.")
    @Key("cmdPaletteClearedMessage")
    String cmdPaletteClearedMessage();

    /**
     * Translate "Search commands and settings".
     *
     * @return the translated value
     */
    @DefaultMessage("Search commands and settings")
    @Key("searchCmdsAriaLabelProperty")
    String searchCmdsAriaLabelProperty();

    /**
     * Translate "Matching commands and settings".
     *
     * @return the translated value
     */
    @DefaultMessage("Matching commands and settings")
    @Key("matchCmdsAriaLabelProperty")
    String matchCmdsAriaLabelProperty();

    /**
     * Translate "Search for commands and settings".
     *
     * @return the translated value
     */
    @DefaultMessage("Search for commands and settings")
    @Key("searchForCmdsAriaLabelProperty")
    String searchForCmdsAriaLabelProperty();

    /**
     * Translate "commands found, press up and down to navigate".
     *
     * @return the translated value
     */
    @DefaultMessage("commands found, press up and down to navigate")
    @Key("cmdsFoundReportStatusMsg")
    String cmdsFoundReportStatusMsg();

    /**
     * Translate "On".
     *
     * @return the translated value
     */
    @DefaultMessage("On")
    @Key("checkboxLabelOn")
    String checkboxLabelOn();

    /**
     * Translate "Off".
     *
     * @return the translated value
     */
    @DefaultMessage("Off")
    @Key("checkboxLabelOff")
    String checkboxLabelOff();

    /**
     * Translate "Setting".
     *
     * @return the translated value
     */
    @DefaultMessage("Setting")
    @Key("settingText")
    String settingText();

    /**
     * Translate "Ctrl".
     *
     * @return the translated value
     */
    @DefaultMessage("Ctrl")
    @Key("commandCtrl")
    String commandCtrl();

    /**
     * Translate "Alt".
     *
     * @return the translated value
     */
    @DefaultMessage("Alt")
    @Key("commandAlt")
    String commandAlt();

    /**
     * Translate "Shift".
     *
     * @return the translated value
     */
    @DefaultMessage("Shift")
    @Key("commandShift")
    String commandShift();

    /**
     * Translate "Cmd".
     *
     * @return the translated value
     */
    @DefaultMessage("Cmd")
    @Key("commandCmd")
    String commandCmd();
}
