package org.rstudio.studio.client.workbench.addins;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Pair;
import org.rstudio.core.client.command.AddinCommandBinding;
import org.rstudio.core.client.command.EditorCommandManager.EditorKeyBindings;
import org.rstudio.core.client.command.KeyMap;
import org.rstudio.core.client.command.KeyMap.CommandBinding;
import org.rstudio.core.client.command.KeyMap.KeyMapType;
import org.rstudio.core.client.command.KeyboardShortcut.KeySequence;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.files.FileBacked;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedHandler;

import java.util.ArrayList;
import java.util.List;

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
   private void initialize(EventBus events)
   {
      events_ = events;
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
      List<Pair<List<KeySequence>, CommandBinding>> commands =
            new ArrayList<Pair<List<KeySequence>, CommandBinding>>();
      
      for (String id : bindings.iterableKeys())
      {
         List<KeySequence> keyList = bindings.get(id).getKeyBindings();
         CommandBinding binding = new AddinCommandBinding(id);
         commands.add(new Pair<List<KeySequence>, CommandBinding>(keyList, binding));
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
   
   private final FileBacked<EditorKeyBindings> bindings_;
   private static final String KEYBINDINGS_PATH = "~/.R/rstudio/keybindings/addins.json";
   
   
   // Injected ----
   private EventBus events_;

}
