/*
 * SourceNavigationHistory.java
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
package org.rstudio.studio.client.workbench.views.source.model;

import java.util.LinkedList;

import org.rstudio.core.client.Debug;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;



public class SourceNavigationHistory
{
   public interface Filter
   {
      boolean includeEntry(SourceNavigation navigation);
   }
   
   public SourceNavigationHistory(int maxItems)
   {
      maxItems_ = maxItems;
      history_ = new LinkedList<SourceNavigation>();
      currentLocation_ = -1;
   }
  
   public void add(SourceNavigation navigation)
   {      
      // rewind the history to the current location
      while ( (history_.size() - 1) > currentLocation_)
         history_.removeLast();
      
      // screen out duplicates
      if ( (history_.size() == 0) || !history_.getLast().isAtSameRowAs(navigation))
      {         
         // implement capacity restriction
         if (history_.size() == maxItems_)
            history_.removeFirst();
         
         // add the item and set the current location
         history_.add(navigation);
         currentLocation_ = history_.size() - 1;
      }
      
      fireChangeEvent();
   }
   
   
   public void clear()
   {
      history_.clear();
      currentLocation_ = -1;
      fireChangeEvent();
   }
   
   public boolean isBackEnabled()
   {
      return currentLocation_ >= 0;
   }
   
   public boolean isForwardEnabled()
   {
      return currentLocation_ < (history_.size() - 1);
   }
   
   public SourceNavigation scanBack(Filter filter)
   {
       if (!isBackEnabled())
          return null;
       
       for (int i=currentLocation_; i >= 0; i--)
       {
          if (filter.includeEntry(history_.get(i)))
             return history_.get(i);
       }
       
       return null;
   }
   
   public SourceNavigation goBack()
   {    
      if (!isBackEnabled())
         return null;
      
      SourceNavigation navigation = history_.get(currentLocation_--);
      
      // if we have only one more item in the stack and it matches
      // this one then clear the history
      if (isBackEnabled() && 
          (history_.size() == 1) &&
          navigation.isAtSameRowAs(history_.get(currentLocation_)))
      {
         clear();
      }
      
      fireChangeEvent();
      
      return navigation;
        
   }
  
   public SourceNavigation goForward()
   {
      if (!isForwardEnabled())
         return null;
      
      SourceNavigation navigation = history_.get(++currentLocation_);
      fireChangeEvent();
      return navigation;
   }
   
   public HandlerRegistration addChangeHandler(ChangeHandler handler)
   {
      return handlerManager_.addHandler(ChangeEvent.getType(), handler);
   }
   
   private void fireChangeEvent()
   {
      DomEvent.fireNativeEvent(Document.get().createChangeEvent(), 
                               handlerManager_);
     
   }
 
   @SuppressWarnings("unused")
   private void debugPrintCurrentHistory()
   {
      Debug.logToConsole("HISTORY (location=" + currentLocation_ + ")");
      for (int i=0; i<history_.size(); i++)
         Debug.logToConsole(history_.get(i).toDebugString());
      Debug.logToConsole("");
   }
  
   private final int maxItems_;
   private LinkedList<SourceNavigation> history_;
   private int currentLocation_;
   private HandlerManager handlerManager_ = new HandlerManager(this);
}
