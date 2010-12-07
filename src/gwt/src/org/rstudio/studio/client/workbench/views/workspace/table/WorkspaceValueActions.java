/*
 * WorkspaceValueActions.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.workspace.table;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;

public class WorkspaceValueActions implements HasSelectionHandlers<Integer>
{
   private static class ButtonEx extends Button
   {
      private ButtonEx(Element element)
      {
         super(element);
      }

      public static ButtonEx wrap(ButtonElement element)
      {
         assert Document.get().getBody().isOrHasChild(element);

         ButtonEx button = new ButtonEx(element);
         button.onAttach();
         return button;
      }

      @Override
      public void onDetach()
      {
         super.onDetach();
      }
   }
   private class ButtonDetails
   {
      ButtonEx button;
      HandlerRegistration registration;

      public ButtonDetails(ButtonElement element, final int value)
      {
         this.button = ButtonEx.wrap(element);
         this.registration = button.addClickHandler(new ClickHandler()
         {
            public void onClick(ClickEvent event)
            {
               event.preventDefault();
               event.stopPropagation();
               SelectionEvent.fire(WorkspaceValueActions.this, value);
            }
         });
      }

      public ButtonDetails detach()
      {
         registration.removeHandler();
         button.onDetach();
         return null;
      }
   }

   interface MyUiBinder extends UiBinder<DivElement, WorkspaceValueActions>
   {}
   static MyUiBinder binder = GWT.create(MyUiBinder.class);

   public void attachToCell(TableCellElement cell)
   {
      detach();
      
      if (actionsEl_ == null)
      {
         actionsEl_ = binder.createAndBindUi(this);
      }

      cell.insertFirst(actionsEl_);

      rmBtn_ = new ButtonDetails(rmEl_, 1);
   }

   public void detach()
   {
      if (rmBtn_ != null)
         rmBtn_ = rmBtn_.detach();
      if (actionsEl_ != null && actionsEl_.getParentElement() != null)
      {
         actionsEl_.getParentElement().removeChild(actionsEl_);
         actionsEl_ = null;
      }
   }

   public TableCellElement getCurrentParentCell()
   {
      if (actionsEl_ == null)
         return null;
      TableCellElement cell = (TableCellElement) actionsEl_.getParentElement();
      return cell;
   }

   public HandlerRegistration addSelectionHandler(SelectionHandler<Integer> handler)
   {
      return handlers_.addHandler(SelectionEvent.getType(), handler);
   }

   public void fireEvent(GwtEvent<?> event)
   {
      handlers_.fireEvent(event);
   }

   private final HandlerManager handlers_ = new HandlerManager(null);
   private DivElement actionsEl_;
   @UiField
   ButtonElement rmEl_;
   private ButtonDetails rmBtn_;
   public static final int FIX = 0;
   public static final int RM = 1;
}
