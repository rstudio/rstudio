/*
 * Clipboard.java
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
package org.rstudio.core.client;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.inject.Inject;

// NOTE: On the browser, the clipboard can only be accessed when called
// within an event handler (e.g. a keydown event). To ensure this happens,
// make sure the ClipboardAccessor is only used within e.g. @Handler for
// commands.
public class Clipboard
{
   // Only public so that the Ginjector can see it.
   public static class ClipboardResources
   {
      public ClipboardResources()
      {
         RStudioGinjector.INSTANCE.injectMembers(this);
      }
      
      @Inject
      private void initialize(Commands commands)
      {
         commands_ = commands;
      }
      
      public Commands getCommands() { return commands_; }
      
      private Commands commands_;
   }
   
   public static final void copy()
   {
      if (isCommandEnabled("copy"))
         execCommand("copy");
      else
         commands_.copyDummy().execute();
   }
   
   public static final void cut()
   {
      if (isCommandEnabled("cut"))
         execCommand("cut");
      else
         desktopCut();
   }
   
   public static final void paste()
   {
      if (isCommandEnabled("paste"))
         execCommand("paste");
      else
         desktopPaste();
   }
   
   private static void desktopCopy()
   {
      commands_.copyDummy().execute();
   }
   
   private static void desktopCut()
   {
      commands_.cutDummy().execute();
   }
   
   private static void desktopPaste()
   {
      commands_.pasteDummy().execute();
   }
   
   private static final native boolean isCommandEnabled(String command) /*-{
      return $doc.queryCommandEnabled(command);
   }-*/;
   
   private static final native boolean execCommand(String command) /*-{
      return $doc.execCommand(command);
   }-*/;
   
   private static ClipboardResources RES = new ClipboardResources();
   private static Commands commands_ = RES.getCommands();
   
}
