/*
 * BaseMenuBar.java
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
package org.rstudio.core.client.command;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MenuItemSeparator;
import com.google.gwt.user.client.ui.UIObject;

import elemental2.dom.DomGlobal;

import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.SeparatorManager;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.events.GlassVisibilityEvent;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BaseMenuBar extends MenuBar
                         implements CommandHandler
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

   /**
    * Saves and restores current focus so keyboard use of Server main menu doesn't
    * leave keyboard focus indeterminate after using them.
    */
   private class FocusTracker implements CommandHandler
   {
      public FocusTracker()
      {
         commandHandler_.add(eventBus_.addHandler(CommandEvent.TYPE, this));
      }

      @Override
      public void onCommand(AppCommand command)
      {
         // Restore focus when a command is fired so any resulting modal dialog can pick up
         // the correct focused element when it does its own focus push/pop.
         restore();
      }

      /**
       * Store currently focused element and start monitoring focus changes.
       */
      public void save()
      {
         // Don't recapture currently active element if we already have one, e.g. user
         // hits another main menu shortcut while menu still open from previous one
         if (tracking())
            return;

         originallyActiveElement_ = DomUtils.getActiveElement();
         if (originallyActiveElement_ == null)
            return;

         // Monitor focus changes to know if user has, for example, clicked the
         // mouse outside the menus. Using method reference instead of lambda so
         // listener can be removed later.
         DomGlobal.document.addEventListener("focusin", this::focusInCallback);
         originallyActiveElement_.blur();

         if (tabHandlerReg_ == null)
         {
            tabHandlerReg_ = Event.addNativePreviewHandler(previewEvent ->
            {
               if (previewEvent.getTypeInt() != Event.ONKEYDOWN)
                  return;

               if (previewEvent.getNativeEvent().getKeyCode() == KeyCodes.KEY_TAB)
               {
                  // Focus goes to next/prev item in tab order, not bounce back to
                  // where it was before menu was invoked
                  cancel();
               }
               else if (previewEvent.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE)
               {
                  // ESC key; if main menu has focus but no expanded child menu, then restore focus to
                  // mimic what happens on a Windows desktop menu
                  if (getSelectedItem() != null &&
                     getSelectedItem().getSubMenu() != null &&
                     !getSelectedItem().getSubMenu().isAttached())
                  {
                     restore();
                  }
               }
            });
         }
      }

      /**
       * Restore previously focused element and stop tracking focus changes.
       */
      public void restore()
      {
         if (!tracking())
            return;

         Element focusMe = originallyActiveElement_;
         cancel();
         try
         {
            focusMe.focus();
         }
         catch (Exception e)
         {
            // swallow exceptions, following example in ModalDialogBase::restoreFocus()
         }
      }

      public void cancel()
      {
         originallyActiveElement_ = null;
         if (tabHandlerReg_ != null)
         {
            tabHandlerReg_.removeHandler();
            tabHandlerReg_ = null;
         }
         DomGlobal.document.removeEventListener("focusin", this::focusInCallback);
      }

      public boolean tracking()
      {
         return originallyActiveElement_ != null;
      }

      private void focusInCallback(elemental2.dom.Event event)
      {
         if (!tracking())
            return;

         // If menu has no selection then it is no longer being used
         if (getSelectedItem() == null)
            restore();
      }

      private Element originallyActiveElement_;
      private HandlerRegistration tabHandlerReg_;
   }

   public BaseMenuBar(boolean vertical)
   {
      super(vertical);
      vertical_ = vertical;

      // Would prefer to inject this from the constructor but some
      // subclasses are instantiated using generated code--don't feel
      // like messing with all that now
      eventBus_ = RStudioGinjector.INSTANCE.getEventBus();
      commandHandler_ = new HandlerRegistrations();
   }

   private MenuItem getTargetedMenuItem(Event event)
   {
      Element targetEl = DOM.eventGetTarget(event);
      if (targetEl == null)
         return null;

      for (MenuItem item : getItems())
         if (item.getElement().isOrHasChild(targetEl))
            return item;

      return null;
   }

   @Override
   public void onBrowserEvent(Event event)
   {
      if (event.getTypeInt() == Event.ONKEYDOWN ||
          event.getTypeInt() == Event.ONKEYPRESS ||
          event.getTypeInt() == Event.ONKEYUP)
      {
         // For 'left' and 'right' keypresses, if an input
         // element is focused, we want to allow the default
         // browser behavior for the keypress -- therefore,
         // don't allow superclass handling of event in such
         // a case.
         int keyCode = event.getKeyCode();
         switch (keyCode)
         {
         case KeyCodes.KEY_LEFT:
         case KeyCodes.KEY_RIGHT:
            break;
         default:
            // For other keypresses, allow superclass to handle
            super.onBrowserEvent(event);
            return;
         }

         Element activeEl = DomUtils.getActiveElement();
         if (activeEl.hasTagName("input"))
            return;

         super.onBrowserEvent(event);
      }
      else if (event.getTypeInt() == Event.ONCLICK)
      {
         // By default, GWT handles click events by sending focus
         // to the menu bar instance, even when the target of the
         // click was not a menu item (e.g. it was a separator).
         // We want to avoid this behavior and so suppress click
         // handling when the click target is not a menu item.
         // If we found a menu item, let super handle the event.
         MenuItem targetItem = getTargetedMenuItem(event);
         if (targetItem != null)
         {
            super.onBrowserEvent(event);
            return;
         }

         // Further verify that the element click is actually
         // focusable, just to ensure that e.g. clicking on
         // non-editable HTML entries in the menu still do
         // focus on the menu.
         Element targetEl = DOM.eventGetTarget(event);
         if (!DomUtils.isFocusable(targetEl))
         {
            super.onBrowserEvent(event);
            return;
         }

         // Don't forward browser event to superclass, effectively
         // hiding this event from the GWT MenuBar class.
      }
      else
      {
         super.onBrowserEvent(event);
      }
   }

   @Override
   protected void onLoad()
   {
      if (vertical_ && glass++ == 0)
         eventBus_.fireEvent(new GlassVisibilityEvent(true));
      super.onLoad();
      commandHandler_.add(eventBus_.addHandler(CommandEvent.TYPE, this));
      for (MenuItem child : getItems())
      {
         if (child instanceof AppMenuItem)
            ((AppMenuItem)child).onShow();
         else
         {
            // if this is a submenu that consists entirely of hidden commands,
            // hide the submenu and its flyout icon
            MenuBar submenu = child.getSubMenu();
            if (submenu != null &&
                submenu instanceof AppMenuBar)
            {
               boolean visible = child.isVisible();
               boolean newVisible = !((AppMenuBar)submenu).allInvisibleCmds();
               if (visible != newVisible)
               {
                  child.setVisible(newVisible);
                  updateSubmenuIcon(child);
               }
            }
         }
      }
      if (autoHideRedundantSeparators_)
         manageSeparators();
   }

   @Override
   protected void onUnload()
   {
      super.onUnload();
      commandHandler_.removeHandler();
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
      if (separators_.isEmpty())
         return;
      List<MenuItem> menuItems = getItems();
      ArrayList<UIObject> allItems = new ArrayList<>(menuItems.size() + separators_.size());
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
      ArrayList<MenuItem> items = new ArrayList<>();
      for (MenuItem item : getItems())
         if (item.isVisible())
            items.add(item);
      return items;
   }

   public MenuItem getItem(int index)
   {
      int count = getItemCount();
      if (count == 0)
         return null;

      if (index < 0)
         index = 0;

      if (index >= count - 1)
         index = count - 1;

      List<MenuItem> items = getItems();
      return items.get(index);
   }

   public void selectItem(int index)
   {
      MenuItem item = getItem(index);
      if (item == null)
         return;

      selectItem(item);
   }

   /**
    * Activate a menu item, tracking where keyboard focus was beforehand so we
    * can restore it user is done using the menu.
    *
    * Only intended for use by the main menubar in RStudio Server, when a menu
    * is activated via keyboard shortcut. If menu is being used via mouse, we
    * don't make any explicit attempt to restore focus after menu is closed.
    */
   public void keyboardActivateItem(int index)
   {
      MenuItem item = getItem(index);
      if (item == null)
         return;

      if (focusTracker_ == null)
         focusTracker_ = new FocusTracker();
      focusTracker_.save();

      // set focus
      getElement().focus();

      // activate item
      doItemAction(item, true, true);
   }

   @Override
   public void onCommand(AppCommand command)
   {
      if (command.getExecutedFromShortcut())
      {
         closeAllChildren(false);
      }
   }

   /**
    * Reference count for glass visibility. NOTE: Perhaps this should be
    * hoisted into a more general class so that everyone who raises
    * GlassVisibilityEvent shares the same refcount.
    */
   private static int glass = 0;

   private boolean autoHideRedundantSeparators_ = true;
   private final ArrayList<MenuItemSeparator> separators_ = new ArrayList<>();
   private final EventBus eventBus_;
   private final boolean vertical_;
   private final HandlerRegistrations commandHandler_;
   private FocusTracker focusTracker_;
}
