package org.rstudio.studio.client.workbench;

import org.rstudio.studio.client.workbench.events.ListChangedHandler;

import com.google.gwt.event.shared.HandlerRegistration;

/*
 * Interface to workbench lists. The contract is that the mutating functions
 * call the server and then an updated copy of the list is (eventually) 
 * returned via the ListChangedHandler
 */
public interface WorkbenchList
{
   // mutating operations
   void append(String item);
   void prepend(String item);
   void remove(String item);
   void clear();
   
   // change handler
   HandlerRegistration addListChangedHandler(ListChangedHandler handler);
}
