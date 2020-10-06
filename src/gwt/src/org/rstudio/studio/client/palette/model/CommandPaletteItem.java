/*
 * CommandPaletteEntrySource.java
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
package org.rstudio.studio.client.palette.model;

import org.rstudio.studio.client.palette.events.PaletteItemInvokedEvent;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.IsWidget;

/**
 * Represents a single command palette item. Implements IsWidget so that
 * rendering the HTML for the command palette item can be deferred until it's
 * displayed; classes implementing this interface should create the widget on
 * demand in IsWidget rather than up front in their constructors.
 */
public interface CommandPaletteItem extends IsWidget, HasHandlers
{
   /**
    * The methods for invoking an item (these can have different semantics)
    */
   public enum InvocationSource
   {
      Keyboard,
      Mouse
   };

   /**
    * Indicate whether or not the item has been rendered
    * 
    * @return True if the item has been rendered (i.e. has an active widget)
    */
   public boolean isRendered();

   /**
    * Invoke the entry (execute the command, etc.)
    */
   public void invoke(InvocationSource source);
   
   /**
    * Add a handler to be called when the palette item is invoked
    */
   HandlerRegistration addInvokeHandler(PaletteItemInvokedEvent.Handler handler);

   /**
    * Does this item match the given search keywords?
    * 
    * @param keywords The keywords to match on
    * 
    * @return True if the palette item matches; false otherwise
    */
   boolean matchesSearch(String[] keywords);

   /**
    * Turns on search highlighting for the item.
    * 
    * @param keywords The search keywords to highlight.
    */
   public void setSearchHighlight(String[] keywords);

   /**
    * Dismiss after invoke?
    * 
    * @return Whether to dismiss the palette after invoking the entry.
    */
   public boolean dismissOnInvoke();
   
   /**
    * Draw the entry as selected (or not)
    * 
    * @param selected Whether to draw the entry as selected
    */
   public void setSelected(boolean selected);
}
