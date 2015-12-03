package org.rstudio.studio.client.workbench.addins;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.command.EditorCommandManager.EditorKeyBindings;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.command.KeyboardShortcut.KeySequence;
import org.rstudio.core.client.files.FileBacked;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class AddinsCommandManager
{
   public AddinsCommandManager()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      bindings_ = new FileBacked<EditorKeyBindings>(
            KEYBINDINGS_PATH,
            false,
            EditorKeyBindings.create());
      
      commandMap_ = new HashMap<KeySequence, Command>();
      
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
            AddinsKeyBindingsChangedEvent.TYPE,
            new AddinsKeyBindingsChangedEvent.Handler()
            {
               @Override
               public void onAddinsKeyBindingsChanged(AddinsKeyBindingsChangedEvent event)
               {
                  registerBindings(event.getBindings(), null);
               }
            });
   }
   
   @Inject
   private void initialize(EventBus events,
                           AddinsServerOperations server)
   {
      events_ = events;
      server_ = server;
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
      commandMap_.clear();
      for (String commandId : bindings.iterableKeys())
      {
         List<KeySequence> keyList = bindings.get(commandId).getKeyBindings();
         for (KeySequence keys : keyList)
            registerBinding(commandId, keys);
      }
      
      if (afterLoad != null)
         afterLoad.execute(bindings);
   }
   
   private void registerBinding(final String commandId, final KeySequence keys)
   {
      commandMap_.put(keys, new Command()
      {
         @Override
         public void execute()
         {
            server_.executeRAddin(commandId, new VoidServerRequestCallback());
         }
      });
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
   
   public boolean dispatch(KeyboardShortcut shortcut)
   {
      KeySequence keys = shortcut.getKeySequence();
      if (commandMap_.containsKey(keys))
      {
         Command command = commandMap_.get(keys);
         command.execute();
         return true;
      }
      return false;
   }
   
   private final Map<KeySequence, Command> commandMap_;
   private final FileBacked<EditorKeyBindings> bindings_;
   private static final String KEYBINDINGS_PATH = "~/.R/rstudio/keybindings/addins.json";
   
   
   // Injected ----
   private EventBus events_;
   private AddinsServerOperations server_;

}
