/*
 * ApplicationCommandManager.java
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
package org.rstudio.core.client.command;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.Pair;
import org.rstudio.core.client.command.EditorCommandManager.EditorKeyBindings;
import org.rstudio.core.client.command.KeyMap.KeyMapType;
import org.rstudio.core.client.command.impl.DesktopMenuCallback;
import org.rstudio.core.client.files.ConfigFileBacked;
import org.rstudio.core.client.events.ExecuteAppCommandEvent;
import org.rstudio.core.client.events.RStudioKeybindingsChangedEvent;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.rstudioapi.model.RStudioAPIServerOperations;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedEvent;

import java.util.ArrayList;
import java.util.List;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ApplicationCommandManager
{
   public ApplicationCommandManager()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);

      customBindingsConfig_ = new ConfigFileBacked<>(
            server_,
            KEYBINDINGS_PATH,
            false,
            EditorKeyBindings.create());

      events_.addHandler(
            EditorLoadedEvent.TYPE,
            new EditorLoadedEvent.Handler()
            {
               @Override
               public void onEditorLoaded(EditorLoadedEvent event)
               {
                  loadBindings();
               }
            });

      // Listen for changes to the list of commands that have callbacks. We maintain a list of these on the client
      // side in order to avoid having to emit an RPC every time a command is executed.
      events_.addHandler(
         CommandCallbacksChangedEvent.TYPE,
         new CommandCallbacksChangedEvent.Handler()
         {
            @Override
            public void onCommandCallbacksChanged(CommandCallbacksChangedEvent event)
            {
               commandsWithCallbacks_ = JsArrayUtil.fromJsArrayString(event.getCommands());
            }
         });

      // Listen for commands to be executed. If a command that has a callback is executed, notify the server
      // so that the callback can be invoked.
      events_.addHandler(
         CommandEvent.TYPE,
         new CommandHandler()
         {
            @Override
            public void onCommand(AppCommand command)
            {
               if (commandsWithCallbacks_ == null)
               {
                  // Expected if no commands are registered
                  return;
               }

               if (commandsWithCallbacks_.contains(command.getId()) || commandsWithCallbacks_.contains("*"))
               {
                  apiServer_.recordCommandExecution(command.getId(), new VoidServerRequestCallback());
               }
            }
         });

      // This event should only be received by satellites.
      events_.addHandler(
            RStudioKeybindingsChangedEvent.TYPE,
            new RStudioKeybindingsChangedEvent.Handler()
            {
               @Override
               public void onRStudioKeybindingsChanged(RStudioKeybindingsChangedEvent event)
               {
                  loadBindings(event.getBindings(), null);
               }
            });

      events_.addHandler(ExecuteAppCommandEvent.TYPE, evt ->
      {
         AppCommand command = commands_.getCommandById(evt.getData().command());
         if (command == null && !evt.getData().quiet())
         {
            RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage("Invalid Command",
                  "The command '" + evt.getData().command() + "' does not exist.");
            return;
         }
         if (Satellite.isCurrentWindowSatellite() &&
             command.getWindowMode() != AppCommand.WINDOW_MODE_ANY)
         {
            // If this command doesn't want to run in satellites, attempting to
            // execute will forward it to the main window (which already knows
            // to execute it), causing the execution to occur multiple times.
            return;
         }

         command.execute();
         return;
      });

   }

   @Inject
   private void initialize(EventBus events,
                           FilesServerOperations server,
                           RStudioAPIServerOperations apiServer,
                           Commands commands)
   {
      events_ = events;
      server_ = server;
      apiServer_ = apiServer;
      commands_ = commands;
   }

   public void addBindingsAndSave(final EditorKeyBindings newBindings,
                                  final CommandWithArg<EditorKeyBindings> onLoad)
   {
      customBindingsConfig_.execute(new CommandWithArg<EditorKeyBindings>()
      {
         @Override
         public void execute(final EditorKeyBindings currentBindings)
         {
            currentBindings.insert(newBindings);
            customBindingsConfig_.set(currentBindings, new Command()
            {
               @Override
               public void execute()
               {
                  loadBindings(onLoad);
               }
            });
         }
      });
   }

   public void loadBindings()
   {
      loadBindings(null);
   }

   public void loadBindings(final CommandWithArg<EditorKeyBindings> afterLoad)
   {
      customBindingsConfig_.execute(new CommandWithArg<EditorKeyBindings>()
      {
         @Override
         public void execute(EditorKeyBindings bindings)
         {
            loadBindings(bindings, afterLoad);
         }
      });
   }

   private void loadBindings(EditorKeyBindings bindings,
                             final CommandWithArg<EditorKeyBindings> afterLoad)
   {
      customBindings_ = bindings;
      List<Pair<List<KeySequence>, AppCommand>> resolvedBindings;
      resolvedBindings = new ArrayList<>();

      for (String id : bindings.iterableKeys())
      {
         AppCommand command = commands_.getCommandById(id);
         if (command == null)
            continue;
         List<KeySequence> keys = bindings.get(id).getKeyBindings();
         resolvedBindings.add(new Pair<>(keys, command));

         if (keys.size() > 0) 
            command.setCustomShortcut(new KeyboardShortcut(keys.get(keys.size() - 1)));
      }

      KeyMap map = ShortcutManager.INSTANCE.getKeyMap(KeyMapType.APPLICATION);
      for (int i = 0; i < resolvedBindings.size(); i++)
      {
         // TODO: We should make it possible for users to define a command
         // binding that is disabled for certain modes.
         map.setBindings(
               resolvedBindings.get(i).first,
               new AppCommandBinding(resolvedBindings.get(i).second, "", true));
      }

      // TODO: Set the bindings in the AppCommand keymap, removing any
      // previously registered bindings.

      if (Desktop.hasDesktopFrame())
         DesktopMenuCallback.commitCommandShortcuts();

      if (afterLoad != null)
         afterLoad.execute(bindings);
   }

   public void resetBindings()
   {
      resetBindings(null);
   }

   public void resetBindings(final CommandWithArg<EditorKeyBindings> afterReset)
   {

      // clear the customShortcuts on the last-loaded set of customized commands
      if (customBindings_ != null) {
         for (String id : customBindings_.iterableKeys())
         {
            AppCommand command = commands_.getCommandById(id);
            if (command == null)
               continue;

            command.setCustomShortcut(null);
         }
      }

      customBindingsConfig_.set(EditorKeyBindings.create(), new Command()
      {
         @Override
         public void execute()
         {
            loadBindings(afterReset);
         }
      });
   }

   private final ConfigFileBacked<EditorKeyBindings> customBindingsConfig_;
   private EditorKeyBindings customBindings_;
   private List<String> commandsWithCallbacks_;

   public static final String KEYBINDINGS_PATH =
         "keybindings/rstudio_bindings.json";

   // Injected ----
   private EventBus events_;
   private FilesServerOperations server_;
   private RStudioAPIServerOperations apiServer_;
   private Commands commands_;
}

