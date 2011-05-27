/*
 * StatusBarPopupMenu.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.status;

import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.ToolbarPopupMenu;

public class StatusBarPopupMenu extends ToolbarPopupMenu
{
   public StatusBarPopupMenu()
   {
      addStyleName(ThemeStyles.INSTANCE.statusBarMenu());
   }

   public void showRelativeToUpward(final UIObject target)
   {
      setPopupPositionAndShow(new PositionCallback()
      {
         public void setPosition(int offsetWidth, int offsetHeight)
         {
            setPopupPosition(target.getAbsoluteLeft(),
                             target.getAbsoluteTop() - offsetHeight);
         }
      });
   }

   @Override
   protected ToolbarMenuBar createMenuBar()
   {
      final StatusBarMenuBar menuBar = new StatusBarMenuBar(true);
      menuBar.addSelectionHandler(new SelectionHandler<MenuItem>()
      {
         public void onSelection(SelectionEvent<MenuItem> event)
         {
            ensureSelectedIsVisible();
         }
      });
      return menuBar;
   }

   public void ensureSelectedIsVisible()
   {
      if (menuBar_.getSelectedItem() != null)
      {
         DomUtils.ensureVisibleVert(scrollPanel_.getElement(),
                                    menuBar_.getSelectedItem().getElement(),
                                    0);
      }
   }

   @Override
   protected Widget wrapMenuBar(ToolbarMenuBar menuBar)
   {
      scrollPanel_ = new ScrollPanel(menuBar);
      scrollPanel_.getElement().getStyle().setOverflowY(Overflow.AUTO);
      scrollPanel_.getElement().getStyle().setOverflowX(Overflow.HIDDEN);
      scrollPanel_.getElement().getStyle().setProperty("maxHeight", "300px");
      return scrollPanel_;
   }

   protected class StatusBarMenuBar extends ToolbarMenuBar
      implements HasSelectionHandlers<MenuItem>
   {
      public StatusBarMenuBar(boolean vertical)
      {
         super(vertical);
      }

      public HandlerRegistration addSelectionHandler(
            SelectionHandler<MenuItem> handler)
      {
         return addHandler(handler, SelectionEvent.getType());
      }

      @Override
      public void selectItem(MenuItem item)
      {
         super.selectItem(item);
         SelectionEvent.fire(this, item);
      }
   }

   private ScrollPanel scrollPanel_;
}
