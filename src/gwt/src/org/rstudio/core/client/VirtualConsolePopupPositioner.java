/*
 * VirtualConsolePopupPositioner.java
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
package org.rstudio.core.client;

import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;

import org.rstudio.studio.client.workbench.views.console.shell.assist.PopupPositioner;

public class VirtualConsolePopupPositioner implements PositionCallback
{

    private Rectangle cursorBounds_;
    private VirtualConsolePopupDisplay popup_;
   
    public VirtualConsolePopupPositioner(Rectangle rect, VirtualConsolePopupPanel popup)
    {
        cursorBounds_ = rect;
        popup_ = popup;
    }

    @Override
    public void setPosition(int popupWidth, int popupHeight) {
        if (cursorBounds_ == null)
        {
           assert false : "Positioning popup but no cursor bounds available";
           return;
        }
        
        PopupPositioner.Coordinates coords = PopupPositioner.getPopupPosition(
            popupWidth, 
            popupHeight, 
            cursorBounds_.getLeft(), 
            cursorBounds_.getBottom(), 
            5, 
            true);

        popup_.setPopupPosition(coords.getLeft(), coords.getTop());
    }
   

}
