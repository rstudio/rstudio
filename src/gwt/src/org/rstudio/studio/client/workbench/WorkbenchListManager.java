/*
 * WorkbenchListManager.java
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
package org.rstudio.studio.client.workbench;

import java.util.ArrayList;
import java.util.HashMap;

import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.events.ListChangedEvent;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.WorkbenchLists;
import org.rstudio.studio.client.workbench.model.WorkbenchListsServerOperations;

import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;


public class WorkbenchListManager
{

   @Inject
   public WorkbenchListManager(EventBus events,
                               Session session,
                               WorkbenchListsServerOperations server)
   {
      session_ = session;
      server_ = server;

      listContexts_.put(FILE_MRU, new ListContext(FILE_MRU));
      listContexts_.put(PROJECT_MRU, new ListContext(PROJECT_MRU));
      listContexts_.put(PLOT_PUBLISH_MRU, new ListContext(PLOT_PUBLISH_MRU));
      listContexts_.put(HELP_HISTORY, new ListContext(HELP_HISTORY));
      listContexts_.put(USER_DICTIONARY, new ListContext(USER_DICTIONARY));

      events.addHandler(SessionInitEvent.TYPE, (SessionInitEvent sie) ->
      {
         WorkbenchLists lists = session_.getSessionInfo().getLists();
         updateList(FILE_MRU, lists);
         updateList(PROJECT_MRU, lists);
         updateList(PLOT_PUBLISH_MRU, lists);
         updateList(HELP_HISTORY, lists);
         updateList(USER_DICTIONARY, lists);
      });

      events.addHandler(ListChangedEvent.TYPE, listChangedEvent ->
      {
         updateList(listChangedEvent.getName(), listChangedEvent.getList());
      });
   }

   public WorkbenchList getFileMruList()
   {
      return listContexts_.get(FILE_MRU);
   }

   public WorkbenchList getProjectMruList()
   {
      return listContexts_.get(PROJECT_MRU);
   }

   public WorkbenchList getHelpHistoryList()
   {
      return listContexts_.get(HELP_HISTORY);
   }

   public WorkbenchList getUserDictionaryList()
   {
      return listContexts_.get(USER_DICTIONARY);
   }

   public WorkbenchList getPlotPublishMruList()
   {
      return listContexts_.get(PLOT_PUBLISH_MRU);
   }

   private void updateList(String name, WorkbenchLists lists)
   {
      updateList(name, lists.getList(name));
   }

   private void updateList(String name, ArrayList<String> list)
   {
      ListContext context = listContexts_.get(name);
      if (context == null)
      {
         Debug.logWarning("Unknown workbench list: " + name);
         return;
      }
      context.setList(list);
   }

   private class ListContext implements WorkbenchList
   {
      public ListContext(String name)
      {
         name_ = name;
      }

      public void setList(ArrayList<String> list)
      {
         list_ = list;
         handlers_.fireEvent(new ListChangedEvent(name_, list_));
      }

      @Override
      public void setContents(ArrayList<String> list)
      {
         server_.listSetContents(name_, list, new ListRequestCallback());
      }

      @Override
      public void append(String item)
      {
         server_.listAppendItem(name_, item, new ListRequestCallback());
      }

      @Override
      public void prepend(String item)
      {
         server_.listPrependItem(name_, item, new ListRequestCallback());
      }

      @Override
      public void remove(String item)
      {
         server_.listRemoveItem(name_, item, new ListRequestCallback());
      }

      @Override
      public void clear()
      {
         server_.listClear(name_, new ListRequestCallback());
      }

      @Override
      public HandlerRegistration addListChangedHandler(ListChangedEvent.Handler handler)
      {
         HandlerRegistration hreg =  handlers_.addHandler(ListChangedEvent.TYPE,
                                                          handler);

         if (list_ != null)
            handler.onListChanged(new ListChangedEvent(name_, list_));

         return hreg;
      }

      // for now we have a no-op stub for server request callbacks
      private class ListRequestCallback extends VoidServerRequestCallback
      {
      }

      private final String name_;
      private ArrayList<String> list_ = null;
      private final HandlerManager handlers_ = new HandlerManager(this);

   }

   private final HashMap<String,ListContext> listContexts_ = new HashMap<>();

   private final Session session_;
   private final WorkbenchListsServerOperations server_;

   private static final String FILE_MRU = "file_mru";
   private static final String PROJECT_MRU = "project_mru";
   private static final String PLOT_PUBLISH_MRU = "plot_publish_mru";
   private static final String HELP_HISTORY = "help_history_links";
   private static final String USER_DICTIONARY = "user_dictionary";
}
