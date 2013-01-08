/*
 * PopupPositioner.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import org.rstudio.core.client.Rectangle;

public class PopupPositioner implements PositionCallback
{
   private Rectangle cursorBounds_ ;
   private CompletionPopupDisplay popup_ ;
   
   public PopupPositioner(Rectangle cursorBounds, CompletionPopupDisplay popup)
   {
      this.cursorBounds_ = cursorBounds ;
      popup_ = popup ;
   }

   public void setPosition(int popupWidth, int popupHeight)
   {
      if (cursorBounds_ == null)
      {
         assert false : "Positioning popup but no cursor bounds available" ;
         return;
      }
      
      int windowBottom = Window.getScrollTop() + Window.getClientHeight() ;
      int cursorBottom = cursorBounds_.getBottom() ;
      
      if (windowBottom - cursorBottom >= popupHeight)
         popup_.setPopupPosition(cursorBounds_.getLeft(), cursorBottom) ;
      else
         popup_.setPopupPosition(cursorBounds_.getLeft(), 
                                 cursorBounds_.getTop() - popupHeight) ;
   }
}