package org.rstudio.studio.client.workbench;

import java.util.ArrayList;
import java.util.HashMap;

import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.events.ListChangedEvent;
import org.rstudio.studio.client.workbench.events.ListChangedHandler;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;


// TODO: more name checking for safety?

// TODO: generally less JsObject at the application level

// TODO: consider enforceUnique being the default and convert to set?

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
      JsObject jsLists = session_.getSessionInfo().getLists();
      updateList(FILE_MRU, extractList(FILE_MRU, jsLists));
      updateList(PROJECT_MRU, extractList(PROJECT_MRU, jsLists));
      updateList(HELP_HISTORY, extractList(HELP_HISTORY, jsLists));
   }
   
   @Override
   public void onListChanged(ListChangedEvent event)
   {
      updateList(event.getName(), event.getList());
   }

   
   private void updateList(String name, ArrayList<String> list)
   {
      listContexts_.get(name).setList(list);
   }
   
   private ArrayList<String> extractList(String name, JsObject lists)
   {
      ArrayList<String> list = new ArrayList<String>();
      JsArrayString jsList = lists.<JsArrayString>getObject(name);
      for (int i=0; i<jsList.length(); i++)
         list.add(jsList.get(i));
      return list;
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
