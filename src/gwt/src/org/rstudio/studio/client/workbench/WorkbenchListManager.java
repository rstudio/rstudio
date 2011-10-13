package org.rstudio.studio.client.workbench;

import java.util.ArrayList;
import java.util.HashMap;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.events.ListChangedEvent;
import org.rstudio.studio.client.workbench.events.ListChangedHandler;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.WorkbenchLists;

import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;


public class WorkbenchListManager implements SessionInitHandler,
                                             ListChangedHandler
{
   public static final String FILE_MRU = "file_mru";
   public static final String PROJECT_MRU = "project_mru";
   public static final String HELP_HISTORY = "help_history";
   
   @Inject
   public WorkbenchListManager(EventBus events,
                               Session session)
   {
      session_ = session;
      
      listContexts_.put(FILE_MRU, new ListContext(FILE_MRU));
      listContexts_.put(PROJECT_MRU, new ListContext(PROJECT_MRU));
      listContexts_.put(HELP_HISTORY, new ListContext(HELP_HISTORY));
      
      events.addHandler(SessionInitEvent.TYPE, this);
      events.addHandler(ListChangedEvent.TYPE, this);
   }
   
   public HandlerRegistration addListChangedHandler(String name,
                                                    ListChangedHandler handler)
   {
      return listContexts_.get(name).addListChangedHandler(handler);
   }
   
   
   @Override
   public void onSessionInit(SessionInitEvent event)
   {
      WorkbenchLists lists = session_.getSessionInfo().getLists();
      updateList(FILE_MRU, lists);
      updateList(PROJECT_MRU, lists);
      updateList(HELP_HISTORY, lists);
   }
   
   @Override
   public void onListChanged(ListChangedEvent event)
   {
      updateList(event.getName(), event.getList());
   }

   
   private void updateList(String name, WorkbenchLists lists)
   {
      updateList(name, lists.getList(name));
   }
    
   private void updateList(String name, ArrayList<String> list)
   {
      listContexts_.get(name).setList(list);
   }
    
   private class ListContext
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
      
      public HandlerRegistration addListChangedHandler(
                                                ListChangedHandler handler)
      {
         HandlerRegistration hreg =  handlers_.addHandler(ListChangedEvent.TYPE, 
                                                          handler);
    
         if (list_ != null)
            handler.onListChanged(new ListChangedEvent(name_, list_));
         
         return hreg;
      }
      
      private final String name_;
      private ArrayList<String> list_ = null;
      private final HandlerManager handlers_ = new HandlerManager(this);
   }

   private HashMap<String,ListContext> listContexts_ = 
                                       new HashMap<String,ListContext>();

   private final Session session_;
}
