/*
 * BaseMenuBar.java
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
package org.rstudio.core.client.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.SeparatorManager;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.events.GlassVisibilityEvent;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MenuItemSeparator;
import com.google.gwt.user.client.ui.UIObject;

import elemental2.dom.DomGlobal;
import elemental2.dom.EventListener;

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
    * Saves the element that had focus before the main menubar was entered, by
    * shortcut, Tab or click, and restores it when a command runs or Escape is
    * pressed on the bar, so focus isn't left on the body afterwards.
    */
   private class FocusTracker
   {
      /**
       * Store the given element and start monitoring focus changes.
       *
       * @return true if tracking started; false if already tracking or the element is null
       */
      public boolean save(Element focused)
      {
         // Don't recapture the focused element if we already have one, e.g. user
         // hits another main menu shortcut while menu still open from previous one
         if (tracking() || focused == null)
            return false;

         originallyActiveElement_ = focused;
         DomGlobal.document.addEventListener("focusin", focusInListener_);

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
         return true;
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
         DomGlobal.document.removeEventListener("focusin", focusInListener_);
      }

      public boolean tracking()
      {
         return originallyActiveElement_ != null;
      }

      private void focusInCallback(elemental2.dom.Event event)
      {
         // If the menu has no selection, focus has moved somewhere else, e.g. the
         // user clicked outside the menus. Leave it there.
         if (tracking() && getSelectedItem() == null)
            cancel();
      }

      private Element originallyActiveElement_;
      private HandlerRegistration tabHandlerReg_;
      // A single instance, so removeEventListener() gets the listener that was added
      private final EventListener focusInListener_ = this::focusInCallback;
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

      // See the ONMOUSEDOWN case in onBrowserEvent()
      if (!vertical)
         sinkEvents(Event.ONMOUSEDOWN);
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
      else if (event.getTypeInt() == Event.ONFOCUS)
      {
         EventTarget previous = event.getRelatedEventTarget();
         if (!vertical_ && previous != null && Element.is(previous))
            trackFocusFrom(Element.as(previous));
         super.onBrowserEvent(event);
      }
      else if (event.getTypeInt() == Event.ONMOUSEDOWN)
      {
         // Focus coming from an iframe (e.g. the Help pane) has no relatedTarget,
         // but before a click moves focus, the iframe is still the active element.
         if (!vertical_)
            trackFocusFrom(DomUtils.getActiveElement());
         super.onBrowserEvent(event);
      }
      else
      {
         super.onBrowserEvent(event);
      }
   }

   /**
    * Start tracking focus when the menubar is entered from outside the menus,
    * whether by Tab, click or shortcut. Focus coming back from one of the
    * menus' own popups isn't an entry, and the body isn't worth restoring.
    *
    * @return true if tracking started
    */
   private boolean trackFocusFrom(Element previous)
   {
      if (previous == null || previous == Document.get().getBody())
         return false;

      String role = previous.getAttribute("role");
      if (role.equals("menu") || role.equals("menubar"))
         return false;

      return focusTracker().save(previous);
   }

   private FocusTracker focusTracker()
   {
      if (focusTracker_ == null)
         focusTracker_ = new FocusTracker();
      return focusTracker_;
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
    * Open a menu with focus on its first item, tracking where keyboard focus
    * was beforehand so we can restore it when the user is done with the menu.
    *
    * Only intended for use by the main menubar in RStudio Server, when a menu
    * is activated via keyboard shortcut.
    */
   public void keyboardActivateItem(int index)
   {
      MenuItem item = getItem(index);
      if (item == null)
         return;

      Element active = DomUtils.getActiveElement();
      if (trackFocusFrom(active))
         active.blur();

      // Open the menu explicitly: another menu may already be open, e.g. when
      // the shortcut was dispatched from a satellite window.
      getElement().focus();
      doItemAction(item, false, true);
      moveSelectionDown();
   }

   @Override
   public void onCommand(AppCommand command)
   {
      if (command.getExecutedFromShortcut())
      {
         closeAllChildren(false);
      }

      // Restore focus when a command is fired so any resulting modal dialog can pick up
      // the correct focused element when it does its own focus push/pop. This comes after
      // closing menus, which can focus a parent menu. The menubar's own registration is
      // used, since onUnload() removes all of them.
      if (focusTracker_ != null)
         focusTracker_.restore();
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
