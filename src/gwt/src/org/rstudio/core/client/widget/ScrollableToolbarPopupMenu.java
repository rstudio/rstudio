/*
 * ScrollableToolbarPopupMenu.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
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
      if (menuBar_.getSelectedItem() != null)
      {
         DomUtils.ensureVisibleVert(scrollPanel_.getElement(),
                                    menuBar_.getSelectedItem().getElement(),
                                    0);
      }
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

