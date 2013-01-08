/*
 * ListChangedEvent.java
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
package org.rstudio.studio.client.workbench.events;

import java.util.ArrayList;

import org.rstudio.core.client.js.JsObject;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.shared.GwtEvent;

public class ListChangedEvent extends GwtEvent<ListChangedHandler>
{
   public static final GwtEvent.Type<ListChangedHandler> TYPE =
      new GwtEvent.Type<ListChangedHandler>();
   
   public ListChangedEvent(String name, ArrayList<String> list)
   {
      name_ = name;
      list_ = list;
   }
   
   public ListChangedEvent(JsObject eventData)
   {
      name_ = eventData.getString("name");  
      
      JsArrayString list = eventData.getObject("list");
      list_ = new ArrayList<String>();
      for (int i=0; i<list.length(); i++)
         list_.add(list.get(i));
   }
   
   public String getName()
   {
      return name_;
   }
   
   public ArrayList<String> getList()
   {
      return list_;
   }
   
   @Override
   protected void dispatch(ListChangedHandler handler)
   {
      handler.onListChanged(this);
   }

   @Override
   public GwtEvent.Type<ListChangedHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final String name_;
   private final ArrayList<String> list_;
}
