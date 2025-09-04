/*
 * UserCommandManager.java
 *
 * Copyright (C) 2015 by Posit Software, PBC
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
package org.rstudio.core.client.command;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.remote.ExecuteUserCommandEvent;
import org.rstudio.studio.client.server.remote.RegisterUserCommandEvent;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UserCommandManager
{
   public static class UserCommand
   {
      public UserCommand(String name, Command command)
      {
         name_ = name;
         command_ = command;
      }

      public String getName() { return name_; }
      public void execute() { command_.execute(); }

      private final String name_;
      private final Command command_;
   }

   public UserCommandManager()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      commandMap_ = new HashMap<>();

      events_.addHandler(
            RegisterUserCommandEvent.TYPE,
            new RegisterUserCommandEvent.Handler()
            {
               @Override
               public void onRegisterUserCommand(RegisterUserCommandEvent event)
               {
                  UserCommandManager.this.onRegisterUserCommand(event);
               }
            });
   }

   @Inject
   public void initialize(EventBus events)
   {
      events_ = events;
   }

   public boolean dispatch(KeyboardShortcut shortcut)
   {
      if (commandMap_.containsKey(shortcut))
      {
         UserCommand command = commandMap_.get(shortcut);
         command.execute();
         return true;
      }

      return false;
   }

   private void onRegisterUserCommand(RegisterUserCommandEvent event)
   {
      final String name = event.getData().getName();
      JsArrayString shortcutStrings = event.getData().getShortcuts();

      for (int i = 0; i < shortcutStrings.length(); i++)
      {
         String shortcutString = shortcutStrings.get(i);
         KeySequence sequence = KeySequence.fromShortcutString(shortcutString);
         assert sequence != null : "Failed to parse string '" + shortcutString + "'";

         KeyboardShortcut shortcut = new KeyboardShortcut(sequence);
         UserCommand command = new UserCommand(name, new Command()
         {
            @Override
            public void execute()
            {
               events_.fireEvent(new ExecuteUserCommandEvent(name));
            }
         });

         commandMap_.put(shortcut, command);
      }
   }

   public Set<KeyboardShortcut> getKeyboardShortcuts()
   {
      return commandMap_.keySet();
   }

   public Map<KeyboardShortcut, UserCommand> getCommands()
   {
      return commandMap_;
   }

   private final Map<KeyboardShortcut, UserCommand> commandMap_;

   // Injected ----
   private EventBus events_;
}
