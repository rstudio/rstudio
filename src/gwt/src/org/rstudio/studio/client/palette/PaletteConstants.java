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
    String commandNotAvailableCaption();
    String commandNotAvailableMessage(String commandLabel);
    String commandDisabledCaption();
    String commandDisabledMessage(String commandLabel);
    String commandExecutionFailedCaption();
    String commandExecutionFailedMessage(String commandLabel, String errMsg);
    String cmdPaletteClearedCaption();
    String cmdPaletteClearedMessage();
    String searchCmdsAriaLabelProperty();
    String matchCmdsAriaLabelProperty();
    String searchForCmdsAriaLabelProperty();
    String cmdsFoundReportStatusMsg();
    String checkboxLabelOn();
    String checkboxLabelOff();
    String settingText();
    String commandCtrl();
    String commandAlt();
    String commandShift();
    String commandCmd();
}
