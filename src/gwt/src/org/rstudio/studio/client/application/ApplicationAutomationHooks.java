/*
 * ApplicationHooks.java
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
public class ApplicationAutomationHooks
{
   static interface ExecuteCommandCallback
   {
      public void execute(String commandId);
   }
   
   static interface ListCommandsCallback
   {
      public JsArrayString execute();
   }
   
   @Inject
   public ApplicationAutomationHooks(Commands commands)
   {
      commands_ = commands;
   }
   
   public final void initialize()
   {
      initializeCallbacks();
      
      exportExecuteCommandCallback("executeCommand", new ExecuteCommandCallback()
      {
         @Override
         public void execute(String commandId)
         {
            AppCommand command = commands_.getCommandById(commandId);
            command.execute();
         }
      });
      
      exportListCommandsCallback("listCommands", new ListCommandsCallback()
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
      $wnd.rstudioCallbacks = $wnd.rstudio.callbacks || {};
   }-*/;

   private native final void exportExecuteCommandCallback(String name, ExecuteCommandCallback callback)
   /*-{
      $wnd.rstudioCallbacks[name] = $entry(function(value) {
         return callback.@org.rstudio.studio.client.application.ApplicationHooks.ExecuteCommandCallback::execute(*)(value);
      });
   }-*/;
   
   private native final void exportListCommandsCallback(String name, ListCommandsCallback callback)
   /*-{
      $wnd.rstudioCallbacks[name] = $entry(function(value) {
         return callback.@org.rstudio.studio.client.application.ApplicationHooks.ListCommandsCallback::execute()();
      });
   }-*/;
   
   private final Commands commands_;
}
