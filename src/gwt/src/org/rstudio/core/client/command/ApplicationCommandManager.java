/*
 * ApplicationCommandManager.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
import org.rstudio.core.client.Pair;
import org.rstudio.core.client.command.EditorCommandManager.EditorKeyBindings;
import org.rstudio.core.client.command.KeyMap.KeyMapType;
import org.rstudio.core.client.files.ConfigFileBacked;
import org.rstudio.core.client.events.ExecuteAppCommandEvent;
import org.rstudio.core.client.events.RStudioKeybindingsChangedEvent;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedHandler;

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

      bindings_ = new ConfigFileBacked<EditorKeyBindings>(
            server_,
            KEYBINDINGS_PATH,
            false,
            EditorKeyBindings.create());

      events_.addHandler(
            EditorLoadedEvent.TYPE,
            new EditorLoadedHandler()
            {
               @Override
               public void onEditorLoaded(EditorLoadedEvent event)
               {
                  loadBindings();
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
   private void initialize(EventBus events, FilesServerOperations server, Commands commands)
   {
      events_ = events;
      server_ = server;
      commands_ = commands;
   }

   public void addBindingsAndSave(final EditorKeyBindings newBindings,
                                  final CommandWithArg<EditorKeyBindings> onLoad)
   {
      bindings_.execute(new CommandWithArg<EditorKeyBindings>()
      {
         @Override
         public void execute(final EditorKeyBindings currentBindings)
         {
            currentBindings.insert(newBindings);
            bindings_.set(currentBindings, new Command()
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
      bindings_.execute(new CommandWithArg<EditorKeyBindings>()
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
      List<Pair<List<KeySequence>, AppCommand>> resolvedBindings;
      resolvedBindings = new ArrayList<Pair<List<KeySequence>, AppCommand>>();

      for (String id : bindings.iterableKeys())
      {
         AppCommand command = commands_.getCommandById(id);
         if (command == null)
            continue;
         List<KeySequence> keys = bindings.get(id).getKeyBindings();
         resolvedBindings.add(new Pair<List<KeySequence>, AppCommand>(keys, command));
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
      if (afterLoad != null)
         afterLoad.execute(bindings);
   }

   public void resetBindings()
   {
      resetBindings(null);
   }

   public void resetBindings(final CommandWithArg<EditorKeyBindings> afterReset)
   {
      bindings_.set(EditorKeyBindings.create(), new Command()
      {
         @Override
         public void execute()
         {
            loadBindings(afterReset);
         }
      });
   }

   private final ConfigFileBacked<EditorKeyBindings> bindings_;

   public static final String KEYBINDINGS_PATH =
         "keybindings/rstudio_bindings.json";

   // Injected ----
   private EventBus events_;
   private FilesServerOperations server_;
   private Commands commands_;
}

