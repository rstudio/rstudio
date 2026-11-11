/*
 * ScrollableToolbarPopupMenu.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.res.ThemeStyles;

public class ScrollableToolbarPopupMenu extends ToolbarPopupMenu
{
   @Override
   protected ToolbarMenuBar createMenuBar()
   {
      // This class may now be partially/entirely deprecated with the autoContrain
      // functionality but since ScrollableToolbarPopupMenu touches so many
      // parts of the app it is best to leave autoConstrain turned
      // off and defaulting to what exists now until it's thoroughly tested
      autoConstrain_ = false;

      final ScrollableToolbarMenuBar menuBar = new ScrollableToolbarMenuBar(true);
      menuBar.addSelectionHandler(new SelectionHandler<MenuItem>()
      {
         public void onSelection(SelectionEvent<MenuItem> event)
         {
            ensureSelectedIsVisible();
         }
      });
      return menuBar;
   }
   
   public HandlerRegistration addSelectionHandler(SelectionHandler<MenuItem> handler)
   {
      return ((ScrollableToolbarMenuBar)menuBar_).addSelectionHandler(handler);
   }


   public void ensureSelectedIsVisible()
   {
      MenuItem item = menuBar_.getSelectedItem();
      if (item == null)
         return;

      Element itemEl = item.getElement();
      DomUtils.ensureVisibleVert(scrollPanel_.getElement(), itemEl, 0);

      // Also show the headings and separators directly above the item, so that
      // moving up to a group's first item (or the menu's) brings its heading
      // back into view. They are above the item, so this only scrolls up. Not
      // for the mouse: hovering selects, and the item would move from the pointer.
      Event event = Event.getCurrentEvent();
      if (event != null && (event.getTypeInt() & (Event.MOUSEEVENTS | Event.ONCLICK)) != 0)
         return;

      Element rowEl = itemEl.getParentElement();
      Element topEl = null;
      for (Element prevEl = rowEl.getPreviousSiblingElement();
           prevEl != null && !isItemRow(prevEl);
           prevEl = prevEl.getPreviousSiblingElement())
      {
         topEl = prevEl;
      }
      if (topEl != null)
         DomUtils.ensureVisibleVert(scrollPanel_.getElement(), topEl, 0);
   }

   private boolean isItemRow(Element rowEl)
   {
      for (MenuItem item : menuBar_.getMenuItems())
      {
         if (item.getElement().getParentElement() == rowEl)
            return true;
      }
      return false;
   }
   
   public int getSelectedIndex()
   {
      return menuBar_.getSelectedIndex();
   }

   @Override
   protected Widget createMainWidget()
   {
      scrollPanel_ = new ScrollPanel(menuBar_);
      scrollPanel_.addStyleName(ThemeStyles.INSTANCE.scrollableMenuBar());
      scrollPanel_.getElement().getStyle().setOverflowY(Overflow.AUTO);
      scrollPanel_.getElement().getStyle().setOverflowX(Overflow.HIDDEN);
      setMaxHeight(getMaxHeight());
      return scrollPanel_;
   }
   
   protected int getMaxHeight()
   {
      return 300;
   }
   
   protected void setMaxHeight(int maxHeight)
   {
      scrollPanel_.getElement().getStyle().setProperty("maxHeight", 
            maxHeight + "px");
   }

   protected class ScrollableToolbarMenuBar extends ToolbarMenuBar
      implements HasSelectionHandlers<MenuItem>
   {
      public ScrollableToolbarMenuBar(boolean vertical)
      {
         super(vertical);
         setFocusOnHoverEnabled(false);
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

