/*
 * BaseMenuBar.java
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
package org.rstudio.core.client.command;

import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MenuItemSeparator;
import com.google.gwt.user.client.ui.UIObject;
import org.rstudio.core.client.SeparatorManager;
import org.rstudio.core.client.widget.events.GlassVisibilityEvent;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BaseMenuBar extends MenuBar
{
   private class PositionComparator implements Comparator<UIObject>
   {
      public int compare(UIObject a, UIObject b)
      {
         return getPosition(a) - getPosition(b);
      }

      private int getPosition(UIObject item)
      {
         if (item instanceof MenuItem)
            return getItemIndex((MenuItem) item);
         else if (item instanceof MenuItemSeparator)
            return getSeparatorIndex((MenuItemSeparator) item);

         assert false;
         return -1;
      }
   }

   public BaseMenuBar(boolean vertical)
   {
      super(vertical);
      vertical_ = vertical;

      // Would prefer to inject this from the constructor but some
      // subclasses are instantiated using generated code--don't feel
      // like messing with all that now
      eventBus_ = RStudioGinjector.INSTANCE.getEventBus();
   }

   @Override
   protected void onLoad()
   {
      if (vertical_ && glass++ == 0)
         eventBus_.fireEvent(new GlassVisibilityEvent(true));
      super.onLoad();
      for (MenuItem child : getItems())
         if (child instanceof AppMenuItem)
            ((AppMenuItem)child).onShow();
      if (autoHideRedundantSeparators_)
         manageSeparators();
   }

   @Override
   protected void onUnload()
   {
      super.onUnload();
      if (vertical_ && --glass == 0)
         eventBus_.fireEvent(new GlassVisibilityEvent(false));
   }

   public void setAutoHideRedundantSeparators(boolean value)
   {
      autoHideRedundantSeparators_ = value;
   }

   @Override
   public MenuItemSeparator insertSeparator(MenuItemSeparator separator,
                                            int beforeIndex) throws IndexOutOfBoundsException
   {
      MenuItemSeparator value = super.insertSeparator(separator, beforeIndex);
      separators_.add(value);
      return value;
   }

   @Override
   public void removeSeparator(MenuItemSeparator separator)
   {
      separators_.remove(separator);
      super.removeSeparator(separator);
   }

   @Override
   public MenuItem getSelectedItem()
   {
      return super.getSelectedItem();
   }

   private static class MenuSeparatorManager extends SeparatorManager<UIObject>
   {
      @Override
      protected boolean isSeparator(UIObject item)
      {
         return item instanceof MenuItemSeparator;
      }

      @Override
      protected boolean isVisible(UIObject item)
      {
         return item.isVisible();
      }

      @Override
      protected void setVisible(UIObject item, boolean visible)
      {
         item.setVisible(visible);
      }
   }

   /**
    * Make sure the proper separators appear and disappear
    */
   protected void manageSeparators()
   {
      if (separators_.size() == 0)
         return;
      List<MenuItem> menuItems = getItems();
      ArrayList<UIObject> allItems =
            new ArrayList<UIObject>(menuItems.size() + separators_.size());
      allItems.addAll(separators_);
      allItems.addAll(menuItems);
      Collections.sort(allItems, new PositionComparator());

      new MenuSeparatorManager().manageSeparators(allItems);
   }

   public int getItemCount()
   {
      return getItems().size();
   }

   public ArrayList<MenuItem> getVisibleItems()
   {
      ArrayList<MenuItem> items = new ArrayList<MenuItem>();
      for (MenuItem item : getItems())
         if (item.isVisible())
            items.add(item);
      return items;
   }

   /**
    * Reference count for glass visibility. NOTE: Perhaps this should be
    * hoisted into a more general class so that everyone who raises
    * GlassVisibilityEvent shares the same refcount.
    */
   private static int glass = 0;

   private boolean autoHideRedundantSeparators_ = true;
   private final ArrayList<MenuItemSeparator> separators_ =
         new ArrayList<MenuItemSeparator>();
   private final EventBus eventBus_;
   private final boolean vertical_;
}
