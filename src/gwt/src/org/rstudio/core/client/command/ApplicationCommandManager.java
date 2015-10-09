/*
 * ApplicationCommandManager.java
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

import java.util.List;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.command.EditorCommandManager.EditorKeyBindings;
import org.rstudio.core.client.command.KeyboardShortcut.KeySequence;
import org.rstudio.core.client.files.FileBacked;
import org.rstudio.core.client.widget.KeybindingsChangedEvent;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedHandler;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ApplicationCommandManager
{
   public ApplicationCommandManager()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      shortcuts_ = ShortcutManager.INSTANCE;
      
      bindings_ = new FileBacked<EditorKeyBindings>(
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
      
      events_.addHandler(
            KeybindingsChangedEvent.TYPE,
            new KeybindingsChangedEvent.Handler()
            {
               @Override
               public void onKeybindingsChanged(KeybindingsChangedEvent event)
               {
                  loadBindings();
               }
            });
   }
   
   @Inject
   private void initialize(EventBus events, Commands commands)
   {
      events_ = events;
      commands_ = commands;
   }
   
   public void addBindingsAndSave(final EditorKeyBindings newBindings)
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
                  loadBindings();
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
            shortcuts_.clearCustomBindings();
            for (String commandId : bindings.iterableKeys())
            {
               AppCommand command = commands_.getCommandById(commandId);
               if (command == null)
               {
                  // TODO: How should mis-named commands be reported?
                  continue;
               }
               
               List<KeySequence> keyList = bindings.get(commandId).getKeyBindings();
               for (KeySequence keys : keyList)
                  shortcuts_.addCustomBinding(keys, command);
            }
            
            if (afterLoad != null)
               afterLoad.execute(bindings);
         }
      });
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
   
   private final FileBacked<EditorKeyBindings> bindings_;
   private final ShortcutManager shortcuts_;
   
   public static final String KEYBINDINGS_PATH =
         "~/.R/rstudio/keybindings/rstudio_bindings.json";
   
   // Injected ----
   private EventBus events_;
   private Commands commands_;
}
