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
import java.util.List;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Pair;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.KeyMap;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.command.EditorCommandManager.EditorKeyBindings;
import org.rstudio.core.client.command.KeyboardShortcut.KeySequence;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.command.KeyMap.KeyMapType;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.addins.Addins.RAddin;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;
import org.rstudio.studio.client.workbench.addins.AddinsCommandManager;
import org.rstudio.studio.client.workbench.addins.events.AddinRegistryUpdatedEvent;
import org.rstudio.studio.client.workbench.commands.Commands;
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
      
      events.addHandler(SessionInitEvent.TYPE, this);
      events.addHandler(AddinRegistryUpdatedEvent.TYPE, this);
   }

   @Override
   public void onSessionInit(SessionInitEvent sie)
   {
      RAddins addins = session_.getSessionInfo().getAddins();
      update(addins);
   }

   @Override
   public void onAddinRegistryUpdated(AddinRegistryUpdatedEvent event)
   {
      update(event.getData());
   }
   
   private void update(final RAddins addins)
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
   
   private void finishUpdate(RAddins addins)
   {
      // Collect the addins as key-value pairs (so we can sort)
      List<Pair<String, RAddin>> addinsList =
            new ArrayList<Pair<String, RAddin>>();
      
      for (String key : JsUtil.asIterable(addins.keys()))
         addinsList.add(new Pair<String, RAddin>(key, addins.get(key)));
      
      Collections.sort(addinsList, new Comparator<Pair<String, RAddin>>()
      {
         @Override
         public int compare(Pair<String, RAddin> o1, Pair<String, RAddin> o2)
         {
            return o1.first.compareTo(o2.first);
         }
      });
      
      KeyMap addinsKeyMap =
            ShortcutManager.INSTANCE.getKeyMap(KeyMapType.ADDIN);
      
      // Populate up to 15 commands.
      manageCommand(commands_.addinsMru0(),  addinsList, addinsKeyMap, 0);
      manageCommand(commands_.addinsMru1(),  addinsList, addinsKeyMap, 1);
      manageCommand(commands_.addinsMru2(),  addinsList, addinsKeyMap, 2);
      manageCommand(commands_.addinsMru3(),  addinsList, addinsKeyMap, 3);
      manageCommand(commands_.addinsMru4(),  addinsList, addinsKeyMap, 4);
      manageCommand(commands_.addinsMru5(),  addinsList, addinsKeyMap, 5);
      manageCommand(commands_.addinsMru6(),  addinsList, addinsKeyMap, 6);
      manageCommand(commands_.addinsMru7(),  addinsList, addinsKeyMap, 7);
      manageCommand(commands_.addinsMru8(),  addinsList, addinsKeyMap, 8);
      manageCommand(commands_.addinsMru9(),  addinsList, addinsKeyMap, 9);
      manageCommand(commands_.addinsMru10(), addinsList, addinsKeyMap,10);
      manageCommand(commands_.addinsMru11(), addinsList, addinsKeyMap,11);
      manageCommand(commands_.addinsMru12(), addinsList, addinsKeyMap,12);
      manageCommand(commands_.addinsMru13(), addinsList, addinsKeyMap,13);
      manageCommand(commands_.addinsMru14(), addinsList, addinsKeyMap,14);
   }
   
   private void manageCommand(AppCommand command,
                              List<Pair<String, RAddin>> addinsList,
                              KeyMap keyMap,
                              int index)
   {
      if (index >= addinsList.size())
      {
         command.setEnabled(false);
         command.setVisible(false);
         return;
      }
      
      String id    = addinsList.get(index).first;
      RAddin addin = addinsList.get(index).second;
      
      command.setEnabled(true);
      command.setVisible(true);
      command.setDesc(addin.getDescription());
      command.setLabel(addin.getName());
      
      List<KeySequence> keys = keyMap.getBindings(id);
      if (keys != null && !keys.isEmpty())
         command.setShortcut(new KeyboardShortcut(keys.get(0)));
   }
   
   // Private Members ----
   private WorkbenchList mruList_;
   
   // Injected ----
   private final Provider<WorkbenchListManager> pListManager_;
   private final Provider<AddinsCommandManager> pAddinManager_;
   private final Session  session_;
   private final EventBus events_;
   private final Commands commands_;

}
