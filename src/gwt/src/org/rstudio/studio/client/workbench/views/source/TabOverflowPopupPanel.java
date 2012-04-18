/*
 * TabOverflowPopupPanel.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SuggestOracle;
import org.rstudio.core.client.command.BaseMenuBar;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.core.client.widget.ThemedPopupPanel;

import java.util.ArrayList;

public class TabOverflowPopupPanel extends ThemedPopupPanel
      implements ValueChangeHandler<String>
{
   private class DocsOracle extends SuggestOracle
   {
      @Override
      public void requestSuggestions(Request request, Callback callback)
      {
         
      }
   }

   private class MenuKeyHandler implements KeyDownHandler
   {
      public MenuKeyHandler(BaseMenuBar menu)
      {
         menu_ = menu;
      }

      public void onKeyDown(KeyDownEvent event)
      {
         switch (event.getNativeKeyCode())
         {
            case KeyCodes.KEY_DOWN:
            case KeyCodes.KEY_UP:
               event.preventDefault();
               event.stopPropagation();

               ArrayList<MenuItem> items = menu_.getVisibleItems();

               if (items.size() == 0)
                  return;

               boolean up = event.getNativeKeyCode() == KeyCodes.KEY_UP;

               int index = up ? items.size() + 1 : -1;

               MenuItem selectedItem = menu_.getSelectedItem();
               if (selectedItem != null && items.contains(selectedItem))
                  index = items.indexOf(selectedItem);

               index = (index + (up ? -1 : 1) + items.size()) % items.size();

               menu_.selectItem(items.get(index));
               break;
            case KeyCodes.KEY_ENTER:
               event.preventDefault();
               event.stopPropagation();

               MenuItem selected = menu_.getSelectedItem();
               if (selected != null && selected.isVisible())
                  selected.getCommand().execute();
               else
               {
                  ArrayList<MenuItem> visibleItems = menu_.getVisibleItems();
                  if (visibleItems.size() == 1)
                     visibleItems.get(0).getCommand().execute();
               }
               break;
            case KeyCodes.KEY_ESCAPE:
               event.preventDefault();
               event.stopPropagation();

               hide(false);
         }
      }

      private final BaseMenuBar menu_;
   }

   public TabOverflowPopupPanel()
   {
      super(true, false);

      DockPanel dockPanel = new DockPanel();

      search_ = new SearchWidget(new DocsOracle());
      search_.addValueChangeHandler(this);

      search_.getElement().getStyle().setMarginRight(0, Unit.PX);
      dockPanel.add(search_, DockPanel.NORTH);

      menu_ = new DocsMenu();
      menu_.setOwnerPopupPanel(this);
      menu_.setWidth("100%");
      dockPanel.add(menu_, DockPanel.CENTER);
      setWidget(dockPanel);

      setStylePrimaryName(ThemeStyles.INSTANCE.tabOverflowPopup());

      addDomHandler(new MenuKeyHandler(menu_), KeyDownEvent.getType());

      addHandler(new CloseHandler<PopupPanel>() {

         public void onClose(CloseEvent<PopupPanel> popupPanelCloseEvent)
         {
            search_.setText("", true);
            menu_.filter(null);
            menu_.selectItem(null);
         }
      }, CloseEvent.getType());
   }

   public void onValueChange(ValueChangeEvent<String> event)
   {
      String value = event.getValue();
      menu_.filter(value);
   }

   @Override
   public void show()
   {
      super.show();
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         public void execute()
         {
            search_.focus();
         }
      });
   }

   private final DocsMenu menu_;
   private final SearchWidget search_;
}
