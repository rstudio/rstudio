/*
 * SelectionCommitEvent.java
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
package org.rstudio.core.client.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class SelectionCommitEvent<I> extends GwtEvent<SelectionCommitEvent.Handler<I>>
{
   private static Type<Handler<?>> TYPE;

   public static <I> void fire(HasSelectionCommitHandlers<I> source, I selectedItem)
   {
     if (TYPE != null)
     {
       SelectionCommitEvent<I> event = new SelectionCommitEvent<I>(selectedItem);
       source.fireEvent(event);
     }
   }

   public static Type<Handler<?>> getType()
   {
     if (TYPE == null)
     {
       TYPE = new Type<>();
     }
     return TYPE;
   }

   private final I selectedItem;

   /**
    * Creates a new selection event.
    *
    * @param selectedItem selected item
    */
   protected SelectionCommitEvent(I selectedItem)
   {
     this.selectedItem = selectedItem;
   }

   // The instance knows its BeforeSelectionHandler is of type I, but the TYPE
   // field itself does not, so we have to do an unsafe cast here.
   @SuppressWarnings({ "unchecked", "rawtypes" })
   @Override
   public final Type<Handler<I>> getAssociatedType()
   {
     return (Type) TYPE;
   }

   /**
    * Gets the selected item.
    *
    * @return the selected item
    */
   public I getSelectedItem()
   {
     return selectedItem;
   }

   @Override
   protected void dispatch(Handler<I> handler)
   {
     handler.onSelectionCommit(this);
   }

   public interface Handler<I> extends EventHandler
   {
      void onSelectionCommit(SelectionCommitEvent<I> event);
   }
}
