/*
 * ApplicationAutomation.java
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
package org.rstudio.studio.client.application;

import java.util.Map;
import java.util.Set;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.core.client.JsArrayString;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ApplicationAutomation
{
   static interface NullaryCallback<R>
   {
      public R execute();
   }
   
   static interface UnaryCallback<R, T>
   {
      public R execute(T value);
   }
   
   @Inject
   public ApplicationAutomation(Commands commands)
   {
      commands_ = commands;
   }
   
   public final boolean isAutomationHost()
   {
      return isAutomationHost_;
   }
   public final boolean isAutomationAgent()
   {
      return isAutomationAgent_;
   }
   
   public final void initializeHost()
   {
      isAutomationHost_ = true;
   }
   
   public final void initializeAgent()
   {
      isAutomationAgent_ = true;
      
      initializeCallbacks();
      
      exportCallback("commandExecute", new UnaryCallback<Void, String>()
      {
         @Override
         public Void execute(String commandId)
         {
            AppCommand command = commands_.getCommandById(commandId);
            command.execute();
            return null;
         }
      });
      
      exportCallback("commandList", new NullaryCallback<JsArrayString>()
      {
         @Override
         public JsArrayString execute()
         {
            Map<String, AppCommand> allCommands = commands_.getCommands();
            Set<String> commandIds = allCommands.keySet();
            return JsUtil.toJsArrayString(commandIds);
         }
      });
   }
   
   private native final void initializeCallbacks()
   /*-{
      $wnd.rstudioCallbacks = $wnd.rstudioCallbacks || {};
   }-*/;
   
   private native final <R> void exportCallback(String name, NullaryCallback<R> callback)
   /*-{
      $wnd.rstudioCallbacks[name] = $entry(function() {
         return callback.@org.rstudio.studio.client.application.ApplicationAutomation.NullaryCallback::execute(*)();
      });
   }-*/;
   
   private native final <R, T> void exportCallback(String name, UnaryCallback<R, T> callback)
   /*-{
      $wnd.rstudioCallbacks[name] = $entry(function(value) {
         return callback.@org.rstudio.studio.client.application.ApplicationAutomation.UnaryCallback::execute(*)(value);
      });
   }-*/;
   
   private final Commands commands_;
   private boolean isAutomationHost_ = false;
   private boolean isAutomationAgent_ = false;
}
