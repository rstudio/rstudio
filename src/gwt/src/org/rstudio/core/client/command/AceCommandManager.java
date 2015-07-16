/*
 * AceCommandManager.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.core.client.command;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AceCommandManager
{
   public static class AceCommandAlias
   {
      public AceCommandAlias(String name, String binding, boolean replace)
      {
         name_ = name;
         binding_ = binding;
         replace_ = replace;
      }
      
      public String getName() { return name_; }
      public String getBinding() { return binding_; }
      public boolean getReplace() { return replace_; }
      
      private final String name_;
      private final String binding_;
      private final boolean replace_;
   }
   
   public static class AceCommand extends JavaScriptObject
   {
      protected AceCommand() {}
      
      public final native String getName()
      /*-{
         return this.name;
      }-*/;
      
      public final JsArrayString getWindowsKeyBinding()
      {
         return getKeyBinding(true);
      }
      
      public final JsArrayString getMacKeyBinding()
      {
         return getKeyBinding(false);
      }
      
      public final JsArrayString getBindingsForCurrentPlatform()
      {
         return getKeyBinding(BrowseCap.isWindows());
      }
      
      private final native JsArrayString getKeyBinding(boolean isWindows)
      /*-{
         
         var binding = this.bindKey;
         
         if (binding == null) {
            return [];
         }
         
         if (typeof binding === "string") {
            return binding.split("|");
         }
         
         var shortcut = isWindows ? binding.win : binding.mac;
         if (typeof shortcut === "string") {
            return shortcut.split("|");
         } else {
            return [];
         }
         
         
      }-*/;
      
      public final native boolean isReadOnly()
      /*-{
         return this.readOnly;
      }-*/;
   }
   
   public AceCommandManager()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   private void initialize(EventBus events)
   {
      events_ = events;
   }
   
   public static final native JsArray<AceCommand> getAceCommands() /*-{
      return $wnd.require("ace/commands/default_commands").commands;
   }-*/;
   
   // Injected ----
   private EventBus events_;
}
