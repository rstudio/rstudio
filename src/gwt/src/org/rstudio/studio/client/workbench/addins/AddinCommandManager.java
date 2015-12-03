package org.rstudio.studio.client.workbench.addins;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.command.EditorCommandManager.EditorKeyBindings;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.files.FileBacked;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedHandler;

import java.util.HashMap;
import java.util.Map;

public class AddinCommandManager
{
   public AddinCommandManager()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      bindings_ = new FileBacked<EditorKeyBindings>(
            KEYBINDINGS_PATH,
            false,
            EditorKeyBindings.create());
      
      commandMap_ = new HashMap<KeyboardShortcut, Command>();
      
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
   private void initialize(EventBus events)
   {
      events_ = events;
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
            finishLoading(bindings, afterLoad);
         }
      });
   }
   
   private void finishLoading(EditorKeyBindings bindings,
                              final CommandWithArg<EditorKeyBindings> afterLoad)
   {
      // TODO
   }
   
   private final Map<KeyboardShortcut, Command> commandMap_;
   private final FileBacked<EditorKeyBindings> bindings_;
   private static final String KEYBINDINGS_PATH = "~/.R/rstudio/keybindings/addins.json";
   
   
   // Injected ----
   private EventBus events_;

}
