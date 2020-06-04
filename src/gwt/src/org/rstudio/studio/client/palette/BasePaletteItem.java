/*
 * BasePaletteItem.java
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
package org.rstudio.studio.client.palette;

import org.rstudio.studio.client.palette.events.PaletteItemInvokedEvent;
import org.rstudio.studio.client.palette.events.PaletteItemInvokedEvent.Handler;
import org.rstudio.studio.client.palette.model.CommandPaletteItem;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

public abstract class BasePaletteItem<T extends Widget> implements CommandPaletteItem
{
   public BasePaletteItem()
   {
      handlers_ = new HandlerManager(this);
   }
   
   @Override
   public boolean isRendered()
   {
      return widget_ != null;
   }

   @Override
   public Widget asWidget()
   {
      if (widget_ == null)
      {
         // Create widget in derived class
         widget_ = createWidget();
         
         // Attach click-to-invoke handlers
         if (widget_ != null)
         {
            widget_.sinkEvents(Event.ONCLICK);
            widget_.addHandler((evt) ->
            {
               fireEvent(new PaletteItemInvokedEvent(this));
            }, 
            ClickEvent.getType());
         }
      }
      return widget_;
   }

   @Override
   public void fireEvent(GwtEvent<?> evt)
   {
      handlers_.fireEvent(evt);
   }

   @Override
   public HandlerRegistration addInvokeHandler(Handler handler)
   {
      return handlers_.addHandler(PaletteItemInvokedEvent.TYPE, handler);
   }

   protected boolean labelMatchesSearch(String label, String[] keywords)
   {
      String hay = label.toLowerCase();
      for (String needle: keywords)
      {
         if (!hay.contains(needle))
         {
            return false;
         }
      }
      return true;
   }

   public abstract T createWidget();
   
   protected T widget_;
   
   private HandlerManager handlers_;
}
