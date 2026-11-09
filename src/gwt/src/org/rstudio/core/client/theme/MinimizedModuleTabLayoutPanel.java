/*
 * MinimizedModuleTabLayoutPanel.java
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
package org.rstudio.core.client.theme;

import java.util.ArrayList;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.aria.client.SelectedValue;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.events.WindowStateChangeEvent;
import org.rstudio.core.client.layout.WindowState;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;

/**
 * This is the minimized version of the Data|File|Plot|Package|Help group
 */
public class MinimizedModuleTabLayoutPanel
      extends MinimizedWindowFrame
   implements HasSelectionHandlers<Integer>
{
   public MinimizedModuleTabLayoutPanel(String accessibleName)
   {
      super(null, accessibleName, new FlowPanel());
      addStyleName(ThemeResources.INSTANCE.themeStyles().moduleTabPanel());
      addStyleName(ThemeResources.INSTANCE.themeStyles().minimized());

      // The surrounding region is already named "<pane> minimized"; name the
      // tablist after the pane alone, as the expanded tab strip is.
      FlowPanel tabList = getTabList();
      tabList.addStyleName(ThemeResources.INSTANCE.themeStyles().minimizedTabList());
      Roles.getTablistRole().set(tabList.getElement());
      Roles.getTablistRole().setAriaLabelProperty(tabList.getElement(), accessibleName);
   }

   public void setTabs(String[] tabNames)
   {
      FlowPanel tabList = getTabList();
      tabList.clear();
      tabs_.clear();
      tabIndices_.clear();

      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();
      for (int i = 0; i < tabNames.length; i++)
      {
         String tabName = tabNames[i];
         if (tabName == null)
            continue;
         ModuleTabLayoutPanel.ModuleTab tab
               = new ModuleTabLayoutPanel.ModuleTab(tabName, styles, false, true /*minimized*/);
         tab.addStyleName("gwt-TabLayoutPanelTab");
         tab.getElement().setId(ElementIds.getUniqueElementId(tab.getTabId()));
         Roles.getTabRole().set(tab.getElement());
         final Integer thisIndex = i;
         tab.addClickHandler(event ->
         {
            event.preventDefault();
            SelectionEvent.fire(
                  MinimizedModuleTabLayoutPanel.this,
                  thisIndex);
         });
         tab.addDomHandler(event -> onTabKeyDown(event, thisIndex), KeyDownEvent.getType());
         tabList.add(tab);
         tabs_.add(tab);
         tabIndices_.add(thisIndex);
      }
      setSelectedIndex(selectedIndex_);
   }

   /**
    * Mark the tab the pane will show when restored. Selection here doesn't
    * restore the pane; it mirrors the expanded tab panel's selection.
    */
   public void setSelectedIndex(int index)
   {
      selectedIndex_ = index;
      int selectedPos = Math.max(0, tabIndices_.indexOf(index));
      for (int pos = 0; pos < tabs_.size(); pos++)
      {
         boolean selected = tabIndices_.get(pos) == index;
         Roles.getTabRole().setAriaSelectedState(tabs_.get(pos).getElement(),
               selected ? SelectedValue.TRUE : SelectedValue.FALSE);
         tabs_.get(pos).getElement().setTabIndex(pos == selectedPos ? 0 : -1);
      }
   }

   public HandlerRegistration addSelectionHandler(SelectionHandler<Integer> handler)
   {
      return addHandler(handler, SelectionEvent.getType());
   }

   // Arrows only move focus: activating a tab restores the pane, so it waits
   // for Enter or Space (manual activation).
   private void onTabKeyDown(KeyDownEvent event, int index)
   {
      int pos = tabIndices_.indexOf(index);
      int count = tabs_.size();
      switch (event.getNativeKeyCode())
      {
         case KeyCodes.KEY_LEFT:
            focusTabAt((pos - 1 + count) % count);
            break;
         case KeyCodes.KEY_RIGHT:
            focusTabAt((pos + 1) % count);
            break;
         case KeyCodes.KEY_HOME:
            focusTabAt(0);
            break;
         case KeyCodes.KEY_END:
            focusTabAt(count - 1);
            break;
         case KeyCodes.KEY_ENTER:
         case KeyCodes.KEY_SPACE:
            // A click on a tab also bubbles up to the frame, which restores
            // the pane; a key press has to do both itself.
            SelectionEvent.fire(this, index);
            fireEvent(new WindowStateChangeEvent(WindowState.NORMAL));
            break;
         default:
            return;
      }
      event.preventDefault();
      event.stopPropagation();
   }

   private void focusTabAt(int pos)
   {
      for (int i = 0; i < tabs_.size(); i++)
         tabs_.get(i).getElement().setTabIndex(i == pos ? 0 : -1);
      tabs_.get(pos).getElement().focus();
   }

   private FlowPanel getTabList()
   {
      return (FlowPanel) getExtraWidget();
   }

   private final ArrayList<ModuleTabLayoutPanel.ModuleTab> tabs_ = new ArrayList<>();
   private final ArrayList<Integer> tabIndices_ = new ArrayList<>();
   private int selectedIndex_ = -1;
}
