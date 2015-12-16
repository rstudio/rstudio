/*
 * AddinsMRUList.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandHandler;
import org.rstudio.core.client.command.KeyMap;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.command.EditorCommandManager.EditorKeyBindings;
import org.rstudio.core.client.command.KeyboardShortcut.KeySequence;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.command.KeyMap.KeyMapType;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.addins.Addins.AddinExecutor;
import org.rstudio.studio.client.workbench.addins.Addins.RAddin;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;
import org.rstudio.studio.client.workbench.addins.AddinsCommandManager;
import org.rstudio.studio.client.workbench.addins.events.AddinRegistryUpdatedEvent;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.ListChangedEvent;
import org.rstudio.studio.client.workbench.events.ListChangedHandler;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class AddinsMRUList implements SessionInitHandler,
                                      AddinRegistryUpdatedEvent.Handler
{
   @Inject
   public AddinsMRUList(Provider<WorkbenchListManager> pListManager,
                        Provider<AddinsCommandManager> pAddinManager,
                        Session session,
                        EventBus events,
                        Commands commands)
   {
      pListManager_ = pListManager;
      pAddinManager_ = pAddinManager;
      session_ = session;
      events_ = events;
      commands_ = commands;
      
      mruCommands_ = new AppCommand[] {
            commands_.addinsMru0(),
            commands_.addinsMru1(),
            commands_.addinsMru2(),
            commands_.addinsMru3(),
            commands_.addinsMru4(),
            commands_.addinsMru5(),
            commands_.addinsMru6(),
            commands_.addinsMru7(),
            commands_.addinsMru8(),
            commands_.addinsMru9(),
            commands_.addinsMru10(),
            commands_.addinsMru11(),
            commands_.addinsMru12(),
            commands_.addinsMru13(),
            commands_.addinsMru14()
      };
      
      events_.addHandler(SessionInitEvent.TYPE, this);
      events_.addHandler(AddinRegistryUpdatedEvent.TYPE, this);
      
      initCommandHandlers();
   }
   
   private void initCommandHandlers()
   {
      for (int i = 0; i < mruCommands_.length; i++)
         addIndexedHandler(mruCommands_[i], i);
   }
   
   private void addIndexedHandler(AppCommand command, int index)
   {
      command.addHandler(new AddinCommandHandler(index));
   }

   @Override
   public void onSessionInit(SessionInitEvent sie)
   {
      mruList_ = pListManager_.get().getAddinsMruList();
      mruList_.addListChangedHandler(new ListChangedHandler()
      {
         @Override
         public void onListChanged(ListChangedEvent event)
         {
            mruEntries_ = event.getList();
            update(addinRegistry_);
         }
      });
      RAddins addins = session_.getSessionInfo().getAddins();
      update(addins);
   }

   @Override
   public void onAddinRegistryUpdated(AddinRegistryUpdatedEvent event)
   {
      update(event.getData());
   }
   
   private void update(RAddins addins)
   {
      List<RAddin> addinsList = new ArrayList<RAddin>();
      for (String key : JsUtil.asIterable(addins.keys()))
         addinsList.add(addins.get(key));
      update(addinsList);
   }
   
   private void update(final List<RAddin> addins)
   {
      pAddinManager_.get().loadBindings(new CommandWithArg<EditorKeyBindings>()
      {
         @Override
         public void execute(EditorKeyBindings bindings)
         {
            finishUpdate(addins);
         }
      });
   }
   
   private void finishUpdate(List<RAddin> addinRegistry)
   {
      // The list that will eventually hold the backing set
      // of addins that the dummy MRU commands will dispatch to
      List<RAddin> addinList = new ArrayList<RAddin>();
      
      // Map used for quick lookup of MRU addin ids
      Map<String, RAddin> addinMap = new HashMap<String, RAddin>();
      for (RAddin addin : addinRegistry)
         addinMap.put(addin.getId(), addin);
      
      // Collect addins. First, collect addins in the MRU list.
      for (String id : mruEntries_)
      {
         if (addinList.size() >= MRU_LIST_SIZE)
            break;
         
         if (addinMap.containsKey(id))
            addinList.add(addinMap.get(id));
      }
      
      // Now, collect the rest of the addins (that haven't been added
      // to the backing list.
      for (RAddin addin : addinRegistry)
      {
         if (addinList.size() >= MRU_LIST_SIZE)
            break;
         
         if (!addinList.contains(addin))
            addinList.add(addin);
      }
      
      // Sort the addins list, favoring addins that have
      // been recently updated.
      Collections.sort(addinList, new Comparator<RAddin>()
      {
         @Override
         public int compare(RAddin o1, RAddin o2)
         {
            int compare = 0;
            
            // Compare first on package name.
            compare = o1.getPackage().compareTo(o2.getPackage());
            if (compare != 0)
               return compare;
            
            // Then compare on actual name.
            compare = o1.getName().compareTo(o2.getName());
            if (compare != 0)
               return compare;
            
            return 0;
         }
      });
      
      // Save the list (so that the dummy commands can be routed properly)
      addinList_ = addinList;
      addinRegistry_ = addinRegistry;
      
      KeyMap addinsKeyMap =
            ShortcutManager.INSTANCE.getKeyMap(KeyMapType.ADDIN);
      
      // Populate up to 15 commands.
      for (int i = 0; i < mruCommands_.length; i++)
         manageCommand(mruCommands_[i], addinList_, addinsKeyMap, i);
   }
   
   private class AddinCommandHandler implements CommandHandler
   {
      public AddinCommandHandler(int index)
      {
         index_ = index;
      }
      
      @Override
      public void onCommand(AppCommand command)
      {
         if (executor_ == null)
            executor_ = new AddinExecutor();
         
         RAddin addin = addinList_.get(index_);
         executor_.execute(addin);
      }
      
      private final int index_;
      private AddinExecutor executor_;
   }
   
   private void manageCommand(AppCommand command,
                              List<RAddin> addinsList,
                              KeyMap keyMap,
                              int index)
   {
      if (index >= addinsList.size())
      {
         command.setEnabled(false);
         command.setVisible(false);
         return;
      }
      
      RAddin addin = addinsList.get(index);
      
      command.setEnabled(true);
      command.setVisible(true);
      
      String description = addin.getDescription() + " [" + addin.getId() + "]";
      command.setDesc(description);
      
      String name = addin.getName();
      command.setLabel(name);
      
      List<KeySequence> keys = keyMap.getBindings(addin.getId());
      if (keys != null && !keys.isEmpty())
         command.setShortcut(new KeyboardShortcut(keys.get(0)));
   }
   
   public void add(RAddin addin)
   {
      mruList_.prepend(addin.getId());
   }
   
   public AppCommand[] getAddinMruCommands()
   {
      return mruCommands_;
   }
   
   // Private Members ----
   private WorkbenchList mruList_;
   private ArrayList<String> mruEntries_;
   
   // NOTE: The addinRegistry_ is distinct from the addinList_;
   // the addinRegistry_ contains ALL commands, while the addinList_
   // is just the top n sorted commands (used as a backing for the
   // MRU commands)
   private List<RAddin> addinRegistry_;
   private List<RAddin> addinList_;
   
   private final AppCommand[] mruCommands_;
   
   private static final int MRU_LIST_SIZE = 15;
   
   // Injected ----
   private final Provider<WorkbenchListManager> pListManager_;
   private final Provider<AddinsCommandManager> pAddinManager_;
   private final Session  session_;
   private final EventBus events_;
   private final Commands commands_;

}
