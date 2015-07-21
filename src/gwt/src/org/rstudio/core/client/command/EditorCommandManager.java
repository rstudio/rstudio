/*
 * EditorCommandManager.java
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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.command.KeyboardShortcut.KeySequence;
import org.rstudio.core.client.files.FileBacked;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.AddEditorCommandEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.RequestEditorCommandsEvent;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceCommand;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceCommandManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedHandler;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class EditorCommandManager
{
   public static class EditorKeyBindings extends JsObject
   {
      public static final EditorKeyBindings create()
      {
         return JavaScriptObject.createObject().cast();
      }
      
      public final EditorKeyBinding get(String key)
      {
         return getObject(key).cast();
      }
      
      protected EditorKeyBindings() {}
   }
   
   public static class EditorKeyBinding extends JavaScriptObject
   {
      protected EditorKeyBinding() {}
      
      public final KeySequence getKeyBinding()
      {
         return KeySequence.fromShortcutString(getBindingString());
      }
      
      private final native String getBindingString()
      /*-{
         return this;
      }-*/;
   }
   
   public EditorCommandManager()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      bindings_ = new FileBacked<EditorKeyBindings>(KEYBINDINGS_PATH);
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
   }
   
   @Inject
   private void initialize(EventBus events,
                           FilesServerOperations files)
   {
      events_ = events;
      files_ = files;
   }
   
   public static final native JsArray<AceCommand> getDefaultAceCommands() /*-{
      return $wnd.require("ace/commands/default_commands").commands;
   }-*/;
   
   private AceCommandManager getAceCommandManager()
   {
      RequestEditorCommandsEvent event = new RequestEditorCommandsEvent();
      events_.fireEvent(event);
      return event.getAceCommandManager();
   }
   
   public boolean hasBinding(KeySequence keys)
   {
      AceCommandManager manager = getAceCommandManager();
      return manager.hasBinding(keys);
   }
   
   public boolean hasPrefix(KeySequence keys)
   {
      AceCommandManager manager = getAceCommandManager();
      return manager.hasPrefix(keys);
   }
   
   public void rebindCommand(String id, KeySequence keySequence)
   {
      events_.fireEvent(new AddEditorCommandEvent(id, keySequence, true));
   }
   
   public void updateBindings(final EditorKeyBindings newBindings)
   {
      bindings_.execute(new CommandWithArg<EditorKeyBindings>()
      {
         @Override
         public void execute(final EditorKeyBindings currentBindings)
         {
            currentBindings.insert(newBindings);
            bindings_.set(currentBindings);
         }
      });
   }
   
   public void saveBindings(EditorKeyBindings bindings)
   {
      bindings_.set(bindings);
   }
   
   public void loadBindings()
   {
      bindings_.execute(new CommandWithArg<EditorKeyBindings>()
      {
         @Override
         public void execute(EditorKeyBindings bindings)
         {
            for (String commandName : bindings.iterableKeys())
            {
               EditorKeyBinding binding = bindings.get(commandName);
               rebindCommand(
                     commandName,
                     binding.getKeyBinding());
            }
         }
      });
   }
   
   private final FileBacked<EditorKeyBindings> bindings_;
   private boolean isBindingsLoaded_ = false;
   public static final String KEYBINDINGS_PATH = "~/.R/keybindings/editor_bindings.json";
   
   // Injected ----
   private EventBus events_;
   private FilesServerOperations files_;
}
