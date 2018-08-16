/*
 * CompletionPopupPresenter.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

import org.rstudio.core.client.command.KeyboardHelper;
import org.rstudio.core.client.command.KeyboardShortcut;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;

public class CompletionPopupPresenter
{
   public CompletionPopupPresenter(CompletionPopupDisplay popup)
   {
      popup_ = popup;
   }
   
   public boolean handleKeyDown(NativeEvent event)
   {
      if (!popup_.isShowing())
         return false;
      
      int keyCode = event.getKeyCode();
      int modifier = KeyboardShortcut.getModifierValue(event);
      if (KeyboardHelper.isModifierKey(keyCode))
         return false;
      
      // allow emacs-style navigation of popup entries
      if (modifier == KeyboardShortcut.CTRL)
      {
         switch (keyCode)
         {
         case KeyCodes.KEY_P: popup_.selectPrev(); return true;
         case KeyCodes.KEY_N: popup_.selectNext(); return true;
         }
      }
      
      else if (modifier == KeyboardShortcut.NONE)
      {
         switch (keyCode)
         {
         case KeyCodes.KEY_UP:       popup_.selectPrev();     return true;
         case KeyCodes.KEY_DOWN:     popup_.selectNext();     return true;
         case KeyCodes.KEY_PAGEUP:   popup_.selectPrevPage(); return true;
         case KeyCodes.KEY_PAGEDOWN: popup_.selectNextPage(); return true;
         case KeyCodes.KEY_HOME:     popup_.selectFirst();    return true;
         case KeyCodes.KEY_END:      popup_.selectLast();     return true;
         }
      }
      
      return false;
   }
   
   private final CompletionPopupDisplay popup_;
}
