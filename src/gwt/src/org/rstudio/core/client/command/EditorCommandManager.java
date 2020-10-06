/*
 * EditorCommandManager.java
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.ConfigFileBacked;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.events.EditorKeybindingsChangedEvent;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.ResetEditorCommandsEvent;
import org.rstudio.studio.client.application.events.SetEditorCommandBindingsEvent;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceCommand;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceCommandManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedHandler;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.Command;
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

      public final void setBindings(String key, List<KeySequence> ksList)
      {
         List<String> bindings = new ArrayList<String>();
         for (KeySequence ks : ksList)
            bindings.add(ks.toString());

         setString(key, StringUtil.join(bindings, "|"));
      }

      protected EditorKeyBindings() {}
   }

   public static class EditorKeyBinding extends JavaScriptObject
   {
      protected EditorKeyBinding() {}

      public final List<KeySequence> getKeyBindings()
      {
         JsArrayString bindings = getBindingsInternal();
         Set<KeySequence> keys = new HashSet<KeySequence>();
         for (String binding : JsUtil.asIterable(bindings))
         {
            String[] splat = binding.split("\\|");
            for (String item : splat)
            {
               keys.add(KeySequence.fromShortcutString(item));
            }
         }

         List<KeySequence> keyList = new ArrayList<KeySequence>();
         keyList.addAll(keys);
         return keyList;
      }

      private final native JsArrayString getBindingsInternal()
      /*-{
         var result = this;
         if (typeof result === "string")
            result = [result];
         return result;
      }-*/;
   }

   public EditorCommandManager()
   {
      AceEditor.load(() -> finishInit());
   }

   private void finishInit()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);

      manager_ = AceCommandManager.create();

      bindings_ = new ConfigFileBacked<EditorKeyBindings>(
            files_,
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
            EditorKeybindingsChangedEvent.TYPE,
            new EditorKeybindingsChangedEvent.Handler()
            {
               @Override
               public void onEditorKeybindingsChanged(EditorKeybindingsChangedEvent event)
               {
                  loadBindings(event.getBindings(), null);
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

   public boolean hasBinding(KeySequence keys)
   {
      return manager_.hasBinding(keys);
   }

   public boolean hasPrefix(KeySequence keys)
   {
      return manager_.hasPrefix(keys);
   }

   public void rebindCommand(String id, List<KeySequence> keys)
   {
      manager_.rebindCommand(id, keys);
      events_.fireEvent(new SetEditorCommandBindingsEvent(id, keys));
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

   private void loadBindings(final EditorKeyBindings bindings,
                             final CommandWithArg<EditorKeyBindings> afterLoad)
   {
      for (String commandName : bindings.iterableKeys())
      {
         EditorKeyBinding binding = bindings.get(commandName);

         rebindCommand(
               commandName,
               binding.getKeyBindings());

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
            manager_ = AceCommandManager.create();
            events_.fireEvent(new ResetEditorCommandsEvent());

            if (afterReset != null)
               afterReset.execute();
         }
      });
   }

   public JsArray<AceCommand> getCommands()
   {
      return manager_.getRelevantCommands();
   }

   private ConfigFileBacked<EditorKeyBindings> bindings_;
   private AceCommandManager manager_;

   private boolean isBindingsLoaded_ = false;
   public static final String KEYBINDINGS_PATH =
         "keybindings/editor_bindings.json";

   // Injected ----
   private EventBus events_;
   private FilesServerOperations files_;
}
