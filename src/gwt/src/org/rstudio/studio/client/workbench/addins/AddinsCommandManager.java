/*
 * AddinsCommandManager.java
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
package org.rstudio.studio.client.workbench.addins;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Pair;
import org.rstudio.core.client.command.AddinCommandBinding;
import org.rstudio.core.client.command.EditorCommandManager.EditorKeyBindings;
import org.rstudio.core.client.command.KeyMap;
import org.rstudio.core.client.command.KeyMap.CommandBinding;
import org.rstudio.core.client.command.KeyMap.KeyMapType;
import org.rstudio.core.client.command.KeySequence;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.files.ConfigFileBacked;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.MainWindowObject;
import org.rstudio.studio.client.workbench.addins.Addins.RAddin;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;
import org.rstudio.studio.client.workbench.addins.events.AddinRegistryUpdatedEvent;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AddinsCommandManager
{
   @Inject
   public AddinsCommandManager(EventBus events,
                               FilesServerOperations server,
                               final Session session)
   {
      events_ = events;

      bindings_ = new ConfigFileBacked<>(
            server,
            KEYBINDINGS_PATH,
            false,
            EditorKeyBindings.create());

      // load addin bindings on session init
      events_.addHandler(SessionInitEvent.TYPE, (SessionInitEvent sie) ->
      {
         MainWindowObject.rAddins().set(session.getSessionInfo().getAddins());
         loadBindings();
      });

      // set bindings when updated (e.g. through ModifyKeyboardShortcuts widget)
      events_.addHandler(
            AddinsKeyBindingsChangedEvent.TYPE,
            new AddinsKeyBindingsChangedEvent.Handler()
            {
               @Override
               public void onAddinsKeyBindingsChanged(AddinsKeyBindingsChangedEvent event)
               {
                  registerBindings(event.getBindings(), null);
               }
            });

      // update addin bindings when registry populated
      events_.addHandler(
            AddinRegistryUpdatedEvent.TYPE,
            new AddinRegistryUpdatedEvent.Handler()
            {
               @Override
               public void onAddinRegistryUpdated(AddinRegistryUpdatedEvent event)
               {
                  // all windows will receive this event, so just let the main window
                  // cache the addins in its own window
                  if (SourceWindowManager.isMainSourceWindow())
                     MainWindowObject.rAddins().set(event.getData());
                  loadBindings();
               }
            });
   }

   public void addBindingsAndSave(final EditorKeyBindings newBindings,
                                  final CommandWithArg<EditorKeyBindings> onLoad)
   {
      bindings_.execute(new CommandWithArg<EditorKeyBindings>()
      {
         @Override
         public void execute(EditorKeyBindings currentBindings)
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
            registerBindings(bindings, afterLoad);
         }
      });
   }

   private void registerBindings(final EditorKeyBindings bindings,
                                 final CommandWithArg<EditorKeyBindings> afterLoad)
   {
      List<Pair<List<KeySequence>, CommandBinding>> commands = new ArrayList<>();

      RAddins rAddins = MainWindowObject.rAddins().get();
      for (String id : bindings.iterableKeys())
      {
         List<KeySequence> keyList = bindings.get(id).getKeyBindings();
         RAddin addin = rAddins.get(id);
         if (addin == null)
            continue;
         CommandBinding binding = new AddinCommandBinding(addin);
         commands.add(new Pair<>(keyList, binding));
      }

      KeyMap map = ShortcutManager.INSTANCE.getKeyMap(KeyMapType.ADDIN);
      for (int i = 0; i < commands.size(); i++)
      {
         map.setBindings(
               commands.get(i).first,
               commands.get(i).second);
      }

      if (afterLoad != null)
         afterLoad.execute(bindings);
   }

   public void resetBindings()
   {
      resetBindings(null);
   }

   public void resetBindings(final Command afterReset)
   {
      bindings_.set(EditorKeyBindings.create(), new Command()
      {
         @Override
         public void execute()
         {
            if (afterReset != null)
               afterReset.execute();
         }
      });
   }

   public RAddins getRAddins()
   {
      return MainWindowObject.rAddins().get();
   }

   private final ConfigFileBacked<EditorKeyBindings> bindings_;
   private static final String KEYBINDINGS_PATH = "keybindings/addins.json";


   // Injected ----
   private EventBus events_;
}
