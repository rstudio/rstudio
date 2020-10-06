/*
 * ToolbarPopupMenu.java
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

import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MenuItemSeparator;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.AppMenuItem;
import org.rstudio.core.client.command.BaseMenuBar;
import org.rstudio.core.client.command.CommandEvent;
import org.rstudio.core.client.command.CommandHandler;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;

public class ToolbarPopupMenu extends ThemedPopupPanel
                              implements CommandHandler
{
   // Extensibility point for dynamically constructed popup menus. The default
   // implementation returns itself, but extensions can do some work to build
   // the menu and return the built menu. Callers can use this in combination
   // with getDynamicPopupMenu() when an up-to-date instance of the object is
   // required.
   public interface DynamicPopupMenuCallback
   {
      void onPopupMenu(ToolbarPopupMenu menu);
   }

   /**
    * Does the control receive keyboard focus?
    */
   public enum ReceivesFocus
   {
      NO,
      YES
   }

   public ToolbarPopupMenu()
   {
      super(true);
      menuBar_ = createMenuBar();
      Widget mainWidget = createMainWidget();
      setWidget(mainWidget);
      events_ = RStudioGinjector.INSTANCE.getEventBus();
      commandHandler_ = new HandlerRegistrations();
      getElement().getStyle().setZIndex(1000);
   }

   public ToolbarPopupMenu(ToolbarPopupMenu parent)
   {
      this();
      parent_ = parent;
   }

   /**
    * Position popup relative to a point, typically for a right-click context menu
    * @param clientX
    * @param clientY
    */
   public void showRelativeTo(int clientX, int clientY)
   {
      setPopupPositionAndShow((offsetWidth, offsetHeight) ->
      {
         // Calculate left position for the popup; normally the clicked location but
         // if it doesn't fix horizontally nudge it to the left
         int left = clientX;

         // Make sure scrolling is taken into account, since
         // box.getAbsoluteLeft() takes scrolling into account.
         int windowRight = Window.getClientWidth() + Window.getScrollLeft();
         int windowLeft = Window.getScrollLeft();

         // Distance from the clicked location to the right edge of the window
         int distanceToWindowRight = windowRight - clientX;

         // Distance from the clicked location to the left edge of the window
         int distanceFromWindowLeft = clientX - windowLeft;

         // If there is not enough space for the overflow of the popup's
         // width to the right, and there IS enough space for the
         // overflow to the left, then right-align the popup.
         // However, if there is not enough space on either side, then stick with
         // left-alignment.
         if (distanceToWindowRight < offsetWidth && distanceFromWindowLeft >= offsetWidth)
         {
            // Align the right edge of popup with clicked location
            left -= offsetWidth;
         }

         // Calculate top position for the popup; normally we expand "down" from where user
         // right-clicked but if there isn't room them expand "up"
         int top = clientY;

         // Make sure scrolling is taken into account, since
         // box.getAbsoluteTop() takes scrolling into account. We don't normally
         // allow the main window to be scrolled, but just in case.
         int windowTop = Window.getScrollTop();
         int windowBottom = Window.getScrollTop() + Window.getClientHeight();

         // Distance from the top edge of the window to the clicked location
         int distanceFromWindowTop = top - windowTop;

         // Distance from the bottom edge of the window to the clicked location
         int distanceToWindowBottom = windowBottom - top;

         // If there is not enough space for the popup's height below the clicked
         // location and there IS enough space for the popup's height above the
         // clicked location, then position the popup above the location. However, if there
         // is not enough space on either side, then stick with displaying the
         // popup below the clicked location.
         if (distanceToWindowBottom < offsetHeight && distanceFromWindowTop >= offsetHeight)
         {
            top -= offsetHeight;
         }
         setAutoConstrain(false);
         setPopupPosition(left, top);
      });
   }

   protected ToolbarMenuBar createMenuBar()
   {
      return new ToolbarMenuBar(true);
   }

   protected Widget createMainWidget()
   {
      return menuBar_;
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      commandHandler_.add(events_.addHandler(CommandEvent.TYPE, this));
   }

   @Override
   protected void onUnload()
   {
      super.onUnload();
      menuBar_.selectItem(null);
      commandHandler_.removeHandler();
   }

   public void selectFirst()
   {
      menuBar_.selectFirst();
   }

   public void selectLast()
   {
      menuBar_.selectLast();
   }

   public void moveSelectionFwd(int numElements)
   {
      menuBar_.moveSelectionFwd(numElements);
   }

   public void moveSelectionBwd(int numElements)
   {
      menuBar_.moveSelectionBwd(numElements);
   }

   public void selectItem(MenuItem menuItem)
   {
      menuBar_.selectItem(menuItem);
   }

   public void addItem(MenuItem menuItem)
   {
      ScheduledCommand command = menuItem.getScheduledCommand();
      if (command == null && menuItem instanceof AppMenuItem)
      {
         AppMenuItem appMenuItem = (AppMenuItem) menuItem;
         command = appMenuItem.getScheduledCommand(true);
      }

      if (command != null)
         menuItem.setScheduledCommand(new ToolbarPopupMenuCommand(command));

      menuBar_.addItem(menuItem);
   }

   public void addItem(SafeHtml html, MenuBar popup)
   {
      menuBar_.addItem(html, popup);
   }

   public void addItem(MenuItem menuItem, final ToolbarPopupMenu popup)
   {
      menuBar_.addItem(SafeHtmlUtils.fromTrustedString(menuItem.getHTML()), popup.menuBar_);
   }

   public void addItem(AppCommand command, ToolbarPopupMenu popup)
   {
      if (command.isEnabled())
         addItem(command.createMenuItem(false), popup);
   }

   public void setAutoOpen(boolean autoOpen)
   {
      menuBar_.setAutoOpen(autoOpen);
   }

   public void insertItem(MenuItem menuItem, int beforeIndex)
   {
     ScheduledCommand command = menuItem.getScheduledCommand();
      if (command != null)
         menuItem.setScheduledCommand(new ToolbarPopupMenuCommand(command));
      menuBar_.insertItem(menuItem, beforeIndex);
   }

   public void removeItem(MenuItem menuItem)
   {
      menuBar_.removeItem(menuItem);
   }

   public boolean containsItem(MenuItem menuItem)
   {
      return menuBar_.getItemIndex(menuItem) >= 0;
   }

   public void clearItems()
   {
      menuBar_.clearItems();
   }

   public void addSeparator()
   {
      menuBar_.addSeparator();
   }

   public void addSeparator(MenuItemSeparator separator)
   {
      menuBar_.addSeparator(separator);
   }

   public void addSeparator(String label)
   {
      menuBar_.addSeparator(new LabelledMenuSeparator(label));
   }

   public void addSeparator(int minPx)
   {
      menuBar_.addSeparator(new MinWidthMenuSeparator(minPx));
   }

   public int getItemCount()
   {
      return menuBar_.getItemCount();
   }

   public List<MenuItem> getMenuItems() { return menuBar_.getMenuItems(); }

   public void focus()
   {
      menuBar_.focus();
   }

   public void setAutoHideRedundantSeparators(boolean value)
   {
      menuBar_.setAutoHideRedundantSeparators(value);
   }

   public void getDynamicPopupMenu(DynamicPopupMenuCallback callback)
   {
      callback.onPopupMenu(this);
   }

   public void addMenuBarStyle(String style)
   {
      menuBar_.addStyleName(style);
   }

   private class ToolbarPopupMenuCommand implements ScheduledCommand
   {
      public ToolbarPopupMenuCommand(ScheduledCommand coreCommand)
      {
         coreCommand_ = coreCommand;
      }
      public void execute()
      {
         Scheduler.get().scheduleFinally(coreCommand_);
         hide();
         if (parent_ != null) parent_.hide();
      }

      private ScheduledCommand coreCommand_;
   }

   protected class ToolbarMenuBar extends BaseMenuBar
   {
      public ToolbarMenuBar(boolean vertical)
      {
         super(vertical);
      }

      @Override
      protected void onUnload()
      {
         nativePreviewReg_.removeHandler();
         super.onUnload();
      }

      @Override
      protected void onLoad()
      {
         super.onLoad();

         nativePreviewReg_ = Event.addNativePreviewHandler(nativePreviewEvent ->
         {
            if (nativePreviewEvent.getTypeInt() == Event.ONKEYDOWN)
            {
               switch (nativePreviewEvent.getNativeEvent().getKeyCode())
               {
                  case KeyCodes.KEY_ESCAPE:
                     nativePreviewEvent.cancel();
                     hide();
                     break;
                  case KeyCodes.KEY_PAGEDOWN:
                     nativePreviewEvent.cancel();
                     moveSelectionFwd(5);
                     break;
                  case KeyCodes.KEY_PAGEUP:
                     nativePreviewEvent.cancel();
                     moveSelectionBwd(5);
                     break;
               }

               /* if we don't have focus, handle keyboard here because our host will not */
               if (getReceivesFocus() == ReceivesFocus.NO)
               {
                  switch (nativePreviewEvent.getNativeEvent().getKeyCode())
                  {
                     case KeyCodes.KEY_UP:
                        nativePreviewEvent.cancel();
                        moveSelectionUp();
                        break;
                     case KeyCodes.KEY_DOWN:
                        nativePreviewEvent.cancel();
                        moveSelectionDown();
                        break;
                     case KeyCodes.KEY_HOME:
                        nativePreviewEvent.cancel();
                        selectFirst();
                        break;
                     case KeyCodes.KEY_END:
                        nativePreviewEvent.cancel();
                        selectLast();
                        break;
                     case KeyCodes.KEY_ENTER:
                     case KeyCodes.KEY_SPACE:
                        nativePreviewEvent.cancel();
                        final MenuItem menuItem = getSelectedItem();
                        if (menuItem != null)
                        {
                           nativePreviewEvent.cancel();
                           NativeEvent evt = Document.get().createClickEvent(
                              0,
                              0,
                              0,
                              0,
                              0,
                              false,
                              false,
                              false,
                              false);
                           menuItem.getElement().dispatchEvent(evt);
                        }
                        break;
                  }
               }
            }
         });
      }

      public int getItemCount()
      {
         return getItems().size();
      }

      public List<MenuItem> getMenuItems() { return getItems(); }

      public int getSelectedIndex()
      {
         MenuItem selectedMenuItem = getSelectedItem();
         List<MenuItem> menuItems = getItems();
         for (int i = 0; i<menuItems.size(); i++)
         {
            if (menuItems.get(i) == selectedMenuItem)
               return i;
         }
         return -1;
      }

      private void moveSelectionFwd(int numElements)
      {
         selectItem(getSelectedIndex() + numElements);
      }

      private void moveSelectionBwd(int numElements)
      {
         selectItem(getSelectedIndex() - numElements);
      }

      private void selectFirst()
      {
         selectItem(0);
      }

      private void selectLast()
      {
         selectItem(getItemCount() - 1);
      }

      private HandlerRegistration nativePreviewReg_;
   }

   public Element getMenuTableElement()
   {
      Element menuEl = getWidget().getElement();
      Node tableNode = DomUtils.findNode(menuEl, true, true, node ->
      {
         if (!(node instanceof Element))
            return false;

         Element el = (Element) node;
         return el.hasTagName("table");
      });

      if (tableNode == null)
         return null;

      return tableNode.cast();

   }

   @Override
   public void onCommand(AppCommand command)
   {
      if (command.getExecutedFromShortcut())
         hide();
   }

   public void setReceivesFocus(ReceivesFocus receivesFocus)
   {
      receivesFocus_ = receivesFocus;
   }

   public ReceivesFocus getReceivesFocus()
   {
      return receivesFocus_;
   }

   protected final ToolbarMenuBar menuBar_;
   private ToolbarPopupMenu parent_;
   private final EventBus events_;
   private final HandlerRegistrations commandHandler_;
   private ReceivesFocus receivesFocus_ = ReceivesFocus.YES;
}
