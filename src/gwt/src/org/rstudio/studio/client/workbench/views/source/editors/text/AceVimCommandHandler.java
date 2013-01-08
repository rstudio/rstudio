/*
 * AceVimCommandHandler.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.command.KeyboardShortcut;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.NativeEvent;

public class AceVimCommandHandler implements AceKeyboardPreviewer.Handler
{
   public AceVimCommandHandler(CommandWithArg<Boolean> onFindCommand)
   {
      onFindCommand_ = onFindCommand;
   }
   

   @Override
   public boolean previewKeyDown(JavaScriptObject data, NativeEvent event)
   {
      if (event.getKeyCode() == 191) /* '/' or '?' */
      {
         if (isNormalMode(data))
         {
            int modifier = KeyboardShortcut.getModifierValue(event);
            if (modifier == KeyboardShortcut.NONE)
            {
               onFindCommand_.execute(true);
               return true;
            }
            else if (modifier == KeyboardShortcut.SHIFT)
            {
               onFindCommand_.execute(false);
               return true;
            }
         }
      }
      
      return false;
   }

   @Override
   public boolean previewKeyPress(JavaScriptObject data, char charCode)
   {
      if (charCode == '/' && isNormalMode(data))
      {
         onFindCommand_.execute(true);
         return true;
      }
      else if (charCode == '?' && isNormalMode(data))
      {
         onFindCommand_.execute(false);
         return true;
      }
      else
      {
         return false;
      }
   }
  
   
   private boolean isNormalMode(JavaScriptObject data)
   {
      VimHandlerData vimData = data.cast();
      return "start".equals(vimData.getState()); 
   }
  

   private static final class VimHandlerData extends JavaScriptObject
   {
      protected VimHandlerData()
      {
      }
      
      public native final String getState() /*-{
         return this.state;
      }-*/;

   }
   
   private  CommandWithArg<Boolean> onFindCommand_;
   
}
